package com.totogutomo.alpi;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.content.*;
import android.graphics.*;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class FrameEditorTest {
  private ActivityController<FrameEditorActivity> controller;
  private final String eventId = "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee";

  private void closeStore() throws Exception {
    Field f = Store.class.getDeclaredField("instance");
    f.setAccessible(true);
    Store old = (Store) f.get(null);
    if (old != null) old.close();
    f.set(null, null);
  }

  @Before
  public void setup() throws Exception {
    Pipeline.IO.submit(() -> {}).get(20, TimeUnit.SECONDS);
    closeStore();
    Context c = RuntimeEnvironment.getApplication();
    c.deleteDatabase("alpi.db");
    Store.get(c)
        .eventSave(eventId, "Kombel Maju Cekatan", "", "https://photos.app.goo.gl/testalbum");
    controller =
        Robolectric.buildActivity(
                FrameEditorActivity.class,
                new Intent(c, FrameEditorActivity.class)
                    .putExtra("event_id", eventId)
                    .putExtra("tab", "pro"))
            .setup();
  }

  @After
  public void cleanup() throws Exception {
    if (controller != null) controller.pause().stop().destroy();
    Pipeline.IO.submit(() -> {}).get(20, TimeUnit.SECONDS);
    closeStore();
  }

  private TextView text(View root, String value) {
    if (root instanceof TextView && value.contentEquals(((TextView) root).getText()))
      return (TextView) root;
    if (root instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) root;
      for (int n = 0; n < group.getChildCount(); n++) {
        TextView found = text(group.getChildAt(n), value);
        if (found != null) return found;
      }
    }
    return null;
  }

  private TextView find(String value) {
    TextView v = text(controller.get().getWindow().getDecorView(), value);
    assertNotNull("Missing " + value, v);
    return v;
  }

  private EditText firstInput(View root) {
    if (root instanceof EditText) return (EditText) root;
    if (root instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) root;
      for (int n = 0; n < group.getChildCount(); n++) {
        EditText v = firstInput(group.getChildAt(n));
        if (v != null) return v;
      }
    }
    return null;
  }

  @Test
  public void proDraftSurvivesRotationAndSavingPersistsSettings() throws Exception {
    firstInput(controller.get().getWindow().getDecorView()).setText("Teks pilihan Pak Guru");
    ((CheckBox) find("Waktu pengambilan dari metadata")).setChecked(true);
    controller.recreate();
    assertEquals(
        "Teks pilihan Pak Guru",
        firstInput(controller.get().getWindow().getDecorView()).getText().toString());
    assertTrue(((CheckBox) find("Waktu pengambilan dari metadata")).isChecked());
    shadowOf(Looper.getMainLooper()).idleFor(400, TimeUnit.MILLISECONDS);
    Pipeline.IO.submit(() -> {}).get(20, TimeUnit.SECONDS);
    shadowOf(Looper.getMainLooper()).idle();
    View decor = controller.get().getWindow().getDecorView();
    decor.measure(
        View.MeasureSpec.makeMeasureSpec(390, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(844, View.MeasureSpec.EXACTLY));
    decor.layout(0, 0, 390, 844);
    Bitmap screenshot = Bitmap.createBitmap(390, 844, Bitmap.Config.ARGB_8888);
    decor.draw(new Canvas(screenshot));
    File dir = new File("build/reports/qa");
    dir.mkdirs();
    try (OutputStream out = new FileOutputStream(new File(dir, "frame-pro-mobile.png"))) {
      screenshot.compress(Bitmap.CompressFormat.PNG, 100, out);
    }
    screenshot.recycle();
    find("Simpan pengaturan").performClick();
    FrameSpec saved = FrameSpec.fromEvent(Store.get(controller.get()).event(eventId));
    assertEquals(2, saved.mode());
    assertEquals("Teks pilihan Pak Guru", saved.text("text"));
    assertTrue(saved.flag("show_time"));
  }

  @Test
  public void compressionTabDoesNotEnableGeneratorAndPresetCanBeSaved() {
    controller.pause().stop().destroy();
    Context c = RuntimeEnvironment.getApplication();
    controller =
        Robolectric.buildActivity(
                FrameEditorActivity.class,
                new Intent(c, FrameEditorActivity.class)
                    .putExtra("event_id", eventId)
                    .putExtra("tab", "compression"))
            .setup();
    View decor = controller.get().getWindow().getDecorView();
    Spinner preset = firstSpinner(decor);
    assertNotNull(preset);
    preset.setSelection(0);
    decor.measure(
        View.MeasureSpec.makeMeasureSpec(390, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(844, View.MeasureSpec.EXACTLY));
    decor.layout(0, 0, 390, 844);
    shadowOf(Looper.getMainLooper()).idle();
    find("Simpan pengaturan").performClick();
    FrameSpec saved = FrameSpec.fromEvent(Store.get(controller.get()).event(eventId));
    assertEquals(0, saved.mode());
    assertEquals(1600, saved.edge());
    assertEquals(72, saved.quality());
  }

  private Spinner firstSpinner(View root) {
    if (root instanceof Spinner) return (Spinner) root;
    if (root instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) root;
      for (int n = 0; n < group.getChildCount(); n++) {
        Spinner v = firstSpinner(group.getChildAt(n));
        if (v != null) return v;
      }
    }
    return null;
  }
}
