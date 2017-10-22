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

    // TidesEntry is the static inner class that defines the contents of the db table
    public static final class TidesEntry implements BaseColumns {

        // Tides_wl15 table and column names
        public static final String TABLE_NAME = "tides_wl15";

        public static final String COLUMN_VALUE = "value";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_STATION_ID = "station_id";
        public static final String COLUMN_STATION_NAME = "station_name";

        // Tides_hilo table and column names
        // TODO: 10/22/2017 SHould be a different Entry?


    }
}
