package com.solvetec.derek.tides.sync;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.Driver;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.Trigger;

import java.util.concurrent.TimeUnit;

/**
 * Created by Derek on 11/23/2017.
 */

public class SyncUtils {

    private static final int SYNC_INTERVAL_HOURS = 24;
    private static final int SYNC_INTERVAL_SECONDS = (int) (TimeUnit.HOURS.toSeconds(SYNC_INTERVAL_HOURS));
    private static final int SYNC_FLEXTIME_SECONDS = SYNC_INTERVAL_SECONDS;

    private static final String TAG = SyncUtils.class.getSimpleName();
    private static final String SYNC_JOB_TAG = "sync_job_tag";

    private static boolean sInitialized;

    synchronized public static void scheduleBackgroundSync(@NonNull final Context context) {

        // If the job has already been initialized, return
        if (sInitialized) return;

        Driver driver = new GooglePlayDriver(context);
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(driver);

        Job constraintSyncJob = dispatcher.newJobBuilder()
                .setService(TidesSyncFirebaseJobService.class)
                .setTag(SYNC_JOB_TAG)
                .setConstraints(Constraint.DEVICE_CHARGING, Constraint.ON_UNMETERED_NETWORK)
                .setLifetime(Lifetime.FOREVER)
                .setRecurring(true)
                .setTrigger(Trigger.executionWindow(
                        SYNC_INTERVAL_SECONDS,
                        SYNC_INTERVAL_SECONDS + SYNC_FLEXTIME_SECONDS))
                .setReplaceCurrent(true)
                .build();

        int scheduleResult = dispatcher.schedule(constraintSyncJob);
        Log.d(TAG, "scheduleBackgroundSync: Job scheduled, with result " + scheduleResult);

        sInitialized = true;
    }

}
