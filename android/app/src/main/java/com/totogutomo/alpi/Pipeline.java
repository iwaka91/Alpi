package com.totogutomo.alpi;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.*;
import android.net.*;
import android.os.*;
import android.provider.MediaStore;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;

/** A serial, durable pipeline: observer signals never process the same item concurrently. */
public final class Pipeline {
  public static final ExecutorService IO = Executors.newSingleThreadExecutor();
  private static final AtomicBoolean enqueued = new AtomicBoolean();
  static boolean recovered;

  public static String permission() {
    return Build.VERSION.SDK_INT >= 33
        ? Manifest.permission.READ_MEDIA_IMAGES
        : Manifest.permission.READ_EXTERNAL_STORAGE;
  }

  public static boolean allowed(Context c) {
    return c.checkSelfPermission(permission()) == PackageManager.PERMISSION_GRANTED;
  }

  public static void kick(Context c) {
    Context app = c.getApplicationContext();
    if (enqueued.compareAndSet(false, true))
      IO.execute(
          () -> {
            try {
              run(app);
            } finally {
              enqueued.set(false);
              app.sendBroadcast(
                  new Intent("com.totogutomo.alpi.UPDATE").setPackage(app.getPackageName()));
            }
          });
  }

  public static long baseline(Context c) {
    try (Cursor cur =
        c.getContentResolver()
            .query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] {"_id"},
                null,
                null,
                "_id DESC")) {
      if (cur != null && cur.moveToFirst()) return cur.getLong(0);
    }
    return 0;
  }

  static long number(JSONObject o, String k) {
    try {
      return Long.parseLong(o.optString(k, "0"));
    } catch (Exception e) {
      return 0;
    }
  }

  static void scan(Context c, Store store) throws Exception {
    if (!allowed(c)) return;
    List<JSONObject> windows =
        store.rows(
            "SELECT * FROM windows WHERE end IS NULL OR end>?",
            Long.toString(System.currentTimeMillis() - 86400000L));
    if (windows.isEmpty()) return;
    long first = Long.MAX_VALUE;
    for (JSONObject w : windows) first = Math.min(first, number(w, "baseline"));
    String[] fields = {
      "_id",
      MediaStore.Images.Media.DATE_TAKEN,
      MediaStore.Images.Media.DATE_ADDED,
      MediaStore.Images.Media.RELATIVE_PATH,
      MediaStore.Images.Media.SIZE
    };
    try (Cursor cursor =
        c.getContentResolver()
            .query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                fields,
                "_id>? AND is_pending=0",
                new String[] {Long.toString(first)},
                "_id ASC")) {
      if (cursor == null) return;
      while (cursor.moveToNext()) {
        long id = cursor.getLong(0),
            taken = cursor.getLong(1),
            added = cursor.getLong(2) * 1000,
            size = cursor.getLong(4);
        String path = cursor.getString(3), source = Prefs.get(c, "folder", "DCIM/Camera/");
        if (path == null || !path.equalsIgnoreCase(source) || size <= 0) continue;
        long moment = taken > 0 ? taken : added;
        for (JSONObject w : windows) {
          long start = number(w, "start"),
              end = w.isNull("end") ? Long.MAX_VALUE : number(w, "end");
          // DATE_TAKEN is millisecond precision. DATE_ADDED is seconds on older providers.
          if (CapturePolicy.accepts(
              number(w, "baseline"), id, start, end, taken, added, path, source)) {
            store.add(
                w,
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    .toString(),
                moment);
            break;
          }
        }
      }
    }
  }

  public static synchronized void run(Context c) {
    Store s = Store.get(c);
    if (!recovered) {
      s.recover();
      Delivery.cleanup(c);
      recovered = true;
    }
    try {
      scan(c, s);
      Prefs.set(c, "scan_error", "");
    } catch (Exception e) {
      Prefs.set(c, "scan_error", safe(e));
    }
    for (JSONObject item :
        s.rows(
            "SELECT * FROM queue WHERE state='NEW' AND next_try<=? ORDER BY created LIMIT 12",
            Long.toString(System.currentTimeMillis()))) {
      String id = item.optString("id");
      try {
        s.state(id, "PROCESSING", null, "", 0);
        String path = process(c, item);
        JSONObject config = new JSONObject(item.getString("config"));
        s.state(id, config.optInt("automatic") == 1 ? "READY" : "REVIEW", path, "", 0);
      } catch (Exception e) {
        s.attempt(id);
        int count = item.optInt("attempts") + 1;
        long retry =
            System.currentTimeMillis() + Math.min(300000, 5000L * (1L << Math.min(count, 6)));
        s.state(id, count >= 5 ? "ERROR" : "NEW", null, safe(e), retry);
      }
    }
    if (!s.rows("SELECT id FROM queue WHERE state='NEW' LIMIT 1").isEmpty()) SyncJob.retry(c);
  }

  public static String safe(Exception e) {
    String msg = e.getMessage();
    return msg == null
        ? e.getClass().getSimpleName()
        : msg.substring(0, Math.min(msg.length(), 250));
  }

  public static Bitmap decode(Context c, Uri uri, int max) throws Exception {
    return ImageDecoder.decodeBitmap(
        ImageDecoder.createSource(c.getContentResolver(), uri),
        (decoder, info, src) -> {
          decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
          int w = info.getSize().getWidth(), h = info.getSize().getHeight();
          float ratio = Math.min(1f, max / (float) Math.max(w, h));
          decoder.setTargetSize(
              Math.max(1, Math.round(w * ratio)), Math.max(1, Math.round(h * ratio)));
        });
  }

  public static String process(Context c, JSONObject item) throws Exception {
    PhotoProcessor.Result result = PhotoProcessor.process(c, item);
    Store.get(c).stats(item.getString("id"), result.stats);
    return result.path;
  }
}
