import io
import importlib
import secrets
import sys
import pytest
from fastapi.testclient import TestClient
from PIL import Image


@pytest.fixture
def api(tmp_path, monkeypatch):
    monkeypatch.setenv("ALPI_DATA", str(tmp_path))
    monkeypatch.setenv("ALPI_ADMIN_TOKEN", "test-only-" + "x" * 32)
    monkeypatch.setenv("ALPI_PUBLIC_BASE_URL", "https://album.example")
    sys.modules.pop("server.app", None)
    module = importlib.import_module("server.app")
    client = TestClient(module.app)
    client.headers["Authorization"] = "Bearer " + module.TOKEN
    return module, client


def event(client, pin=""):
    eid = secrets.token_hex(16)
    response = client.put("/api/events/" + eid, json={"title": "Acara uji", "description": "Foto bersama", "pin": pin})
    assert response.status_code == 200, response.text
    return response.json()


def jpeg():
    b = io.BytesIO()
    Image.new("RGB", (1200, 800), "#176b56").save(b, format="JPEG")
    return b.getvalue()


def upload(client, eid, pid=None, data=None):
    pid = pid or secrets.token_hex(16)
    r = client.put(f"/api/events/{eid}/photos/{pid}", content=jpeg() if data is None else data,
                   headers={"Content-Type": "image/jpeg", "X-Captured-At": "1793000000000"})
    return pid, r


def test_operator_auth_required(api):
    module, client = api
    client.headers.pop("Authorization")
    assert client.get("/api/events").status_code == 401
    assert client.put("/api/events/" + "a" * 32, json={"title": "Unauthorized"}).status_code == 401


def test_event_url_stable_and_no_secret_in_response(api):
    module, client = api
    e = event(client, "1234")
    updated = client.put("/api/events/" + e["id"], json={"title": "Revisi", "pin": "1234"}).json()
    assert updated["album_url"] == e["album_url"]
    assert updated["album_url"].startswith("https://album.example/a/")
    assert "pin" not in updated and module.TOKEN not in str(updated)


def test_upload_is_idempotent_and_preview_download(api):
    module, client = api
    e = event(client)
    pid, r = upload(client, e["id"])
    assert r.status_code == 200 and not r.json()["duplicate"]
    assert upload(client, e["id"], pid)[1].json()["duplicate"]
    gallery = client.get("/api/albums/" + e["album_key"]).json()
    assert len(gallery["photos"]) == 1
    image = gallery["photos"][0]
    thumb = client.get(image["thumbnail"])
    assert max(Image.open(io.BytesIO(thumb.content)).size) <= 640
    full = client.get(image["url"] + "?download=true")
    assert full.content == jpeg()
    assert "attachment" in full.headers["content-disposition"]
    assert full.headers["cache-control"] == "no-store"


def test_pin_guards_api_and_direct_files(api):
    module, client = api
    e = event(client, "9876")
    pid, _ = upload(client, e["id"])
    root = "/api/albums/" + e["album_key"]
    file = f"/media/{e['album_key']}/{pid}"
    assert client.get(root).status_code == 401
    assert client.get(file).status_code == 401
    assert client.post(root + "/unlock", json={"pin": "wrong"}).status_code == 403
    r = client.post(root + "/unlock", json={"pin": "9876"})
    assert r.status_code == 200 and "HttpOnly" in r.headers["set-cookie"]
    assert client.get(root).status_code == 200
    assert client.get(file).status_code == 200
    # Changing the PIN invalidates an already unlocked browser.
    client.put("/api/events/" + e["id"], json={"title": "Acara", "pin": "5432"})
    assert client.get(file).status_code == 401


def test_moderation_blocks_direct_image_and_thumbnail(api):
    module, client = api
    e = event(client)
    pid, _ = upload(client, e["id"])
    path = f"/api/events/{e['id']}/photos/{pid}"
    assert client.patch(path, json={"visible": False}).status_code == 200
    assert client.get("/api/albums/" + e["album_key"]).json()["photos"] == []
    file = f"/media/{e['album_key']}/{pid}"
    assert client.get(file).status_code == 404
    assert client.get(file + "?thumbnail=true").status_code == 404
    client.patch(path, json={"visible": True})
    assert client.get(file).status_code == 200


def test_bad_file_and_unknown_event_do_not_publish(api):
    module, client = api
    e = event(client)
    assert upload(client, e["id"], data=b"not an image")[1].status_code == 415
    assert upload(client, "0" * 32)[1].status_code == 404
    assert client.get("/api/albums/" + e["album_key"]).json()["photos"] == []
    assert list(module.DATA.rglob("*.part")) == []


def test_cross_event_image_isolation(api):
    _, client = api
    a, b = event(client), event(client)
    pid, _ = upload(client, a["id"])
    assert client.get(f"/media/{b['album_key']}/{pid}").status_code == 404


def test_rate_limit_and_id_validation(api):
    _, client = api
    e = event(client, "1234")
    for i in range(10):
        assert client.post(f"/api/albums/{e['album_key']}/unlock", json={"pin": "bad"}).status_code == 403
    assert client.post(f"/api/albums/{e['album_key']}/unlock", json={"pin": "1234"}).status_code == 429
    assert client.put("/api/events/bad-id", json={"title": "Invalid"}).status_code == 400


def test_qr_encodes_stable_url(api):
    _, client = api
    e = event(client)
    r = client.get(f"/api/events/{e['id']}/qr")
    assert r.status_code == 200 and r.headers["content-type"] == "image/png"
    assert Image.open(io.BytesIO(r.content)).width > 200


def test_guest_shell_has_security_headers(api):
    _, client = api
    e = event(client)
    r = client.get("/a/" + e["album_key"])
    assert r.status_code == 200
    assert "frame-ancestors 'none'" in r.headers["content-security-policy"]
    assert "Alpi" in r.text
