package com.totogutomo.alpi;

import static org.junit.Assert.*;

import android.content.*;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.*;
import android.net.Uri;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DeliveryTest {
  private Context context;
  private Store store;
  private static final String EVENT = "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee";
  private static final String OTHER = "ffffffffffffffffffffffffffffffff";
  private static final String LINK = "https://photos.app.goo.gl/wcFbg5Dcf13mkyKx9";

  private void closeStore() throws Exception {
    Field instance = Store.class.getDeclaredField("instance");
    instance.setAccessible(true);
    Store open = (Store) instance.get(null);
    if (open != null) open.close();
    instance.set(null, null);
  }

  @Before
  public void setup() throws Exception {
    context = RuntimeEnvironment.getApplication();
    closeStore();
    context.deleteDatabase("alpi.db");
    store = Store.get(context);
    store.eventSave(EVENT, "Pentas sekolah", "", LINK);
    store.eventSave(OTHER, "Acara lain", "", LINK);
  }

  @After
  public void teardown() throws Exception {
    closeStore();
  }

  private String photo(String event, int n) throws Exception {
    String id = String.format(Locale.ROOT, "%032x", n);
    File dir = new File(context.getFilesDir(), "results");
    dir.mkdirs();
    File file = new File(dir, id + ".jpg");
    try (OutputStream out = new FileOutputStream(file)) {
      out.write(new byte[] {1, 2, 3, (byte) n});
    }
    ContentValues v = new ContentValues();
    v.put("id", id);
    v.put("event_id", event);
    v.put("uri", "content://media/external/images/media/" + n);
    v.put("state", "READY");
    v.put("output", file.getAbsolutePath());
    v.put("created", n);
    v.put("config", store.event(event).toString());
    store.getWritableDatabase().insertOrThrow("queue", null, v);
    return id;
  }

  @Test
  public void handoffFreezesFileAndWaitsForExplicitConfirmation() throws Exception {
    String id = photo(EVENT, 1);
    JSONObject batch = Delivery.prepare(context, EVENT, Collections.singleton(id));
    assertEquals("SHARED", store.photo(id).getString("state"));
    Intent intent = Delivery.intent(context, batch, true);
    assertEquals(Intent.ACTION_SEND, intent.getAction());
    assertEquals(Delivery.PHOTOS_PACKAGE, intent.getPackage());
    assertTrue((intent.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
    assertEquals(1, intent.getClipData().getItemCount());
    Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
    File snapshot =
        new File(new File(context.getFilesDir(), "deliveries"), uri.getLastPathSegment());
    byte[] before = Files.readAllBytes(snapshot.toPath());
    Files.write(new File(store.photo(id).getString("output")).toPath(), new byte[] {9});
    assertArrayEquals(before, Files.readAllBytes(snapshot.toPath()));
    store.recover();
    assertEquals("SHARED", store.photo(id).getString("state"));
    store.finishBatch(batch.getString("id"), true);
    assertEquals("SENT", store.photo(id).getString("state"));
    assertEquals("Ditandai selesai oleh operator", MainActivity.label("SENT"));
    assertNull(store.pendingBatch());
    assertThrows(IllegalStateException.class, () -> store.finishBatch(batch.getString("id"), true));
  }

  @Test
  public void cancelledHandoffCanBeSelectedAgain() throws Exception {
    String a = photo(EVENT, 1), b = photo(EVENT, 2);
    JSONObject batch = Delivery.prepare(context, EVENT, Arrays.asList(a, b));
    Intent intent = Delivery.intent(context, batch, false);
    assertEquals(Intent.ACTION_SEND_MULTIPLE, intent.getAction());
    assertEquals(2, intent.getClipData().getItemCount());
    assertEquals(2, intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM).size());
    assertNull(intent.getPackage());
    store.finishBatch(batch.getString("id"), false);
    assertEquals("READY", store.photo(a).getString("state"));
    assertEquals("READY", store.photo(b).getString("state"));
    assertNotNull(Delivery.prepare(context, EVENT, Arrays.asList(a, b)));
  }

  @Test
  public void batchSurvivesDatabaseReopenWithoutClaimingUploadSuccess() throws Exception {
    String id = photo(EVENT, 1);
    String batchId = Delivery.prepare(context, EVENT, Collections.singleton(id)).getString("id");
    closeStore();
    store = Store.get(context);
    assertEquals(batchId, store.pendingBatch().getString("id"));
    assertEquals("SHARED", store.photo(id).getString("state"));
    assertEquals(
        1, Delivery.intent(context, store.pendingBatch(), true).getClipData().getItemCount());
  }

  @Test
  public void batchesCannotMixEventsAndFailedTransactionRestoresReadyPhotos() throws Exception {
    String a = photo(EVENT, 1), b = photo(OTHER, 2);
    assertThrows(
        IllegalStateException.class, () -> store.createBatch("batch", EVENT, Arrays.asList(a, b)));
    assertEquals("READY", store.photo(a).getString("state"));
    assertNull(store.pendingBatch());
    assertThrows(IOException.class, () -> Delivery.prepare(context, EVENT, Arrays.asList(a, b)));
    assertNull(store.pendingBatch());
  }

  @Test
  public void onlyOnePendingBatchAndThirtyDifferentPhotosAreAllowed() throws Exception {
    String a = photo(EVENT, 1), b = photo(EVENT, 2);
    assertThrows(
        IllegalArgumentException.class,
        () -> store.createBatch("duplicate", EVENT, Arrays.asList(a, a)));
    assertThrows(
        IOException.class, () -> Delivery.prepare(context, EVENT, Collections.emptyList()));
    assertThrows(
        IOException.class, () -> Delivery.prepare(context, EVENT, Collections.nCopies(31, a)));
    Delivery.prepare(context, EVENT, Collections.singleton(a));
    assertThrows(
        IOException.class, () -> Delivery.prepare(context, EVENT, Collections.singleton(b)));
    assertEquals("READY", store.photo(b).getString("state"));
  }

  @Test
  public void missingOutputDoesNotCreatePendingBatch() throws Exception {
    String id = photo(EVENT, 1);
    new File(store.photo(id).getString("output")).delete();
    assertThrows(
        IOException.class, () -> Delivery.prepare(context, EVENT, Collections.singleton(id)));
    assertEquals("READY", store.photo(id).getString("state"));
    assertNull(store.pendingBatch());
  }

  @Test
  public void qrDecodesToExactSharedAlbumLinkOffline() throws Exception {
    File file = Delivery.qr(context, LINK, EVENT);
    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
    int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
    bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
    Result result =
        new MultiFormatReader()
            .decode(
                new BinaryBitmap(
                    new HybridBinarizer(
                        new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), pixels))));
    assertEquals(LINK, result.getText());
    bitmap.recycle();
  }

  @Test
  public void migrationPreservesOldHistoryAndDoesNotReuseOldServerUrl() throws Exception {
    closeStore();
    context.deleteDatabase("alpi.db");
    File path = context.getDatabasePath("alpi.db");
    try (SQLiteDatabase old = SQLiteDatabase.openOrCreateDatabase(path, null)) {
      old.execSQL(
          "CREATE TABLE events(id TEXT PRIMARY KEY,title TEXT,description TEXT,pin TEXT,portrait"
              + " TEXT DEFAULT '',landscape TEXT DEFAULT '',label TEXT DEFAULT '',automatic INTEGER"
              + " DEFAULT 0,album_url TEXT DEFAULT '',created INTEGER)");
      old.execSQL(
          "CREATE TABLE windows(id INTEGER PRIMARY KEY AUTOINCREMENT,event_id TEXT,start"
              + " INTEGER,end INTEGER,baseline INTEGER,config TEXT)");
      old.execSQL(
          "CREATE TABLE queue(id TEXT PRIMARY KEY,event_id TEXT,uri TEXT,captured INTEGER,config"
              + " TEXT,state TEXT,output TEXT DEFAULT '',error TEXT DEFAULT '',attempts INTEGER"
              + " DEFAULT 0,next_try INTEGER DEFAULT 0,visible INTEGER DEFAULT 1,created"
              + " INTEGER,UNIQUE(event_id,uri))");
      old.execSQL(
          "INSERT INTO events(id,title,album_url) VALUES(?,?,'https://old.example/a/secret')",
          new Object[] {EVENT, "Lama"});
      old.execSQL(
          "INSERT INTO queue(id,event_id,state,output) VALUES('oldphoto',?,'DONE','/kept.jpg')",
          new Object[] {EVENT});
      old.execSQL(
          "INSERT INTO queue(id,event_id,state) VALUES('inflight',?,'UPLOADING')",
          new Object[] {EVENT});
      old.setVersion(1);
    }
    store = Store.get(context);
    assertEquals("", store.event(EVENT).getString("google_album_url"));
    assertEquals("https://old.example/a/secret", store.event(EVENT).getString("album_url"));
    assertEquals("DONE", store.photo("oldphoto").getString("state"));
    assertEquals("/kept.jpg", store.photo("oldphoto").getString("output"));
    assertEquals("READY", store.photo("inflight").getString("state"));
  }
}
