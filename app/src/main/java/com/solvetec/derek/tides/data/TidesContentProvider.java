package com.solvetec.derek.tides.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by dsolven on 10/21/2017.
 */

public class TidesContentProvider extends ContentProvider {

    // Entire table directory
    public static final int WL15 = 100;
    public static final int HILO = 200;
    public static final int STATION_INFO = 300;

    // Individual entries from table
    public static final int WL15_WITH_DATE = 101;
    public static final int STATION_INFO_WITH_STATION_ID = 301;


    private static final UriMatcher sUriMatcher = buildUriMatcher();

    public static UriMatcher buildUriMatcher() {
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        uriMatcher.addURI(TidesContract.AUTHORITY, TidesContract.PATH_WL15, WL15);
        uriMatcher.addURI(TidesContract.AUTHORITY, TidesContract.PATH_WL15 + "/#", WL15_WITH_DATE);
        uriMatcher.addURI(TidesContract.AUTHORITY, TidesContract.PATH_HILO, HILO);
        uriMatcher.addURI(TidesContract.AUTHORITY, TidesContract.PATH_STATION_INFO, STATION_INFO);
        uriMatcher.addURI(TidesContract.AUTHORITY, TidesContract.PATH_STATION_INFO + "/#", STATION_INFO_WITH_STATION_ID);
        // TODO: 10/23/2017 What other things do we want to return from the db? Range of dates?

        return uriMatcher;
    }


    private TidesDbHelper mTidesDbHelper;

    @Override
    public boolean onCreate() {
        Context context = getContext();
        mTidesDbHelper = new TidesDbHelper(context);
        return true;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        final SQLiteDatabase db = mTidesDbHelper.getWritableDatabase();

        int match = sUriMatcher.match(uri);

        long id;

        switch (match) {
            case WL15:
                db.beginTransaction();
                for (ContentValues value : values) {
                    id = db.insert(TidesContract.TidesEntry.TABLE_WL15, null, value);
                }
                db.setTransactionSuccessful();
                db.endTransaction();
                // TODO: 11/6/2017 Not checking for success or failure, like I did in insert(). Problem?
                break;
            case HILO:
                db.beginTransaction();
                for (ContentValues value : values) {
                    id = db.insert(TidesContract.TidesEntry.TABLE_HILO, null, value);
                }
                db.setTransactionSuccessful();
                db.endTransaction();
                break;
            case STATION_INFO:
                db.beginTransaction();
                for (ContentValues value : values) {
                    id = db.insert(TidesContract.TidesEntry.TABLE_STATION_INFO, null, value);
                }
                db.setTransactionSuccessful();
                db.endTransaction();
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Notify the resolver that the uri has changed
        getContext().getContentResolver().notifyChange(uri, null);

        return values.length;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        final SQLiteDatabase db = mTidesDbHelper.getWritableDatabase();

        int match = sUriMatcher.match(uri);
        Uri returnUri; // Points to the newly inserted data

        long id;

        switch (match) {
            case WL15:
                id = db.insert(TidesContract.TidesEntry.TABLE_WL15, null, values);
                if (id > 0) {
                    returnUri = ContentUris.withAppendedId(TidesContract.TidesEntry.WL15_CONTENT_URI, id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            case HILO:
                id = db.insert(TidesContract.TidesEntry.TABLE_HILO, null, values);
                if (id > 0) {
                    returnUri = ContentUris.withAppendedId(TidesContract.TidesEntry.HILO_CONTENT_URI, id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            case STATION_INFO:
                id = db.insert(TidesContract.TidesEntry.TABLE_STATION_INFO, null, values);
                if (id > 0) {
                    returnUri = ContentUris.withAppendedId(TidesContract.TidesEntry.STATION_INFO_CONTENT_URI, id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Notify the resolver that the uri has changed
        getContext().getContentResolver().notifyChange(uri, null);

        return returnUri;
    }


    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        final SQLiteDatabase db = mTidesDbHelper.getReadableDatabase();

        int match = sUriMatcher.match(uri);
        Cursor retCursor;

        switch (match) {
            case WL15:
                retCursor = db.query(TidesContract.TidesEntry.TABLE_WL15,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            case WL15_WITH_DATE:
                // If the Uri has the signature: content://<AUTHORITY>/wl15/19020411
                //   then we want to return just the entry with that UTC standardized date.
                String normalizedUtcDateString = uri.getLastPathSegment();
                String[] selectionArguments = new String[]{normalizedUtcDateString};

                retCursor = db.query(
                        TidesContract.TidesEntry.TABLE_WL15,
                        projection,
                        TidesContract.TidesEntry.COLUMN_DATE + " = ? ",
                        selectionArguments,
                        null,
                        null,
                        sortOrder);
                break;
            case HILO:
                retCursor = db.query(TidesContract.TidesEntry.TABLE_HILO,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            case STATION_INFO:
                retCursor = db.query(TidesContract.TidesEntry.TABLE_STATION_INFO,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        retCursor.setNotificationUri(getContext().getContentResolver(), uri);

        return retCursor;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        final SQLiteDatabase db = mTidesDbHelper.getWritableDatabase();

        int match = sUriMatcher.match(uri);
        int rowsAffected;

        switch (match) {
            case WL15:
                throw new UnsupportedOperationException("update command for WL15 not implemented.");
            case HILO:
                throw new UnsupportedOperationException("update command for HILO not implemented.");
            case STATION_INFO:
                rowsAffected = db.update(TidesContract.TidesEntry.TABLE_STATION_INFO, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Notify the resolver that the uri has changed
        getContext().getContentResolver().notifyChange(uri, null);

        return rowsAffected;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        final SQLiteDatabase db = mTidesDbHelper.getWritableDatabase();

        int match = sUriMatcher.match(uri);
        int tasksDeleted;

        switch (match) {
            case WL15:
                // Delete the whole table. Maybe useful as a nuke option?
                tasksDeleted = db.delete(TidesContract.TidesEntry.TABLE_WL15, "*", null);
                // TODO: 10/26/2017 Ensure this actually works, with the "*" whereClause.
                break;
            case HILO:
                // Delete the whole table. Maybe useful as a nuke option?
                tasksDeleted = db.delete(TidesContract.TidesEntry.TABLE_HILO, "*", null);
                // TODO: 10/26/2017 Ensure this actually works, with the "*" whereClause.
                break;
            case STATION_INFO:
                // Delete the whole table. Maybe useful as a nuke option?
                tasksDeleted = db.delete(TidesContract.TidesEntry.TABLE_STATION_INFO, "*", null);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // If a delete happened, notify the resolver of a change
        if (tasksDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return tasksDeleted;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        throw new UnsupportedOperationException("getType command not implemented.");
    }
}
