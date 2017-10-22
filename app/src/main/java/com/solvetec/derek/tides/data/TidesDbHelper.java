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

    private static final int VERSION = 1;

    public TidesDbHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }


    /**
     * Called when the database is created for the first time.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
//        final String CREATE_TABLE = "CREATE TABLE " +
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
