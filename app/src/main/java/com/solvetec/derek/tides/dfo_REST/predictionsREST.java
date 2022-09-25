package com.solvetec.derek.tides.dfo_REST;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.solvetec.derek.tides.data.TidesContract;
import com.solvetec.derek.tides.utils.PredictionServiceHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class predictionsREST
{
    private static final String TAG = "predictionsREST";
    private static predictionsREST instance = null;

    private static final String prefixURL = "https://api-iwls.dfo-mpo.gc.ca/api/v1/";

    //for Volley API
    public RequestQueue requestQueue;

    private predictionsREST(Context context)
    {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        //other stuff if you need
    }

    public static synchronized predictionsREST getInstance(Context context)
    {
        if (null == instance)
            instance = new predictionsREST(context);
        return instance;
    }

    //this is so you don't need to pass context each time
    public static synchronized predictionsREST getInstance()
    {
        if (null == instance)
        {
            throw new IllegalStateException(predictionsREST.class.getSimpleName() +
                    " is not initialized, call getInstance(...) first");
        }
        return instance;
    }

    public void getJSONArrayFromDFOWebservice(String suffix, final SomeCustomListener<JSONArray> listener)
    {

        String url = prefixURL + suffix;
        Log.d(TAG, "getJSONArrayFromWebservice: request: " + url);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>()
                {
                    @Override
                    public void onResponse(JSONArray response)
                    {
                        Log.d(TAG, "getJSONArrayFromWebservice Response : " + response.toString());
                        if(null != response.toString())
                            listener.getResult(response);
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        if (null != error.networkResponse)
                        {
                            Log.d(TAG + ": ", "getJSONArrayFromWebservice Error Response code: " + error.networkResponse.statusCode);
                            listener.getResult(new JSONArray());
                        }
                    }
                });

        requestQueue.add(request);
    }

    public static ContentValues[] parseStationInfo(JSONArray jsonArray) {
        ContentValues[] cvs = new ContentValues[jsonArray.length()];

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject station = jsonArray.getJSONObject(i);
                ContentValues cv = new ContentValues();
                cv.put(TidesContract.TidesEntry.COLUMN_STATION_ID, station.getString("id"));
                cv.put(TidesContract.TidesEntry.COLUMN_STATION_NAME, station.getString("officialName"));
                cv.put(TidesContract.TidesEntry.COLUMN_STATION_LAT, station.getDouble("latitude"));
                cv.put(TidesContract.TidesEntry.COLUMN_STATION_LON, station.getDouble("longitude"));
                cvs[i] = cv;

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return cvs;
    }

    public static ContentValues[] parseWLPData(JSONArray jsonArray, String stationId) {
        ContentValues[] cvs = new ContentValues[jsonArray.length()];

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject entry = jsonArray.getJSONObject(i);
                ContentValues cv = new ContentValues();
                cv.put(TidesContract.TidesEntry.COLUMN_VALUE, entry.getString("value"));
                cv.put(TidesContract.TidesEntry.COLUMN_STATION_ID, stationId);

                SimpleDateFormat sdf = new SimpleDateFormat(PredictionServiceHelper.SEARCH_DATE_FORMAT, Locale.CANADA);
                TimeZone utcTimezone = TimeZone.getTimeZone("UTC");
                sdf.setTimeZone(utcTimezone);
                Date date = sdf.parse(entry.getString("eventDate"));
                cv.put(TidesContract.TidesEntry.COLUMN_DATE, date.getTime());
                cvs[i] = cv;

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return cvs;
    }
}