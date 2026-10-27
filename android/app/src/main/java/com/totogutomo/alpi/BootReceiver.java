package com.totogutomo.alpi;

import android.content.*;

public class BootReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context c, Intent i) {
    if (Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())) {
      Store.get(c).end();
      Prefs.set(
          c,
          "service_error",
          "HP dimulai ulang. Buka Alpi untuk memulai sesi baru; antrean lama tetap tersimpan.");
      SyncJob.watch(c);
    }
  }
}
