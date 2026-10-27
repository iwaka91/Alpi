package com.totogutomo.alpi;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.content.*;
import android.graphics.Bitmap;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
@LooperMode(LooperMode.Mode.PAUSED)
public class SharingFlowTest {
  private ActivityController<MainActivity> controller;
  private Context context;

  private void resetStore() throws Exception {
    Field field = Store.class.getDeclaredField("instance");
    field.setAccessible(true);
    Store old = (Store) field.get(null);
    if (old != null) old.close();
    field.set(null, null);
  }

  @Before
  public void setup() throws Exception {
    context = RuntimeEnvironment.getApplication();
    Pipeline.IO.submit(() -> {}).get(10, TimeUnit.SECONDS);
    resetStore();
    context.deleteDatabase("alpi.db");
    Prefs.p(context).edit().clear().commit();
    String event = "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee";
    Store store = Store.get(context);
    store.eventSave(event, "Acara uji", "", "https://photos.app.goo.gl/testalbum");
    Prefs.set(context, "selected", event);
    File dir = new File(context.getFilesDir(), "results");
    dir.mkdirs();
    File image = new File(dir, "00000000000000000000000000000001.jpg");
    Bitmap bitmap = Bitmap.createBitmap(120, 90, Bitmap.Config.ARGB_8888);
    try (OutputStream out = new FileOutputStream(image)) {
      bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
    }
    bitmap.recycle();
    ContentValues v = new ContentValues();
    v.put("id", "00000000000000000000000000000001");
    v.put("event_id", event);
    v.put("state", "READY");
    v.put("output", image.getAbsolutePath());
    v.put("created", 1);
    v.put("uri", "content://media/external/images/media/1");
    store.getWritableDatabase().insertOrThrow("queue", null, v);
    controller = Robolectric.buildActivity(MainActivity.class).setup();
    Pipeline.IO.submit(() -> {}).get(10, TimeUnit.SECONDS);
    shadowOf(Looper.getMainLooper()).idle();
  }

  @After
  public void teardown() throws Exception {
    if (controller != null) controller.pause().stop().destroy();
    Pipeline.IO.submit(() -> {}).get(10, TimeUnit.SECONDS);
    resetStore();
  }

  private TextView text(View root, String label) {
    if (root instanceof TextView && label.contentEquals(((TextView) root).getText()))
      return (TextView) root;
    if (root instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) root;
      for (int i = 0; i < group.getChildCount(); i++) {
        TextView found = text(group.getChildAt(i), label);
        if (found != null) return found;
      }
    }
    return null;
  }

  private TextView find(String label) {
    TextView found = text(controller.get().getWindow().getDecorView(), label);
    assertNotNull("Expected visible control: " + label, found);
    return found;
  }

  @Test
  public void selectionSurvivesRecreationAndSharingDoesNotMarkSuccess() throws Exception {
    find("Siap Kirim").performClick();
    ((CheckBox) find("Pilih foto ini")).setChecked(true);
    controller.recreate();
    Pipeline.IO.submit(() -> {}).get(10, TimeUnit.SECONDS);
    shadowOf(Looper.getMainLooper()).idle();
    assertTrue(((CheckBox) find("Pilih foto ini")).isChecked());
    find("Kirim pilihan ke Google Photos").performClick();
    Pipeline.IO.submit(() -> {}).get(10, TimeUnit.SECONDS);
    shadowOf(Looper.getMainLooper()).idle();
    JSONObject pending = Store.get(context).pendingBatch();
    assertNotNull(pending);
    find("Menunggu konfirmasi kiriman");
    assertEquals(
        "SHARED", Store.get(context).photo("00000000000000000000000000000001").getString("state"));
    Intent launched = shadowOf(controller.get()).getNextStartedActivity();
    assertNotNull(launched);
    assertEquals(Intent.ACTION_CHOOSER, launched.getAction());
    Intent offered = launched.getParcelableExtra(Intent.EXTRA_INTENT);
    assertEquals("image/jpeg", offered.getType());
    assertEquals(1, offered.getClipData().getItemCount());
    find("Belum jadi / kembalikan ke Siap Kirim").performClick();
    assertNull(Store.get(context).pendingBatch());
    assertEquals(
        "READY", Store.get(context).photo("00000000000000000000000000000001").getString("state"));
  }
}
