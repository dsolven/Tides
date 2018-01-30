package com.solvetec.derek.tides.utils;

import android.net.Uri;
import android.util.Log;

import com.solvetec.derek.tides.SunriseSunset;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

/**
 * Created by Derek on 11/25/2017.
 */

public class SunsetUtils {

    private static final String TAG = SunsetUtils.class.getCanonicalName();

    private static final String SUNRISE_SUNSET_ORG_URL =
            "https://api.sunrise-sunset.org/json?";

    /* The query parameter allows us to provide a location string to the API */
    private static final String LAT_PARAM = "lat";
    private static final String LON_PARAM = "lng";
    private static final String DATE_PARAM = "date";
    private static final String FORMATTED_PARAM = "formatted";
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    private static final String TZ_STATUS = "status";

    public static SunriseSunset getSunriseSunset(Double lat, Double lon, Long date) {

        String dateString = DateUtils.getDateString(date, SunsetUtils.DATE_FORMAT);
        URL url = SunsetUtils.buildSunsetUrl(lat, lon, dateString);

        try {
            String response = SunsetUtils.getResponseFromHttpUrl(url);
            Log.d(TAG, "getSunriseSunset: " + response);
            return SunsetUtils.parseSunsetResponse(response);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static SunriseSunset parseSunsetResponse(String response)
            throws JSONException {

        /* Expect response in the format:
            {
              "results":
              {
                "sunrise":"2015-05-21T05:05:35+00:00",
                "sunset":"2015-05-21T19:22:59+00:00",
                "solar_noon":"2015-05-21T12:14:17+00:00",
                "day_length":51444,
                "civil_twilight_begin":"2015-05-21T04:36:17+00:00",
                "civil_twilight_end":"2015-05-21T19:52:17+00:00",
                "nautical_twilight_begin":"2015-05-21T04:00:13+00:00",
                "nautical_twilight_end":"2015-05-21T20:28:21+00:00",
                "astronomical_twilight_begin":"2015-05-21T03:20:49+00:00",
                "astronomical_twilight_end":"2015-05-21T21:07:45+00:00"
              },
               "status":"OK"
            }
         */

        JSONObject sunsetJson = new JSONObject(response);

        // Check the status
        if (sunsetJson.has(TZ_STATUS)) {
            String status = sunsetJson.getString(TZ_STATUS);
            if (status.equals("OK")) {
                return new SunriseSunset(sunsetJson);

            } else {
                // TODO: 11/1/2017 How to handle status != OK?
            }
        } else {
            // TODO: 11/1/2017 How to handle no status returned? HTTP error?
        }

        return null;
    }

    /**
     * Builds the URL used to talk to the sunrise-sunset server using latitude and longitude of a
     * location.
     *
     * @param latitude         The latitude of the location
     * @param longitude        The longitude of the location
     * @param date             The date, in YYYY-MM-DD format
     * @return The Url to use to query the server.
     */
    public static URL buildSunsetUrl(Double latitude, Double longitude, String date) {
        Uri sunsetQueryUri = Uri.parse(SUNRISE_SUNSET_ORG_URL).buildUpon()
                .appendQueryParameter(LAT_PARAM, latitude.toString())
                .appendQueryParameter(LON_PARAM, longitude.toString())
                .appendQueryParameter(FORMATTED_PARAM, "0")
                .appendQueryParameter(DATE_PARAM, date)
                .build();

        try {
            URL sunsetQueryUrl = new URL(sunsetQueryUri.toString());
            Log.v(TAG, "Sunrise-sunset.org URL: " + sunsetQueryUrl);
            return sunsetQueryUrl;
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
