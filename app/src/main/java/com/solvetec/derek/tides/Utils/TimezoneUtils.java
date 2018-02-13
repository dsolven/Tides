
package com.solvetec.derek.tides.utils;

import android.net.Uri;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import java.util.TimeZone;

/**
 * These utilities will be used to communicate with the timezone servers.
 */
public final class TimezoneUtils {

    private static final String TAG = TimezoneUtils.class.getCanonicalName();

    private static final String GOOGLE_TIMEZONE_URL =
            "https://maps.googleapis.com/maps/api/timezone/json?";

    /* The query parameter allows us to provide a location string to the API */
    private static final String LOCATION_PARAM = "location";
    private static final String TIMESTAMP_PARAM = "timestamp";
    private static final String KEY_PARAM = "key";

    /**
     * Retrieves the proper URL to query for the timezone data.
     *
     * @param context used to access other Utility methods
     * @return URL to query timezone service
     */
//    public static URL getUrl(Context context) {
//
//        // TODO: 11/1/2017 Hardcoded lat long for now
////        49.0211,-122.8058
//        double latitude = 49.0211;
//        double longitude = -122.8058;
//        return buildTimezoneUrl(latitude, longitude);
//
//    }

    private static final String TZ_STATUS = "status";
    private static final String TZ_ERRORMESSAGE = "errorMessage";
    private static final String TZ_DSTOFFSET = "dstOffset";
    private static final String TZ_RAWOFFSET = "rawOffset";
    private static final String TZ_TIMEZONEID = "timeZoneId";
    private static final String TZ_TIMEZONENAME = "timeZoneName";


    public static TimeZone parseTimezoneResponse(String response)
            throws JSONException {

        JSONObject timezoneJson = new JSONObject(response);

        // Check the status
        if (timezoneJson.has(TZ_STATUS)) {
            String status = timezoneJson.getString(TZ_STATUS);
            if (status.equals("OK")) {
                Long offset = timezoneJson.getLong(TZ_DSTOFFSET) + timezoneJson.getLong(TZ_RAWOFFSET);
                String timeZoneId = timezoneJson.getString(TZ_TIMEZONEID);
                return TimeZone.getTimeZone(timeZoneId);
            } else {
                // TODO: 11/1/2017 How to handle status != OK?
            }
        } else {
            // TODO: 11/1/2017 How to handle no status returned? HTTP error?
        }

        return null;
    }

    /**
     * Builds the URL used to talk to the weather server using latitude and longitude of a
     * location.
     *
     * @param latitude         The latitude of the location
     * @param longitude        The longitude of the location
     * @param timestamp_in_sec The timestamp of the request, used to calculate daylight savings
     * @return The Url to use to query the timezone server.
     */
    public static URL buildTimezoneUrl(Double latitude, Double longitude, Long timestamp_in_sec) {
        Uri timezoneQueryUri = Uri.parse(GOOGLE_TIMEZONE_URL).buildUpon()
                .appendQueryParameter(LOCATION_PARAM, latitude.toString() + "," + longitude.toString())
                .appendQueryParameter(TIMESTAMP_PARAM, timestamp_in_sec.toString())
                .appendQueryParameter(KEY_PARAM, com.solvetec.derek.tides.BuildConfig.GOOGLE_TIME_ZONE_API_KEY)
                .build();

        try {
            URL timezoneQueryUrl = new URL(timezoneQueryUri.toString());
            Log.v(TAG, "Google Timezone URL: " + timezoneQueryUrl);
            return timezoneQueryUrl;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * This method returns the entire result from the HTTP response.
     *
     * @param url The URL to fetch the HTTP response from.
     * @return The contents of the HTTP response, null if no response
     * @throws IOException Related to network and stream reading
     */
    public static String getResponseFromHttpUrl(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = urlConnection.getInputStream();

            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            String response = null;
            if (hasInput) {
                response = scanner.next();
            }
            scanner.close();
            return response;
        } finally {
            urlConnection.disconnect();
        }
    }
}