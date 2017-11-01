package com.solvetec.derek.tides.Utils;


import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by dsolven on 10/31/2017.
 */

public class DateUtils {

    private static final String TAG = DateUtils.class.getSimpleName();
    public static final int WL15_IN_DAY = 4 * 24; // 4 per hour * 24 hours per day

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
            return parseTimezoneResponse(response);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private static final String TZ_STATUS = "status";
    private static final String TZ_ERRORMESSAGE = "errorMessage";
    private static final String TZ_DSTOFFSET = "dstOffset";
    private static final String TZ_RAWOFFSET = "rawOffset";
    private static final String TZ_TIMEZONEID = "timeZoneId";
    private static final String TZ_TIMEZONENAME = "timeZoneName";


    private static Long parseTimezoneResponse(String response)
            throws JSONException {

        JSONObject timezoneJson = new JSONObject(response);

        // Check the status
        if (timezoneJson.has(TZ_STATUS)) {
            String status = timezoneJson.getString(TZ_STATUS);
            if (status.equals("OK")) {
                Long offset = timezoneJson.getLong(TZ_DSTOFFSET) + timezoneJson.getLong(TZ_RAWOFFSET);
                return offset;
            } else {
                // TODO: 11/1/2017 How to handle status != OK?
            }
        } else {
            // TODO: 11/1/2017 How to handle no status returned? HTTP error?
        }

        return null;
    }

    public class Timezone {
        // TODO: 11/1/2017 Keep the timeZoneName around too, to show in the
    }


}
