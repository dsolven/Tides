package com.solvetec.derek.tides.utils;


import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import com.solvetec.derek.tides.data.TidesContract;
import com.solvetec.derek.tides.dfo_REST.SomeCustomListener;
import com.solvetec.derek.tides.dfo_REST.Station;
import com.solvetec.derek.tides.dfo_REST.predictionsREST;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by dsolven on 10/31/2017.
 */

public class DateUtils {

    private static final String TAG = DateUtils.class.getCanonicalName();
    private TimeZone mTimeZone;

    public DateUtils(TimeZone timeZone) {
        this.mTimeZone = timeZone;
    }

    public static Long getRightNow() {
        Calendar cal = Calendar.getInstance(TimeZone.getDefault()); // by default, returns right now
        return cal.getTimeInMillis();
    }

    public static Long getStartOfToday() {
        Calendar cal = Calendar.getInstance(TimeZone.getDefault()); // by default, returns right now
        return getStartOfDay(cal);
    }

    public Long getStartOfTomorrow() {
        Calendar cal = Calendar.getInstance(mTimeZone); // by default, returns right now
        cal.add(Calendar.DAY_OF_YEAR, 1); // increment by one day
        return getStartOfDay(cal.getTimeInMillis());
    }

    public Long getStartOfDayOneWeekFromNow() {
        Calendar cal = Calendar.getInstance(mTimeZone); // by default, returns right now
        cal.add(Calendar.DAY_OF_YEAR, 7); // increment by one day
        return getStartOfDay(cal.getTimeInMillis());
    }

    public Long getStartOfDaySixMonthsFromNow() {
        Calendar cal = Calendar.getInstance(mTimeZone); // by default, returns right now
        cal.add(Calendar.MONTH, 6); // increment by 6 months
        return getStartOfDay(cal.getTimeInMillis());
    }

    public Long getStartOfDayNDaysFromNow(int nDays) {
        Calendar cal = Calendar.getInstance(mTimeZone); // by default, returns right now
        cal.add(Calendar.DAY_OF_YEAR, nDays);
        return getStartOfDay(cal.getTimeInMillis());
    }

    public Long getFifteenMinutesInMillis() {
        return 15L * 60 * 1000;
    }

    public Long getStartOfDayAfterThis(Long currentDay) {
        Calendar cal = Calendar.getInstance(mTimeZone); // by default, returns right now
        cal.setTimeInMillis(currentDay);
        cal.add(Calendar.DAY_OF_YEAR, 1);
        return getStartOfDay(cal.getTimeInMillis());
    }

    public Long getStartOfDayNDaysAfterThis(Long currentDay, int nDays) {
        Calendar cal = Calendar.getInstance(mTimeZone); // by default, returns right now
        cal.setTimeInMillis(currentDay);
        cal.add(Calendar.DAY_OF_YEAR, nDays);
        return getStartOfDay(cal.getTimeInMillis());
    }

    public Long getStartOfDay(Long millis) {
        Calendar cal = Calendar.getInstance(mTimeZone);

        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTimeInMillis();
    }

    public static String getDateString(Calendar cal, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
        sdf.setTimeZone(cal.getTimeZone());
        return sdf.format(cal.getTime());
    }







    // Static methods

    // TODO: 2/18/2018 Not sure if I should be passing in a Long or a calculating it internally.
    public static void getTimezoneOffset(Context context, String stationId, Double lat, Double lon, Long timestamp){

        Long timestamp_in_sec = timestamp / 1000;
        URL url = TimezoneUtils.buildTimezoneUrl(lat, lon, timestamp_in_sec);

        TimezoneUtils tzu = TimezoneUtils.getInstance();
        tzu.getJSONObjectFromGoogleTimeZoneWebservice(url.toString(), new SomeCustomListener<JSONObject>()
        {
            @Override
            public void getResult(JSONObject result)  {
                Log.v(TAG, "getTimezone: " + result);
                try {

                    TimeZone tz = TimezoneUtils.parseTimezoneResponse(result);
                    // Parse and update db entry
                    ContentValues cv = new ContentValues(1);
                    cv.put(TidesContract.TidesEntry.COLUMN_STATION_TIMEZONE_ID, tz.getID());
                    String where = "(" + TidesContract.TidesEntry.COLUMN_STATION_ID + " = ? )";
                    String[] selectionArgs = {stationId};
                    context.getContentResolver().update(TidesContract.TidesEntry.STATION_INFO_CONTENT_URI, cv, where, selectionArgs);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });


    }

    public static String formatForSearchParams(Long millisIn) {
        SimpleDateFormat sdf = new SimpleDateFormat(PredictionServiceHelper.SEARCH_DATE_FORMAT, Locale.CANADA);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(millisIn));
    }

    public static String formatForRESTQuery(Long millisIn) {
        SimpleDateFormat sdf = new SimpleDateFormat(PredictionServiceHelper.SEARCH_DATE_FORMAT, Locale.CANADA);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(millisIn));
    }


    public static Long getStartOfDay(Calendar inputCal) {
        Calendar cal = Calendar.getInstance(inputCal.getTimeZone());
        cal.setTime(inputCal.getTime());

        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTimeInMillis();
    }

    public static Long getStartOfDayAfterThis(Calendar inputCal) {
        Calendar cal = Calendar.getInstance(inputCal.getTimeZone());
        cal.setTime(inputCal.getTime());

        cal.add(Calendar.DAY_OF_YEAR, 1);
        return getStartOfDay(cal);
    }





}
