package com.solvetec.derek.tides.Utils;


import android.util.Log;

import com.solvetec.derek.tides.data.TidesContract;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by dsolven on 10/31/2017.
 */

public class DateUtils {

    private static final String TAG = DateUtils.class.getSimpleName();

    public static long getRightNow() {
        Date now = new Date();
        return now.getTime();
    }

    public static Long getStartOfToday() {
        return getStartOfDay(new Date());
    }

    public static Long getStartOfTomorrow() {
        Calendar cal = Calendar.getInstance(); // by default, returns right now
        cal.roll(Calendar.DAY_OF_YEAR, true); // increment by one day
        return getStartOfDay(cal.getTime());
    }

    public static Long getStartOfDay(Date time) {
        // use UTC time zone
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
//        cal.setTimeZone(TimeZone.);
        // TODO: 11/2/2017 Need to offset the startOfDay by the timezone. We care about getting 0H - 23.75h of the current location.
        cal.setTime(time);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTimeInMillis();
    }

    public static Long getTimezoneOffset(Double lat, Double lon, Long timestamp) {

        Long timestamp_in_sec = timestamp / 1000;
        URL url = TimezoneUtils.buildTimezoneUrl(lat, lon, timestamp_in_sec);

        try {
            String response = TimezoneUtils.getResponseFromHttpUrl(url);
            Log.d(TAG, "getTimezone: " + response);
            return TimezoneUtils.parseTimezoneResponse(response);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String formatForSearchParams(Long millisIn) {
        SimpleDateFormat sdf = new SimpleDateFormat(PredictionServiceHelper.SEARCH_DATE_FORMAT, Locale.CANADA);
        return sdf.format(new Date(millisIn));
    }





}
