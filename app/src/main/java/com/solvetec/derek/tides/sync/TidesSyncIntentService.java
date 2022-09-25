package com.solvetec.derek.tides.sync;

import android.app.IntentService;
import android.content.Intent;
import androidx.annotation.Nullable;

/**
 * Created by dsolven on 11/1/2017.
 */

public class TidesSyncIntentService extends IntentService {

    public static final String EXTRA_NUM_DAYS_TO_SYNC = "extra_num_days_to_sync";
    public static final String EXTRA_LONG_STARTING_DAY = "extra_long_starting_day";

    public TidesSyncIntentService() {
        super("TidesSyncIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        int numDays = intent.getIntExtra(EXTRA_NUM_DAYS_TO_SYNC, 1);
        Long startingDay = intent.getLongExtra(EXTRA_LONG_STARTING_DAY, 0);

        TidesSyncIntentTask.syncTides(this, startingDay, numDays);
    }
}
