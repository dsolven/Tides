package com.solvetec.derek.tides.sync;

import com.solvetec.derek.tides.dfo_REST.Station;
import com.solvetec.derek.tides.utils.DateUtils;

import android.app.job.JobParameters;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.TimeZone;

import static com.solvetec.derek.tides.sync.TidesSyncIntentTask.syncTides;
import static com.solvetec.derek.tides.utils.PredictionServiceHelper.getCurrentStation;

/**
 * Created by Derek on 11/23/2017.
 */

public class TidesSyncWorkManagerJobService extends Worker {

    private static final String TAG = TidesSyncWorkManagerJobService.class.getSimpleName();
    private static final int NUM_DAYS_TO_SYNC = 6;
    boolean isWorking = false;
    boolean jobCancelled = false;

    public TidesSyncWorkManagerJobService(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        // Get the current station
        Station s = getCurrentStation(context);
        TimeZone timeZone = TimeZone.getTimeZone(s.timezone_id);

        // Sync the next N days from today
        Long startOfToday = DateUtils.getStartOfToday();
        Log.d(TAG, "doWork: Starting sync of " + NUM_DAYS_TO_SYNC + " days from server, starting with " + startOfToday + ".");
        syncTides(context, startOfToday, NUM_DAYS_TO_SYNC);

        Log.d(TAG, "doWork: Job finished!");
//        isWorking = false;
//        boolean needsReschedule = false;
//        jobFinished(jobParameters, needsReschedule);
        return Result.success();
    }

    // Called if the job was cancelled before being finished
    @Override
    public void onStopped() {
        Log.d(TAG, "onStopJob: Job cancelled before being completed.");
//        jobCancelled = true;
//        boolean needsReschedule = isWorking;
//        jobFinished(jobParameters, needsReschedule);
//        return needsReschedule;
    }


}
