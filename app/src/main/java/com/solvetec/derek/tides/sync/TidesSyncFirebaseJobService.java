package com.solvetec.derek.tides.sync;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.solvetec.derek.tides.utils.DateUtils;

import android.content.Context;
import android.util.Log;

import static com.solvetec.derek.tides.sync.TidesSyncIntentTask.syncTides;

/**
 * Created by Derek on 11/23/2017.
 */

public class TidesSyncFirebaseJobService extends JobService {

    private static final String TAG = TidesSyncFirebaseJobService.class.getSimpleName();
    private static final int NUM_DAYS_TO_SYNC = 10;
    boolean isWorking = false;
    boolean jobCancelled = false;

    // Called by the Android system when it's time to run the job
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "onStartJob: Job started!");
        isWorking = true;
        // We need 'jobParameters' so we can call 'jobFinished'
        startWorkOnNewThread(jobParameters); // Services do NOT run on a separate thread

        return isWorking;
    }

    private void startWorkOnNewThread(final JobParameters jobParameters) {
        new Thread(new Runnable() {
            public void run() {
                doWork(jobParameters);
            }
        }).start();
    }

    private void doWork(JobParameters jobParameters) {
        Context context = getApplicationContext();

        // Sync the next N days from today
        syncTides(context, DateUtils.getStartOfToday(), NUM_DAYS_TO_SYNC);

        Log.d(TAG, "doWork: Job finished!");
        isWorking = false;
        boolean needsReschedule = false;
        jobFinished(jobParameters, needsReschedule);
    }

    // Called if the job was cancelled before being finished
    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "onStopJob: Job cancelled before being completed.");
        jobCancelled = true;
        boolean needsReschedule = isWorking;
        jobFinished(jobParameters, needsReschedule);
        return needsReschedule;
    }


}
