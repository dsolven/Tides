package com.solvetec.derek.tides;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by Derek on 11/25/2017.
 */

public class SunriseSunset {
    public Long sunrise;                      //2015-05-21T05:05:35+00:00
    public Long sunset;                       //2015-05-21T19:22:59+00:00
    public Long solar_noon;                   //2015-05-21T12:14:17+00:00
    public Long day_length;                   //1444
    public Long civil_twilight_begin;         //2015-05-21T04:36:17+00:00
    public Long civil_twilight_end;           //2015-05-21T19:52:17+00:00
    public Long nautical_twilight_begin;      //2015-05-21T04:00:13+00:00
    public Long nautical_twilight_end;        //2015-05-21T20:28:21+00:00
    public Long astronomical_twilight_begin;  //2015-05-21T03:20:49+00:00
    public Long astronomical_twilight_end;    //2015-05-21T21:07:45+00:00

    public SunriseSunset(JSONObject jsonObject) {

        String format = "yyyy-MM-dd'T'HH:mm:ssZ";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);

        try {
            JSONObject resultsObject = jsonObject.getJSONObject("results");
            String sunriseString = resultsObject.getString("sunrise");
            String sunsetString = resultsObject.getString("sunset");
            String solar_noonString = resultsObject.getString("solar_noon");
            String day_lengthString = resultsObject.getString("day_length");
            String civil_twilight_beginString = resultsObject.getString("civil_twilight_begin");
            String civil_twilight_endString = resultsObject.getString("civil_twilight_end");
            String nautical_twilight_beginString = resultsObject.getString("nautical_twilight_begin");
            String nautical_twilight_endString = resultsObject.getString("nautical_twilight_end");
            String astronomical_twilight_beginString = resultsObject.getString("astronomical_twilight_begin");
            String astronomical_twilight_endString = resultsObject.getString("astronomical_twilight_end");

            this.sunrise = sdf.parse(sunriseString).getTime();
            this.sunset = sdf.parse(sunsetString).getTime();
            this.solar_noon = sdf.parse(solar_noonString).getTime();
            this.day_length = Long.parseLong(day_lengthString);
            this.civil_twilight_begin = sdf.parse(civil_twilight_beginString).getTime();
            this.civil_twilight_end = sdf.parse(civil_twilight_endString).getTime();
            this.nautical_twilight_begin = sdf.parse(nautical_twilight_beginString).getTime();
            this.nautical_twilight_end = sdf.parse(nautical_twilight_endString).getTime();
            this.astronomical_twilight_begin = sdf.parse(astronomical_twilight_beginString).getTime();
            this.astronomical_twilight_end = sdf.parse(astronomical_twilight_endString).getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
