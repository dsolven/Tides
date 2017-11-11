package com.solvetec.derek.tides.sync;

import android.content.Context;

import com.Wsdl2Code.WebServices.PredictionsService.SearchParams;
import com.solvetec.derek.tides.utils.PredictionServiceHelper;

/**
 * Created by dsolven on 11/1/2017.
 */

public class TidesSyncIntentTask {

    synchronized public static void syncTides(Context context) {
        try {
            // 11/1/2017 Version 1 is going to pull all the required data for the next 7 days, and discard old data.
            // TODO: 11/1/2017 Version 2 needs to check if the data is already cached in the db, and only incrementally download from the server (to save bandwidth)

            // Get the searchParams to download the WL15 data
            SearchParams sp = PredictionServiceHelper.getWl15SearchParams(context);

            // Download the WL15 data

            // Parse the Wl15 data into a CV[]

            // If cv[] is not null, and length != 0

            // Delete old data

            // Insert new data into database

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
