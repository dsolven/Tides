package com.solvetec.derek.tides.sync;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

import androidx.work.Data;
import androidx.work.Constraints;
import androidx.work.Constraints.Builder;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.Operation;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * Created by Derek on 11/23/2017.
 */

public class SyncUtils {

    private static final int SYNC_INTERVAL_HOURS = 24;
    private static final int SYNC_INTERVAL_SECONDS = (int) (TimeUnit.HOURS.toSeconds(SYNC_INTERVAL_HOURS));
    private static final int SYNC_FLEXTIME_SECONDS = SYNC_INTERVAL_SECONDS;

    private static final String TAG = SyncUtils.class.getCanonicalName();
    private static final String SYNC_JOB_TAG = "sync_job_tag";

    private static boolean sInitialized;

    synchronized public static void scheduleBackgroundSync(@NonNull final Context context) {

        // If the job has already been initialized, return
        if (sInitialized) {
            Log.d(TAG, "scheduleBackgroundSync: Job already scheduled. Doing nothing.");
            return;
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresCharging(true)
                .build();

        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(TidesSyncWorkManagerJobService.class, SYNC_INTERVAL_HOURS, TimeUnit.HOURS)
                        .build();

        Operation operation = WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(SYNC_JOB_TAG, ExistingPeriodicWorkPolicy.REPLACE, request);


        Log.d(TAG, "scheduleBackgroundSync: Job scheduled, with result " + operation.getResult().toString());

        sInitialized = true;
    }

}
