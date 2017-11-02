package com.solvetec.derek.tides.sync;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

/**
 * Created by dsolven on 11/1/2017.
 */

public class TidesSyncIntentService extends IntentService {

    public TidesSyncIntentService() {
        super("TidesSyncIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        TidesSyncIntentTask.syncTides(this);
    }
}
