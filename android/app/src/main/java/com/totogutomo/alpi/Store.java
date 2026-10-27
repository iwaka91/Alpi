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
    super(c, "alpi.db", null, 1);
    setWriteAheadLoggingEnabled(true);
  }

  @Override
  public void onCreate(SQLiteDatabase d) {
    d.execSQL(
        "CREATE TABLE events(id TEXT PRIMARY KEY,title TEXT,description TEXT,pin TEXT,portrait TEXT"
            + " DEFAULT '',landscape TEXT DEFAULT '',label TEXT DEFAULT '',automatic INTEGER"
            + " DEFAULT 0,album_url TEXT DEFAULT '',created INTEGER)");
    d.execSQL(
        "CREATE TABLE windows(id INTEGER PRIMARY KEY AUTOINCREMENT,event_id TEXT,start INTEGER,end"
            + " INTEGER,baseline INTEGER,config TEXT)");
    d.execSQL(
        "CREATE TABLE queue(id TEXT PRIMARY KEY,event_id TEXT,uri TEXT,captured INTEGER,config"
            + " TEXT,state TEXT,output TEXT DEFAULT '',error TEXT DEFAULT '',attempts INTEGER"
            + " DEFAULT 0,next_try INTEGER DEFAULT 0,visible INTEGER DEFAULT 1,created"
            + " INTEGER,UNIQUE(event_id,uri))");
  }

  @Override
  public void onUpgrade(SQLiteDatabase d, int old, int next) {
    throw new IllegalStateException("Unsupported DB upgrade");
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

  public synchronized void eventSave(String id, String title, String description, String pin) {
    ContentValues v = new ContentValues();
    v.put("title", title);
    v.put("description", description);
    v.put("pin", pin);
    if (event(id) == null) {
      v.put("id", id);
      v.put("created", System.currentTimeMillis());
      getWritableDatabase().insertOrThrow("events", null, v);
    } else getWritableDatabase().update("events", v, "id=?", new String[] {id});
  }

  public synchronized void eventSet(String id, String field, String value) {
    if (!Arrays.asList("portrait", "landscape", "label", "automatic", "album_url").contains(field))
      throw new IllegalArgumentException();
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
}
