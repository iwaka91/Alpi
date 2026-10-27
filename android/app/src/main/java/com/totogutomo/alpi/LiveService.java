package com.totogutomo.alpi;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.os.*;
import android.provider.MediaStore;

public class LiveService extends Service {
  static final String CHANNEL = "alpi-session";
  public static volatile boolean running = false;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private ContentObserver observer;
  private final Runnable tick =
      new Runnable() {
        public void run() {
          Pipeline.kick(LiveService.this);
          if (Store.get(LiveService.this).active() == null) {
            stopSelf();
            return;
          }
          handler.postDelayed(this, 5000);
        }
      };

  public static Notification notification(Context c, String text) {
    NotificationManager nm = (NotificationManager) c.getSystemService(NOTIFICATION_SERVICE);
    nm.createNotificationChannel(
        new NotificationChannel(CHANNEL, "Sesi foto Alpi", NotificationManager.IMPORTANCE_LOW));
    PendingIntent open =
        PendingIntent.getActivity(
            c,
            0,
            new Intent(c, MainActivity.class),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    PendingIntent pause =
        PendingIntent.getService(
            c,
            1,
            new Intent(c, LiveService.class).setAction("PAUSE"),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    return new Notification.Builder(c, CHANNEL)
        .setSmallIcon(R.drawable.ic_alpi)
        .setContentTitle("Alpi · sesi aktif")
        .setContentText(text)
        .setOngoing(true)
        .setContentIntent(open)
        .addAction(new Notification.Action.Builder(null, "Jeda sesi", pause).build())
        .build();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent != null && "PAUSE".equals(intent.getAction())) {
      Store.get(this).end();
      SyncJob.watch(this);
      Pipeline.kick(this);
      stopSelf();
      return START_NOT_STICKY;
    }
    if (Store.get(this).active() == null) {
      stopSelf();
      return START_NOT_STICKY;
    }
    try {
      startForeground(
          1,
          notification(this, "Memantau kamera · frame dan unggah otomatis"),
          ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
      running = true;
      Prefs.set(this, "service_error", "");
      if (observer == null) {
        observer =
            new ContentObserver(handler) {
              @Override
              public void onChange(boolean selfChange) {
                Pipeline.kick(LiveService.this);
              }
            };
        getContentResolver()
            .registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer);
      }
      handler.removeCallbacks(tick);
      handler.post(tick);
      SyncJob.watch(this);
    } catch (Exception e) {
      Store.get(this).end();
      Prefs.set(this, "service_error", Pipeline.safe(e));
      stopSelf();
    }
    return START_NOT_STICKY;
  }

  @Override
  public void onTimeout(int startId, int fgsType) {
    Store.get(this).end();
    Prefs.set(
        this,
        "service_error",
        "Batas sesi latar belakang Android tercapai. Buka Alpi dan mulai sesi baru.");
    stopForeground(STOP_FOREGROUND_REMOVE);
    stopSelf();
  }

  @Override
  public void onDestroy() {
    running = false;
    handler.removeCallbacksAndMessages(null);
    if (observer != null) getContentResolver().unregisterContentObserver(observer);
    SyncJob.watch(this);
    super.onDestroy();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
