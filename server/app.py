"""Alpi API and guest album. Run: uvicorn server.app:app --host 0.0.0.0."""
from contextlib import contextmanager
import hashlib
import hmac
import io
import os
from pathlib import Path
import re
import secrets
import sqlite3
import threading
import time
import warnings

from fastapi import Depends, FastAPI, HTTPException, Request
from fastapi.responses import FileResponse, JSONResponse, Response
from fastapi.staticfiles import StaticFiles
from PIL import Image, ImageOps, UnidentifiedImageError
from pydantic import BaseModel, Field
import qrcode

DATA = Path(os.getenv("ALPI_DATA", "data")).resolve()
DATA.mkdir(parents=True, exist_ok=True)
TOKEN = os.getenv("ALPI_ADMIN_TOKEN", "")
if len(TOKEN) < 24:
    raise RuntimeError("Set ALPI_ADMIN_TOKEN to a random secret of at least 24 characters.")
PUBLIC_BASE = os.getenv("ALPI_PUBLIC_BASE_URL", "").rstrip("/")
MAX_BYTES = 20 * 1024 * 1024
Image.MAX_IMAGE_PIXELS = 40_000_000
DB = DATA / "alpi.sqlite3"
app = FastAPI(title="Alpi", docs_url=None, redoc_url=None)


@contextmanager
def db():
    con = sqlite3.connect(DB, timeout=30)
    con.row_factory = sqlite3.Row
    con.execute("PRAGMA journal_mode=WAL")
    con.execute("PRAGMA foreign_keys=ON")
    try:
        with con:
            yield con
    finally:
        con.close()


with db() as c:
    c.executescript("""
    CREATE TABLE IF NOT EXISTS events (
      id TEXT PRIMARY KEY, album_key TEXT UNIQUE NOT NULL, title TEXT NOT NULL,
      description TEXT NOT NULL, pin_hash TEXT NOT NULL DEFAULT '', created INTEGER NOT NULL);
    CREATE TABLE IF NOT EXISTS photos (
      id TEXT NOT NULL, event_id TEXT NOT NULL REFERENCES events(id), caption TEXT NOT NULL,
      captured INTEGER NOT NULL, visible INTEGER NOT NULL DEFAULT 1, created INTEGER NOT NULL,
      PRIMARY KEY (event_id,id));
    """)


def operator(request: Request):
    if not hmac.compare_digest(request.headers.get("authorization", ""), "Bearer " + TOKEN):
        raise HTTPException(401, "Token operator tidak sesuai")


def valid_id(value):
    if not re.fullmatch(r"[a-f0-9]{32}", value):
        raise HTTPException(400, "ID tidak valid")
    return value


def event_by(field, value):
    with db() as c:
        row = c.execute(f"SELECT * FROM events WHERE {field}=?", (value,)).fetchone()
    if not row:
        raise HTTPException(404, "Album tidak ditemukan")
    return row


def hash_pin(pin, salt=None):
    salt = salt or secrets.token_hex(16)
    return salt + ":" + hashlib.scrypt(pin.encode(), salt=bytes.fromhex(salt), n=16384, r=8, p=1).hex()


def cookie_value(row):
    expires = str(int(time.time()) + 86400)
    message = f"{row['album_key']}:{row['pin_hash']}:{expires}"
    return expires + "." + hmac.new(TOKEN.encode(), message.encode(), hashlib.sha256).hexdigest()


def guest(row, request):
    if not row["pin_hash"]:
        return
    value = request.cookies.get("alpi_" + row["album_key"], "")
    try:
        expiry, signature = value.split(".")
        message = f"{row['album_key']}:{row['pin_hash']}:{expiry}"
        expected = hmac.new(TOKEN.encode(), message.encode(), hashlib.sha256).hexdigest()
        if int(expiry) > time.time() and hmac.compare_digest(expected, signature):
            return
    except (ValueError, TypeError):
        pass
    raise HTTPException(401, "Album memerlukan PIN")


@app.middleware("http")
async def headers(request, call_next):
    response = await call_next(request)
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["Referrer-Policy"] = "no-referrer"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["Content-Security-Policy"] = "default-src 'self'; img-src 'self' blob:; style-src 'self'; script-src 'self'; connect-src 'self'; frame-ancestors 'none'"
    if request.url.path.startswith(("/api/", "/media/")):
        response.headers["Cache-Control"] = "no-store"
    return response


class EventIn(BaseModel):
    title: str = Field(min_length=1, max_length=120)
    description: str = Field(default="", max_length=1000)
    pin: str = Field(default="", max_length=32)


def event_out(row, request):
    base = PUBLIC_BASE or str(request.base_url).rstrip("/")
    return {"id": row["id"], "title": row["title"], "album_url": base + "/a/" + row["album_key"], "album_key": row["album_key"]}


@app.get("/api/health")
def health():
    return {"ok": True, "version": "0.1.0"}


@app.put("/api/events/{event_id}", dependencies=[Depends(operator)])
def put_event(event_id: str, value: EventIn, request: Request):
    valid_id(event_id)
    title = value.title.strip()
    if not title:
        raise HTTPException(400, "Judul acara wajib diisi")
    with db() as c:
        existing = c.execute("SELECT * FROM events WHERE id=?", (event_id,)).fetchone()
        pin_hash = hash_pin(value.pin) if value.pin else ""
        if existing and value.pin and existing["pin_hash"]:
            salt = existing["pin_hash"].split(":")[0]
            if hmac.compare_digest(hash_pin(value.pin, salt), existing["pin_hash"]):
                pin_hash = existing["pin_hash"]
        c.execute("""INSERT INTO events VALUES (?,?,?,?,?,?)
          ON CONFLICT(id) DO UPDATE SET title=excluded.title, description=excluded.description, pin_hash=excluded.pin_hash""",
          (event_id, existing["album_key"] if existing else secrets.token_hex(16), title, value.description, pin_hash, int(time.time()),))
    return event_out(event_by("id", event_id), request)


@app.get("/api/events", dependencies=[Depends(operator)])
def events(request: Request):
    with db() as c:
        return {"events": [event_out(row, request) for row in c.execute("SELECT * FROM events ORDER BY created DESC")]}


@app.get("/api/events/{event_id}/qr", dependencies=[Depends(operator)])
def event_qr(event_id: str, request: Request):
    url = event_out(event_by("id", valid_id(event_id)), request)["album_url"]
    image = qrcode.make(url)
    buf = io.BytesIO()
    image.save(buf, format="PNG")
    return Response(buf.getvalue(), media_type="image/png")


class Unlock(BaseModel):
    pin: str = Field(max_length=32)


attempts = {}
attempt_lock = threading.Lock()


@app.post("/api/albums/{key}/unlock")
def unlock(key: str, value: Unlock, request: Request):
    row = event_by("album_key", valid_id(key))
    remote = (request.client.host if request.client else "unknown", key)
    now = time.monotonic()
    with attempt_lock:
        # Bound the map and expire old entries without trusting forwarded headers.
        for k in list(attempts):
            if attempts[k][0] < now - 300:
                del attempts[k]
        start, count = attempts.get(remote, (now, 0))
        if count >= 10:
            raise HTTPException(429, "Terlalu banyak percobaan. Tunggu 5 menit.")
        attempts[remote] = (start, count + 1)
    stored = row["pin_hash"]
    if stored and not hmac.compare_digest(hash_pin(value.pin, stored.split(":")[0]), stored):
        raise HTTPException(403, "PIN tidak sesuai")
    response = JSONResponse({"ok": True})
    response.set_cookie("alpi_" + key, cookie_value(row), httponly=True, secure=request.url.scheme == "https", samesite="strict", max_age=86400)
    return response


@app.get("/api/albums/{key}")
def album(key: str, request: Request):
    row = event_by("album_key", valid_id(key))
    guest(row, request)
    with db() as c:
        photos = [dict(p) for p in c.execute("SELECT id,caption,captured FROM photos WHERE event_id=? AND visible=1 ORDER BY captured DESC,id DESC", (row["id"],))]
    for p in photos:
        p.update(url=f"/media/{key}/{p['id']}", thumbnail=f"/media/{key}/{p['id']}?thumbnail=true")
    return {"title": row["title"], "description": row["description"], "photos": photos}


@app.put("/api/events/{event_id}/photos/{photo_id}", dependencies=[Depends(operator)])
async def upload(event_id: str, photo_id: str, request: Request):
    row = event_by("id", valid_id(event_id))
    valid_id(photo_id)
    # Idempotent on a stable client key: a failed ACK cannot create duplicates.
    with db() as c:
        if c.execute("SELECT id FROM photos WHERE event_id=? AND id=?", (event_id, photo_id)).fetchone():
            return {"id": photo_id, "duplicate": True}
    folder = DATA / event_id
    folder.mkdir(exist_ok=True)
    temp = folder / (secrets.token_hex(16) + ".part")
    thumb_temp = folder / (secrets.token_hex(16) + ".part")
    try:
        size = 0
        with temp.open("wb") as output:
            async for chunk in request.stream():
                size += len(chunk)
                if size > MAX_BYTES:
                    raise HTTPException(413, "Ukuran foto maksimal 20 MB")
                output.write(chunk)
        with warnings.catch_warnings():
            warnings.simplefilter("error", Image.DecompressionBombWarning)
            with Image.open(temp) as source:
                if source.format != "JPEG":
                    raise HTTPException(415, "Unggah hasil JPEG")
                source.load()
                thumb = ImageOps.exif_transpose(source).convert("RGB")
                thumb.thumbnail((640, 640))
                thumb.save(thumb_temp, format="JPEG", quality=80)
        # Serialize finalization with SQLite's write lock, including duplicate races.
        with db() as c:
            c.execute("BEGIN IMMEDIATE")
            if c.execute("SELECT id FROM photos WHERE event_id=? AND id=?", (event_id, photo_id)).fetchone():
                return {"id": photo_id, "duplicate": True}
            os.replace(temp, folder / (photo_id + ".jpg"))
            os.replace(thumb_temp, folder / (photo_id + ".thumb.jpg"))
            captured = request.headers.get("X-Captured-At", "0")
            try:
                captured = max(0, int(captured))
            except ValueError:
                captured = 0
            c.execute("INSERT INTO photos VALUES (?,?,?,?,?,?)", (photo_id, event_id, "", captured, 1, int(time.time()),))
        return {"id": photo_id, "duplicate": False}
    except (UnidentifiedImageError, OSError, Image.DecompressionBombError, Image.DecompressionBombWarning):
        raise HTTPException(415, "Foto rusak atau dimensinya terlalu besar")
    finally:
        temp.unlink(missing_ok=True)
        thumb_temp.unlink(missing_ok=True)


class Visibility(BaseModel):
    visible: bool


@app.patch("/api/events/{event_id}/photos/{photo_id}", dependencies=[Depends(operator)])
def visibility(event_id: str, photo_id: str, value: Visibility):
    with db() as c:
        result = c.execute("UPDATE photos SET visible=? WHERE event_id=? AND id=?", (int(value.visible), valid_id(event_id), valid_id(photo_id)))
        if not result.rowcount:
            raise HTTPException(404, "Foto tidak ditemukan")
    return {"ok": True}


@app.get("/media/{key}/{photo_id}")
def media(key: str, photo_id: str, request: Request, thumbnail: bool = False, download: bool = False):
    row = event_by("album_key", valid_id(key))
    valid_id(photo_id)
    guest(row, request)
    with db() as c:
        photo = c.execute("SELECT * FROM photos WHERE event_id=? AND id=? AND visible=1", (row["id"], photo_id)).fetchone()
    if not photo:
        raise HTTPException(404, "Foto tidak tersedia")
    path = DATA / row["id"] / (photo_id + (".thumb.jpg" if thumbnail else ".jpg"))
    if not path.exists():
        raise HTTPException(404, "Berkas foto tidak tersedia")
    return FileResponse(path, media_type="image/jpeg", filename=("Alpi-" + photo_id[:8] + ".jpg") if download else None)


WEB = Path(__file__).parent / "web"
app.mount("/assets", StaticFiles(directory=WEB), name="assets")


@app.get("/a/{key}")
def album_page(key: str):
    valid_id(key)
    return FileResponse(WEB / "index.html")


@app.get("/")
def home():
    return FileResponse(WEB / "home.html")
