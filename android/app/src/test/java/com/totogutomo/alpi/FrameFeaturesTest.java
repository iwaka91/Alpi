package com.totogutomo.alpi;

import static org.junit.Assert.*;

import android.content.*;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.*;
import android.net.Uri;
import androidx.exifinterface.media.ExifInterface;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class FrameFeaturesTest {
  private Context context;
  private Store store;
  private final String eventId = "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee";

  private void reset() throws Exception {
    Field f = Store.class.getDeclaredField("instance");
    f.setAccessible(true);
    Store old = (Store) f.get(null);
    if (old != null) old.close();
    f.set(null, null);
  }

  @Before
  public void setup() throws Exception {
    context = RuntimeEnvironment.getApplication();
    reset();
    context.deleteDatabase("alpi.db");
    store = Store.get(context);
    store.eventSave(eventId, "Acara sekolah", "", "https://photos.app.goo.gl/testalbum");
  }

  @After
  public void teardown() throws Exception {
    reset();
  }

  private File image(String name, int w, int h) throws Exception {
    Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    int[] row = new int[w];
    Random random = new Random(22);
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++)
        row[x] =
            Color.rgb(
                (x / 5 + random.nextInt(30)) % 256,
                (y / 5 + random.nextInt(30)) % 256,
                80 + random.nextInt(50));
      bitmap.setPixels(row, 0, w, 0, y, w, 1);
    }
    File file = new File(context.getFilesDir(), name);
    try (OutputStream out = new FileOutputStream(file)) {
      bitmap.compress(Bitmap.CompressFormat.JPEG, 96, out);
    }
    bitmap.recycle();
    return file;
  }

  private JSONObject job(FrameSpec spec, File input, int id) throws Exception {
    JSONObject e = store.event(eventId);
    e.put("processing", spec.data.toString());
    return new JSONObject()
        .put("id", String.format(Locale.US, "%032x", id))
        .put("uri", Uri.fromFile(input).toString())
        .put("config", e.toString());
  }

  @Test
  public void compressionReducesMeasuredSizeWithoutChangingOriginalAndPreservesCaptureTime()
      throws Exception {
    File input = image("original.jpg", 2560, 1920);
    ExifInterface exif = new ExifInterface(input.getAbsolutePath());
    exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "2026:10:28 09:41:12");
    exif.setAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL, "+07:00");
    exif.setLatLong(-2.55, 114.18);
    exif.saveAttributes();
    byte[] original = Files.readAllBytes(input.toPath());
    FrameSpec spec = FrameSpec.fresh();
    spec.preset(0);
    spec.put("mode", 1);
    spec.put("text", "Kombel Maju Cekatan");
    spec.put("show_time", true);
    spec.put("show_location", true);
    PhotoProcessor.Result result = PhotoProcessor.process(context, job(spec, input, 1));
    assertArrayEquals(original, Files.readAllBytes(input.toPath()));
    assertEquals(1600, result.stats.getInt("width"));
    assertEquals(1200, result.stats.getInt("height"));
    assertEquals(input.length(), result.stats.getLong("source"));
    assertTrue(new File(result.path).length() < input.length());
    assertEquals(0, result.stats.getJSONArray("notes").length());
    ExifInterface processed = new ExifInterface(result.path);
    assertEquals(
        "2026:10:28 09:41:12", processed.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL));
    assertEquals("+07:00", processed.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL));
    assertNull(processed.getLatLong());
    Bitmap bitmap = BitmapFactory.decodeFile(result.path);
    assertEquals(1600, bitmap.getWidth());
    bitmap.recycle();
  }

  @Test
  public void missingMetadataIsOmittedAndExplainedWithoutInventingTimeOrCoordinates()
      throws Exception {
    FrameSpec spec = FrameSpec.fresh();
    spec.put("mode", 1);
    spec.put("text", "Kegiatan");
    spec.put("show_time", true);
    spec.put("show_location", true);
    File source = image("no-metadata.jpg", 600, 400);
    PhotoMetadata m = PhotoMetadata.read(context, Uri.fromFile(source), spec);
    assertEquals("", m.time);
    assertEquals("", m.location);
    assertEquals(2, m.notes.size());
    assertEquals("Kegiatan", FramePainter.lines(spec, m));
    PhotoProcessor.Result result = PhotoProcessor.process(context, job(spec, source, 2));
    assertEquals(600, result.stats.getInt("width"));
    assertEquals(400, result.stats.getInt("height"));
    assertEquals(2, result.stats.getJSONArray("notes").length());
  }

  @Test
  public void exifCoordinatesAndWallClockAreReadPrecisely() throws Exception {
    File input = image("metadata.jpg", 120, 90);
    ExifInterface exif = new ExifInterface(input.getAbsolutePath());
    exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "2026:10:28 23:45:01");
    exif.setLatLong(0, 0);
    exif.saveAttributes();
    FrameSpec spec = FrameSpec.fresh();
    spec.put("show_location", true);
    spec.put("show_time", true);
    spec.put("mode", 2);
    PhotoMetadata metadata = PhotoMetadata.read(context, Uri.fromFile(input), spec);
    assertEquals("0.00000, 0.00000", metadata.location);
    assertEquals("28 Okt 2026 23:45:01", metadata.time);
    assertEquals("", PhotoMetadata.cleanDate("2026:02:31 23:45:01"));
    assertEquals("", PhotoMetadata.cleanDate(""));
  }

  @Test
  public void proLayoutsKeepLogoAspectAndAvoidTextAcrossAllPositionPairs() {
    FrameSpec spec = FrameSpec.fresh();
    spec.put("mode", 2);
    spec.put("text", "Kombel Maju Cekatan");
    spec.put("activity", "Pertemuan komunitas belajar");
    spec.put("show_time", true);
    spec.put("show_location", true);
    Bitmap logo = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888);
    for (int[] dimensions : new int[][] {{1200, 1600}, {1600, 1200}})
      for (int t = 0; t < 9; t++)
        for (int l = 0; l < 9; l++) {
          spec.put("text_position", t);
          spec.put("logo_position", l);
          FramePainter.Layout placement =
              FramePainter.layout(dimensions[0], dimensions[1], spec, PhotoMetadata.sample(), logo);
          assertEquals(2f, placement.logo.width() / placement.logo.height(), .001f);
          assertTrue(
              placement.logo.left >= 0
                  && placement.logo.top >= 0
                  && placement.logo.right <= dimensions[0]
                  && placement.logo.bottom <= dimensions[1]);
          assertTrue(
              placement.text.left >= 0
                  && placement.text.top >= 0
                  && placement.text.right <= dimensions[0]
                  && placement.text.bottom <= dimensions[1]);
          assertFalse(
              "Text/logo overlap at " + t + "/" + l,
              RectF.intersects(placement.text, placement.logo));
        }
    logo.recycle();
  }

  @Test
  public void logoTransparencyChangesActualPixelsAndZeroOpacityHidesIt() {
    FrameSpec spec = FrameSpec.fresh();
    spec.put("mode", 2);
    spec.put("text", "");
    spec.put("activity", "");
    spec.put("logo_opacity", 50);
    spec.put("logo_position", 4);
    Bitmap logo = Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888);
    logo.eraseColor(Color.RED);
    Bitmap canvas = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888);
    canvas.eraseColor(Color.WHITE);
    FramePainter.paint(canvas, spec, new PhotoMetadata(), logo);
    int middle = canvas.getPixel(200, 150);
    assertEquals(255, Color.red(middle));
    assertTrue(Color.green(middle) > 110 && Color.green(middle) < 145);
    spec.put("logo_opacity", 0);
    canvas.eraseColor(Color.WHITE);
    FramePainter.paint(canvas, spec, new PhotoMetadata(), logo);
    assertEquals(Color.WHITE, canvas.getPixel(200, 150));
    logo.recycle();
    canvas.recycle();
  }

  @Test
  public void basicAndProIncludeOnlyRequestedTextAndMetadata() {
    FrameSpec spec = FrameSpec.fresh();
    spec.put("mode", 1);
    spec.put("text", "Judul");
    spec.put("activity", "Kegiatan");
    assertEquals("Judul", FramePainter.lines(spec, PhotoMetadata.sample()));
    spec.put("show_time", true);
    spec.put("show_location", true);
    String basic = FramePainter.lines(spec, PhotoMetadata.sample());
    assertTrue(basic.contains("GPS"));
    assertFalse(basic.contains("Kegiatan"));
    spec.put("mode", 2);
    assertTrue(FramePainter.lines(spec, PhotoMetadata.sample()).contains("Kegiatan"));
  }

  @Test
  public void sessionSnapshotAndReprocessingCannotChangeAnActiveHandoff() throws Exception {
    JSONObject event = store.event(eventId);
    FrameSpec spec = FrameSpec.fromEvent(event);
    spec.put("mode", 2);
    spec.put("text", "Awal");
    store.eventSet(eventId, "processing", spec.data.toString());
    store.begin(store.event(eventId), 10);
    store.add(
        store.active(), "content://media/external/images/media/11", System.currentTimeMillis());
    JSONObject q = store.queue().get(0);
    spec.put("text", "Baru");
    store.eventSet(eventId, "processing", spec.data.toString());
    assertEquals("Awal", FrameSpec.fromEvent(new JSONObject(q.getString("config"))).text("text"));
    store.state(q.getString("id"), "READY", null, "", 0);
    store.reprocess(eventId, Collections.singleton(q.getString("id")));
    assertEquals(
        "Baru",
        FrameSpec.fromEvent(new JSONObject(store.photo(q.getString("id")).getString("config")))
            .text("text"));
    store.state(q.getString("id"), "READY", null, "", 0);
    store.createBatch("batch", eventId, Collections.singletonList(q.getString("id")));
    assertThrows(
        IllegalStateException.class,
        () -> store.reprocess(eventId, Collections.singleton(q.getString("id"))));
    assertEquals("SHARED", store.photo(q.getString("id")).getString("state"));
  }

  @Test
  public void legacySettingsAndCorruptValuesRemainBounded() throws Exception {
    FrameSpec legacy = FrameSpec.fromEvent(new JSONObject().put("label", "Pita lama"));
    assertEquals(0, legacy.mode());
    assertEquals(2560, legacy.edge());
    assertEquals(92, legacy.quality());
    FrameSpec fresh = FrameSpec.fromEvent(store.event(eventId));
    assertEquals(2048, fresh.edge());
    assertEquals(82, fresh.quality());
    fresh.put("edge", 99999);
    fresh.put("quality", -50);
    assertEquals(3200, fresh.edge());
    assertEquals(60, fresh.quality());
  }

  @Test
  public void versionTwoMigrationPreservesAlbumFrameAndPendingHandoff() throws Exception {
    reset();
    context.deleteDatabase("alpi.db");
    try (SQLiteDatabase db =
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath("alpi.db"), null)) {
      db.execSQL(
          "CREATE TABLE events(id TEXT PRIMARY KEY,title TEXT,description TEXT,pin TEXT,portrait"
              + " TEXT DEFAULT '',landscape TEXT DEFAULT '',label TEXT DEFAULT '',automatic INTEGER"
              + " DEFAULT 0,album_url TEXT DEFAULT '',google_album_url TEXT DEFAULT '',created"
              + " INTEGER)");
      db.execSQL(
          "CREATE TABLE windows(id INTEGER PRIMARY KEY AUTOINCREMENT,event_id TEXT,start"
              + " INTEGER,end INTEGER,baseline INTEGER,config TEXT)");
      db.execSQL(
          "CREATE TABLE queue(id TEXT PRIMARY KEY,event_id TEXT,uri TEXT,captured INTEGER,config"
              + " TEXT,state TEXT,output TEXT DEFAULT '',error TEXT DEFAULT '',attempts INTEGER"
              + " DEFAULT 0,next_try INTEGER DEFAULT 0,visible INTEGER DEFAULT 1,created"
              + " INTEGER,gallery_uri TEXT DEFAULT '',UNIQUE(event_id,uri))");
      db.execSQL(
          "CREATE TABLE delivery_batches(id TEXT PRIMARY KEY,event_id TEXT,photo_ids TEXT,album_url"
              + " TEXT,state TEXT,created INTEGER)");
      db.execSQL(
          "INSERT INTO events(id,title,portrait,label,google_album_url) VALUES(?,'Acara"
              + " lama','/kept/frame.png','Kombel','https://photos.app.goo.gl/keepalbum')",
          new Object[] {eventId});
      db.execSQL(
          "INSERT INTO queue(id,event_id,state,output)"
              + " VALUES('photo',?,'SHARED','/kept/photo.jpg')",
          new Object[] {eventId});
      db.execSQL(
          "INSERT INTO delivery_batches(id,event_id,photo_ids,state,created)"
              + " VALUES('batch',?,'[\"photo\"]','PENDING',1)",
          new Object[] {eventId});
      db.setVersion(2);
    }
    store = Store.get(context);
    assertEquals("/kept/frame.png", store.event(eventId).getString("portrait"));
    assertEquals(
        "https://photos.app.goo.gl/keepalbum", store.event(eventId).getString("google_album_url"));
    assertEquals(92, FrameSpec.fromEvent(store.event(eventId)).quality());
    assertEquals("SHARED", store.photo("photo").getString("state"));
    assertEquals("/kept/photo.jpg", store.photo("photo").getString("output"));
    assertEquals("", store.photo("photo").getString("stats"));
    assertEquals("batch", store.pendingBatch().getString("id"));
  }
}
