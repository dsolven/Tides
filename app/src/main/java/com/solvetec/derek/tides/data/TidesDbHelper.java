package com.solvetec.derek.tides.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.solvetec.derek.tides.data.TidesContract.TidesEntry;

/**
 * Created by dsolven on 10/21/2017.
 */

public class TidesDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "tidesDb.db";

    private static final int DATABASE_VERSION = 1;

    public TidesDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    private static final String CREATE_TABLE_WL15 = "CREATE TABLE " + TidesEntry.TABLE_WL15 + " (" +
            TidesEntry._ID + " INTEGER PRIMARY_KEY, " +
            TidesEntry.COLUMN_DATE + " INTEGER NOT NULL, " +
            TidesEntry.COLUMN_VALUE + " REAL NOT NULL, " +
            TidesEntry.COLUMN_STATION_ID + " INTEGER NOT NULL, " +
            " UNIQUE (" + TidesEntry.COLUMN_DATE + ") ON CONFLICT REPLACE);";

    private static final String CREATE_TABLE_STATION_INFO = "CREATE TABLE " + TidesEntry.TABLE_STATION_INFO + " (" +
            TidesEntry._ID + " INTEGER PRIMARY_KEY, " +
            TidesEntry.COLUMN_STATION_ID + " INTEGER NOT NULL, " +
            TidesEntry.COLUMN_STATION_NAME + " TEXT NOT NULL, " +
            TidesEntry.COLUMN_STATION_LON + " REAL NOT NULL, " +
            TidesEntry.COLUMN_STATION_LAT + " REAL NOT NULL, " +
            " UNIQUE (" + TidesEntry.COLUMN_STATION_ID + ") ON CONFLICT REPLACE);";
    // TODO: 10/25/2017 Should never have a conflict. If so, I guess replace?
    // TODO: 10/25/2017 Should station_id be the primary key? There should only be a single entry, so maybe.
    // TODO: 10/25/2017 I have made a relational database now. Need to figure out how to actually link the tables... Probably done in ContentProvider?


    /**
     * Called when the database is created for the first time.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_WL15);
        db.execSQL(CREATE_TABLE_STATION_INFO);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TABLE_WL15 and TABLE_STATION_INFO only caches data, so simply wipe on upgrade
        db.execSQL("DROP TABLE IF EXISTS " + TidesEntry.TABLE_WL15);
        db.execSQL("DROP TABLE IF EXISTS " + TidesEntry.TABLE_STATION_INFO);
        onCreate(db);
    }
}
