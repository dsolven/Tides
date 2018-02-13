package com.solvetec.derek.tides.data;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by dsolven on 10/21/2017.
 */

public class TidesContract {

    // To allow access to my content provider
    public static final String AUTHORITY = "com.solvetec.derek.tides";

    // The base content URI = "content://" + <authority>
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    // Path for the entire wl15 directory
    public static final String PATH_WL15 = "wl15";

    // Path for the entire hilo directory
    public static final String PATH_HILO = "hilo";

    // Path for the entire station_info directory
    public static final String PATH_STATION_INFO = "station_info";

    // TidesEntry is the static inner class that defines the contents of the db table
    public static final class TidesEntry implements BaseColumns {

        // Common column names
        public static final String COLUMN_STATION_ID = "station_id";
        public static final String COLUMN_VALUE = "value";
        public static final String COLUMN_DATE = "date";

        // tides_wl15 table and column names
        public static final String TABLE_WL15 = "tides_wl15";

        // tides_hilo table and column names
        public static final String TABLE_HILO = "tides_hilo";

        // tides_station_info table and column names
        public static final String TABLE_STATION_INFO = "tides_station_info";
        public static final String COLUMN_STATION_LON = "station_longitude";
        public static final String COLUMN_STATION_LAT = "station_latitude";
        public static final String COLUMN_STATION_NAME = "station_name";
        public static final String COLUMN_STATION_TIMEZONE_ID = "station_timezone_id";



        // Content Uri = base content Uri + path
        public static final Uri WL15_CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_WL15).build();

        // Content Uri = base content Uri + path
        public static final Uri HILO_CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_HILO).build();


        // Content Uri = base content Uri + path
        public static final Uri STATION_INFO_CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_STATION_INFO).build();

    }
}
