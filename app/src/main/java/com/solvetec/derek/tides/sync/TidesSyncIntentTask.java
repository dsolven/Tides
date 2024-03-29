package com.solvetec.derek.tides.sync;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import com.Wsdl2Code.WebServices.PredictionsService.PredictionsService;
import com.Wsdl2Code.WebServices.PredictionsService.ResultSet;
import com.solvetec.derek.tides.dfo_REST.SearchParams;
import com.solvetec.derek.tides.dfo_REST.SomeCustomListener;
import com.solvetec.derek.tides.dfo_REST.Station;
import com.solvetec.derek.tides.dfo_REST.predictionsREST;
import com.solvetec.derek.tides.utils.PredictionServiceHelper;
import com.solvetec.derek.tides.data.TidesContract.TidesEntry;

import org.json.JSONArray;

/**
 * Created by dsolven on 11/1/2017.
 */

public class TidesSyncIntentTask {

    private static final String TAG = TidesSyncIntentTask.class.getSimpleName();
    private static final int MIN_RESULTS = 97; // 4 per hour * 24 hours + 1 endpoint. We only search for full days.
    private static final int MAX_RESULTS = 961; // 4 per hour * 24 hours * 10 days + 1 endpoint. Max per search = 1000 from webserver.
    private static final String dataFormat = "REST";

    synchronized public static void syncTides(Context context, Long startingDay, int numDaysToSync) {



        switch (dataFormat) {

            case "REST":
                predictionsREST pr = predictionsREST.getInstance();
                String searchTerm = PredictionServiceHelper.getWLPSearchParams(context, startingDay, numDaysToSync);
                pr.getJSONArrayFromDFOWebservice(searchTerm, new SomeCustomListener<JSONArray>()
                {
                    @Override
                    public void getResult(JSONArray result)
                    {
                        if (!result.isNull(0))
                        {
                            Station selectedStation = PredictionServiceHelper.getCurrentStation(context);
                            ContentValues[] cvs = predictionsREST.parseWLPData(result, selectedStation.station_id);
                            int rowsAdded = context.getContentResolver().bulkInsert(TidesEntry.WL15_CONTENT_URI, cvs);
                            Log.d(TAG, "syncTides: Attempted " + cvs.length + " rows, actually added " + rowsAdded + " entries to WL15 database.");
                        }
                    }
                });
                return;

            case "SOAP":
                try {
                    // 11/1/2017 Version 1 is going to pull all the required data for the next 7 days, and discard old data.
                    // TODO: 11/1/2017 Version 2 needs to check if the data is already cached in the db, and only incrementally download from the server (to save bandwidth)

                    // TODO: 11/23/2017 If requested days to sync is > 10, split into multiple requests

                    // Get the searchParams to download the WL15 data
                    SearchParams sp = PredictionServiceHelper.getWl15SearchParams(context, startingDay, numDaysToSync);

                    PredictionsService predictionsService = new PredictionsService();
                    predictionsService.setTimeOut(10); // The default of 180ms was often timing out.

                    ResultSet searchResult = predictionsService.search(sp);
                    if (searchResult == null) {
                        // Something went wrong, probably no cell reception.
                        // TODO: 11/23/2017 Signal something went wrong to the user
                        return;
                    } else if (searchResult.size == 0) {
                        // Returned an empty dataset. Search out of range?
                        // TODO: 11/23/2017 Signal something went wrong to the user
                        return;
                    } else if (searchResult.size < MIN_RESULTS) {
                        // Returned a partially complete dataset. Returned dataset incomplete?
                        // TODO: 11/23/2017 Signal something went wrong to the user
                        return;
                    }

                    ContentValues[] cvs = PredictionServiceHelper.parseSearchResultSet(searchResult);
                    switch (sp.dataName) {
                        case "wl15":
                            context.getContentResolver().bulkInsert(TidesEntry.WL15_CONTENT_URI, cvs);
                            Log.d(TAG, "syncTides: Added " + cvs.length + " entries to WL15 database.");
                            return;
                        default:
                            throw new UnsupportedOperationException("Unsupported searchParam dataName: " + sp.dataName);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
    }
}
