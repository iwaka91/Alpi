package com.totogutomo.alpi;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;
import org.json.*;

/** Durable metadata. JPEG files and secrets stay in app-private storage. */
public class Store extends SQLiteOpenHelper {
  private static Store instance;

  public static synchronized Store get(Context c) {
    if (instance == null) instance = new Store(c.getApplicationContext());
    return instance;
  }

  private Store(Context c) {
    super(c, "alpi.db", null, 2);
    setWriteAheadLoggingEnabled(true);
  }

  @Override
  public void onCreate(SQLiteDatabase d) {
    d.execSQL(
        "CREATE TABLE events(id TEXT PRIMARY KEY,title TEXT,description TEXT,pin TEXT,portrait TEXT"
            + " DEFAULT '',landscape TEXT DEFAULT '',label TEXT DEFAULT '',automatic INTEGER"
            + " DEFAULT 0,album_url TEXT DEFAULT '',google_album_url TEXT DEFAULT '',created"
            + " INTEGER)");
    d.execSQL(
        "CREATE TABLE windows(id INTEGER PRIMARY KEY AUTOINCREMENT,event_id TEXT,start INTEGER,end"
            + " INTEGER,baseline INTEGER,config TEXT)");
    d.execSQL(
        "CREATE TABLE queue(id TEXT PRIMARY KEY,event_id TEXT,uri TEXT,captured INTEGER,config"
            + " TEXT,state TEXT,output TEXT DEFAULT '',error TEXT DEFAULT '',attempts INTEGER"
            + " DEFAULT 0,next_try INTEGER DEFAULT 0,visible INTEGER DEFAULT 1,created"
            + " INTEGER,gallery_uri TEXT DEFAULT '',UNIQUE(event_id,uri))");
    createDeliveries(d);
  }

  private void createDeliveries(SQLiteDatabase d) {
    d.execSQL(
        "CREATE TABLE delivery_batches(id TEXT PRIMARY KEY,event_id TEXT,photo_ids TEXT,album_url"
            + " TEXT,state TEXT,created INTEGER)");
  }

  @Override
  public void onUpgrade(SQLiteDatabase d, int old, int next) {
    if (old < 2) {
      // Keep server history and URLs separate. An old server link must never become a Google QR.
      d.execSQL("ALTER TABLE events ADD COLUMN google_album_url TEXT DEFAULT ''");
      d.execSQL("ALTER TABLE queue ADD COLUMN gallery_uri TEXT DEFAULT ''");
      d.execSQL("UPDATE queue SET state='READY',error='',next_try=0 WHERE state='UPLOADING'");
      createDeliveries(d);
    }
  }

  public synchronized List<JSONObject> rows(String sql, String... args) {
    List<JSONObject> out = new ArrayList<>();
    try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
      while (c.moveToNext()) {
        JSONObject o = new JSONObject();
        for (int i = 0; i < c.getColumnCount(); i++) {
          try {
            o.put(c.getColumnName(i), c.isNull(i) ? JSONObject.NULL : c.getString(i));
          } catch (JSONException e) {
            throw new IllegalStateException(e);
          }
        }
        out.add(o);
      }
    }
    return out;
  }

  public JSONObject event(String id) {
    List<JSONObject> r = rows("SELECT * FROM events WHERE id=?", id);
    return r.isEmpty() ? null : r.get(0);
  }

  public List<JSONObject> events() {
    return rows("SELECT * FROM events ORDER BY created DESC");
  }

  public List<JSONObject> queue() {
    return rows(
        "SELECT q.*,e.title FROM queue q JOIN events e ON e.id=q.event_id ORDER BY q.created DESC");
  }

  public synchronized void eventSave(String id, String title, String description, String albumUrl) {
    ContentValues v = new ContentValues();
    v.put("title", title);
    v.put("description", description);
    v.put("google_album_url", AlbumLinks.normalize(albumUrl));
    if (event(id) == null) {
      v.put("id", id);
      v.put("created", System.currentTimeMillis());
      getWritableDatabase().insertOrThrow("events", null, v);
    } else getWritableDatabase().update("events", v, "id=?", new String[] {id});
  }

  public synchronized void eventSet(String id, String field, String value) {
    if (!Arrays.asList("portrait", "landscape", "label", "automatic", "google_album_url")
        .contains(field)) throw new IllegalArgumentException();
    ContentValues v = new ContentValues();
    v.put(field, value);
    getWritableDatabase().update("events", v, "id=?", new String[] {id});
  }

  public synchronized void begin(JSONObject event, long baseline) {
    end();
    ContentValues v = new ContentValues();
    v.put("event_id", event.optString("id"));
    v.put("start", System.currentTimeMillis());
    v.putNull("end");
    v.put("baseline", baseline);
    v.put("config", event.toString());
    getWritableDatabase().insertOrThrow("windows", null, v);
  }

  public synchronized void end() {
    ContentValues v = new ContentValues();
    v.put("end", System.currentTimeMillis());
    getWritableDatabase().update("windows", v, "end IS NULL", null);
  }

  public JSONObject active() {
    List<JSONObject> r = rows("SELECT * FROM windows WHERE end IS NULL ORDER BY id DESC LIMIT 1");
    return r.isEmpty() ? null : r.get(0);
  }

  public synchronized void add(JSONObject window, String uri, long taken) {
    ContentValues v = new ContentValues();
    v.put("id", UUID.randomUUID().toString().replace("-", ""));
    v.put("event_id", window.optString("event_id"));
    v.put("uri", uri);
    v.put("captured", taken);
    v.put("config", window.optString("config"));
    v.put("state", "NEW");
    v.put("created", System.currentTimeMillis());
    getWritableDatabase().insertWithOnConflict("queue", null, v, SQLiteDatabase.CONFLICT_IGNORE);
  }

  public synchronized void state(String id, String state, String output, String error, long retry) {
    ContentValues v = new ContentValues();
    v.put("state", state);
    v.put("error", error);
    v.put("next_try", retry);
    if (output != null) v.put("output", output);
    getWritableDatabase().update("queue", v, "id=?", new String[] {id});
  }

  public synchronized void visibility(String id, boolean visible) {
    ContentValues v = new ContentValues();
    v.put("visible", visible ? 1 : 0);
    getWritableDatabase().update("queue", v, "id=?", new String[] {id});
  }

  public synchronized void attempt(String id) {
    getWritableDatabase()
        .execSQL("UPDATE queue SET attempts=attempts+1 WHERE id=?", new Object[] {id});
  }

  public synchronized void recover() {
    getWritableDatabase().execSQL("UPDATE queue SET state='NEW' WHERE state='PROCESSING'");
    getWritableDatabase().execSQL("UPDATE queue SET state='READY' WHERE state='UPLOADING'");
  }

  public JSONObject photo(String id) {
    List<JSONObject> r =
        rows("SELECT q.*,e.title FROM queue q JOIN events e ON e.id=q.event_id WHERE q.id=?", id);
    return r.isEmpty() ? null : r.get(0);
  }

  public synchronized void gallery(String id, String uri) {
    ContentValues v = new ContentValues();
    v.put("gallery_uri", uri);
    getWritableDatabase().update("queue", v, "id=?", new String[] {id});
  }

  public JSONObject pendingBatch() {
    List<JSONObject> r =
        rows("SELECT * FROM delivery_batches WHERE state='PENDING' ORDER BY created DESC LIMIT 1");
    return r.isEmpty() ? null : r.get(0);
  }

  public synchronized void createBatch(String id, String eventId, List<String> ids)
      throws Exception {
    if (ids.isEmpty() || ids.size() > Delivery.MAX_BATCH || new HashSet<>(ids).size() != ids.size())
      throw new IllegalArgumentException("Pilih 1–30 foto yang berbeda.");
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      if (pendingBatch() != null)
        throw new IllegalStateException("Masih ada kiriman menunggu konfirmasi.");
      JSONObject e = event(eventId);
      if (e == null) throw new IllegalArgumentException("Acara tidak ditemukan.");
      for (String photoId : ids) {
        ContentValues changed = new ContentValues();
        changed.put("state", "SHARED");
        if (db.update(
                "queue",
                changed,
                "id=? AND event_id=? AND state='READY'",
                new String[] {photoId, eventId})
            != 1)
          throw new IllegalStateException("Foto harus siap kirim dan berasal dari satu acara.");
      }
      ContentValues v = new ContentValues();
      v.put("id", id);
      v.put("event_id", eventId);
      v.put("photo_ids", new JSONArray(ids).toString());
      v.put("album_url", e.optString("google_album_url"));
      v.put("state", "PENDING");
      v.put("created", System.currentTimeMillis());
      db.insertOrThrow("delivery_batches", null, v);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  public synchronized void finishBatch(String batchId, boolean confirmed) throws Exception {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      List<JSONObject> found =
          rows("SELECT * FROM delivery_batches WHERE id=? AND state='PENDING'", batchId);
      if (found.isEmpty()) throw new IllegalStateException("Kiriman ini sudah ditangani.");
      JSONArray ids = new JSONArray(found.get(0).getString("photo_ids"));
      for (int n = 0; n < ids.length(); n++) {
        ContentValues v = new ContentValues();
        v.put("state", confirmed ? "SENT" : "READY");
        v.put("error", "");
        db.update("queue", v, "id=? AND state='SHARED'", new String[] {ids.getString(n)});
      }
      ContentValues v = new ContentValues();
      v.put("state", confirmed ? "CONFIRMED" : "CANCELLED");
      db.update("delivery_batches", v, "id=?", new String[] {batchId});
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }
}
