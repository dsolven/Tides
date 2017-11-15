package com.solvetec.derek.tides.utils;


import android.util.Log;

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
        cal.add(Calendar.DAY_OF_YEAR, 1); // increment by one day
        return getStartOfDay(cal.getTime());
    }

    public static Long getStartOfDayOneWeekFromNow() {
        Calendar cal = Calendar.getInstance(); // by default, returns right now
        cal.add(Calendar.DAY_OF_YEAR, 7); // increment by one day
        return getStartOfDay(cal.getTime());
    }

    public static Long getStartOfDaySixMonthsFromNow() {
        Calendar cal = Calendar.getInstance(); // by default, returns right now
        cal.add(Calendar.MONTH, 6); // increment by 6 months
        return getStartOfDay(cal.getTime());
    }

    public static Long getFifteenMinutesInMillis() {
        return 15L * 60 * 1000;
    }

    public static Long getStartOfDay(Date time) {
        // use UTC time zone
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        // TODO: 11/6/2017 Hardcoded timezone of phone right now. Need to change this to get the stored timezone, as queried from the google API.
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

    public static String getDateString(Long millis, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date(millis));
    }





}
