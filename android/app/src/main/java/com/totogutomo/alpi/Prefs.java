package com.totogutomo.alpi;

import android.content.*;

public final class Prefs {
  public static SharedPreferences p(Context c) {
    return c.getSharedPreferences("settings", Context.MODE_PRIVATE);
  }

  public static String get(Context c, String key, String fallback) {
    return p(c).getString(key, fallback);
  }

  public static void set(Context c, String key, String value) {
    p(c).edit().putString(key, value).apply();
  }
}
