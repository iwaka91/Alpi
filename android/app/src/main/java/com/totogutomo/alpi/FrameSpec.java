package com.totogutomo.alpi;

import java.util.Locale;
import org.json.*;

/** One immutable session snapshot is stored with each queued photo. */
public final class FrameSpec {
  public final JSONObject data;
  public static final String[] POSITIONS = {
    "Atas kiri",
    "Atas tengah",
    "Atas kanan",
    "Tengah kiri",
    "Tengah",
    "Tengah kanan",
    "Bawah kiri",
    "Bawah tengah",
    "Bawah kanan"
  };
  public static final int[] EDGES = {1280, 1600, 2048, 2560, 3200};
  public static final String[] PRESETS = {
    "Hemat · 1600 px / JPEG 72",
    "Seimbang · 2048 px / JPEG 82",
    "Detail · 2560 px / JPEG 90",
    "Kustom"
  };

  public FrameSpec(JSONObject data) {
    this.data = data;
  }

  public static FrameSpec fromEvent(JSONObject event) {
    try {
      String stored = event.optString("processing");
      if (!stored.isEmpty()) return new FrameSpec(new JSONObject(stored));
    } catch (JSONException ignored) {
    }
    // Existing events and queued photos keep the 0.2 dimensions/quality until edited.
    FrameSpec spec = fresh();
    spec.put("edge", 2560);
    spec.put("quality", 92);
    spec.put("text", event.optString("label"));
    return spec;
  }

  public static FrameSpec fresh() {
    JSONObject d = new JSONObject();
    FrameSpec s = new FrameSpec(d);
    s.put("mode", 0);
    s.put("edge", 2048);
    s.put("quality", 82);
    s.put("text", "");
    s.put("activity", "");
    s.put("show_time", false);
    s.put("show_location", false);
    s.put("text_position", 7);
    s.put("logo_position", 1);
    s.put("logo", "");
    s.put("logo_opacity", 100);
    s.put("logo_scale", 18);
    s.put("text_scale", 100);
    s.put("band", true);
    s.put("theme", 0);
    return s;
  }

  public void put(String key, Object value) {
    try {
      data.put(key, value);
    } catch (JSONException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public int number(String key, int fallback, int low, int high) {
    return Math.max(low, Math.min(high, data.optInt(key, fallback)));
  }

  public int mode() {
    return number("mode", 0, 0, 2);
  }

  public int edge() {
    return number("edge", 2048, 1280, 3200);
  }

  public int quality() {
    return number("quality", 82, 60, 95);
  }

  public boolean flag(String key) {
    return data.optBoolean(key, false);
  }

  public String text(String key) {
    return data.optString(key, "");
  }

  public int preset() {
    if (edge() == 1600 && quality() == 72) return 0;
    if (edge() == 2048 && quality() == 82) return 1;
    if (edge() == 2560 && quality() == 90) return 2;
    return 3;
  }

  public void preset(int index) {
    if (index < 0 || index > 2) return;
    put("edge", new int[] {1600, 2048, 2560}[index]);
    put("quality", new int[] {72, 82, 90}[index]);
  }

  public String summary() {
    return edge() + " px · JPEG " + quality();
  }

  public String modeName() {
    return new String[] {"PNG / pita lama", "Basic", "Pro"}[mode()];
  }

  public static String size(long bytes) {
    if (bytes <= 0) return "belum diketahui";
    return bytes < 1024 * 1024
        ? String.format(Locale.getDefault(), "%.0f KB", bytes / 1024d)
        : String.format(Locale.getDefault(), "%.2f MB", bytes / (1024d * 1024));
  }
}
