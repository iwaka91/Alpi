package com.totogutomo.alpi;

import android.app.job.*;
import android.content.*;
import android.provider.MediaStore;

public class SyncJob extends JobService {
  static final int CHANGES = 10, RETRY = 11, PERIODIC = 12;

  public static void watch(Context c) {
    JobScheduler scheduler = (JobScheduler) c.getSystemService(JOB_SCHEDULER_SERVICE);
    ComponentName name = new ComponentName(c, SyncJob.class);
    if (Store.get(c).active() != null) {
      scheduler.schedule(
          new JobInfo.Builder(CHANGES, name)
              .addTriggerContentUri(
                  new JobInfo.TriggerContentUri(
                      MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                      JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS))
              .setTriggerContentUpdateDelay(1500)
              .setTriggerContentMaxDelay(4000)
              .build());
    } else scheduler.cancel(CHANGES);
    scheduler.schedule(
        new JobInfo.Builder(PERIODIC, name).setPeriodic(15 * 60 * 1000).setPersisted(true).build());
    retry(c);
  }

  public static void retry(Context c) {
    JobScheduler scheduler = (JobScheduler) c.getSystemService(JOB_SCHEDULER_SERVICE);
    scheduler.schedule(
        new JobInfo.Builder(RETRY, new ComponentName(c, SyncJob.class))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setMinimumLatency(10000)
            .setBackoffCriteria(30000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
            .build());
  }

  @Override
  public boolean onStartJob(JobParameters params) {
    Pipeline.IO.execute(
        () -> {
          Pipeline.run(this);
          sendBroadcast(new Intent("com.totogutomo.alpi.UPDATE").setPackage(getPackageName()));
          if (params.getJobId() == CHANGES && Store.get(this).active() != null) {
            JobScheduler s = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
            s.schedule(
                new JobInfo.Builder(CHANGES, new ComponentName(this, SyncJob.class))
                    .addTriggerContentUri(
                        new JobInfo.TriggerContentUri(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS))
                    .setTriggerContentUpdateDelay(1500)
                    .setTriggerContentMaxDelay(4000)
                    .build());
          }
          boolean pending =
              !Store.get(this)
                  .rows("SELECT id FROM queue WHERE state IN ('NEW','READY') LIMIT 1")
                  .isEmpty();
          jobFinished(params, params.getJobId() == RETRY && pending);
        });
    return true;
  }

  @Override
  public boolean onStopJob(JobParameters params) {
    return true;
  }
}
