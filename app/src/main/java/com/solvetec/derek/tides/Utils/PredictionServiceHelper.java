package com.solvetec.derek.tides.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.Wsdl2Code.WebServices.PredictionsService.Data;
import com.Wsdl2Code.WebServices.PredictionsService.Metadata;
import com.Wsdl2Code.WebServices.PredictionsService.ResultSet;
import com.Wsdl2Code.WebServices.PredictionsService.SearchParams;
import com.Wsdl2Code.WebServices.PredictionsService.Station;
import com.Wsdl2Code.WebServices.PredictionsService.VectorMetadata;
import com.solvetec.derek.tides.HiloDay;
import com.solvetec.derek.tides.R;
import com.solvetec.derek.tides.data.TidesContract;
import com.solvetec.derek.tides.data.TidesContract.TidesEntry;
import com.solvetec.derek.tides.sync.SyncUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by dsolven on 10/26/2017.
 */

public class PredictionServiceHelper {
    private static final String TAG = PredictionServiceHelper.class.getCanonicalName();
    public static final int WL15_IN_DAY = 4 * 24; // 4 per hour * 24 hours per day
    public static final String SEARCH_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static SearchParams makeExampleWl15SearchParams() {
        return new SearchParams(
                "wl15",
                49.0,
                49.1,
                -122.9,
                -122.7,
                0.0,
                0.0,
                "2017-11-01 00:00:00",
                "2017-11-02 00:00:00",
                1,
                1000,
                true,
                "",
                "asc");
    }

    public static SearchParams makeExampleHiloSearchParams() {
        return new SearchParams(
                "hilo",
                49.0,
                49.1,
                -122.9,
                -122.7,
                0.0,
                0.0,
                "2017-12-19 00:00:00",
                "2018-12-19 00:00:00",
                1,
                10000,
                true,
                "",
                "asc");
    }

    public static Station makeExampleStation() {
        double latitude = 49.0211;
        double longitude = -122.8058;
        String station_id = "07577";
        String station_name = "White Rock";
        return new Station(station_id, station_name, latitude, longitude);
    }


    public static SearchParams getWl15SearchParams(Context context, Long startingDay, int numDaysToSearch) {

        // Get the current station
        Station s = getCurrentStation(context);

        String dateMin = DateUtils.formatForSearchParams(DateUtils.getStartOfDay(startingDay));
        String dateMax = DateUtils.formatForSearchParams(DateUtils.getStartOfDayNDaysAfterThis(startingDay, numDaysToSearch));
        int sizeMax = WL15_IN_DAY * numDaysToSearch + 1;
        String metadataSelection = "station_id=" + s.station_id;

        return new SearchParams(
                "wl15",
                s.latitude - 0.1,
                s.latitude + 0.1,
                s.longitude - 0.1,
                s.longitude + 0.1,
                0.0,
                0.0,
                dateMin,
                dateMax,
                1,
                sizeMax,
                true,
                metadataSelection,
                "asc");
    }

    public static SearchParams getHILOSearchParams(Context context) {

        // Get the current station
        Station s = getCurrentStation(context);

        String dateMin = DateUtils.formatForSearchParams(DateUtils.getStartOfToday());
        String dateMax = DateUtils.formatForSearchParams(DateUtils.getStartOfDaySixMonthsFromNow());
        int sizeMax = 1000; // sizeMax is undetermined. Number of hi/lo per day is variable. 1000 should be larger than needed.
        // TODO: 11/2/2017 Right now, this is pulling 6 months worth of data. Change this and maxSize below.

        String metadataSelection = "station_id=" + s.station_id;

        return new SearchParams(
                "hilo",
                s.latitude - 0.1,
                s.latitude + 0.1,
                s.longitude - 0.1,
                s.longitude + 0.1,
                0.0,
                0.0,
                dateMin,
                dateMax,
                1,
                sizeMax,
                true,
                metadataSelection,
                "asc");
    }

    public static Station getCurrentStation(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String stationId = sp.getString(context.getString(R.string.pref_location_key), context.getString(R.string.pref_location_default));
        String[] selArgs = {stationId};
        String sel = TidesContract.TidesEntry.COLUMN_STATION_ID + "=?";

        Cursor resCursor = context.getContentResolver().query(
                TidesContract.TidesEntry.STATION_INFO_CONTENT_URI,
                null,
                sel,
                selArgs,
                null);

        if (resCursor != null && resCursor.getCount() != 0 && resCursor.moveToFirst()) {
            String stationName = resCursor.getString(resCursor.getColumnIndex(TidesContract.TidesEntry.COLUMN_STATION_NAME));
            Double stationLat = resCursor.getDouble(resCursor.getColumnIndex(TidesContract.TidesEntry.COLUMN_STATION_LAT));
            Double stationLon = resCursor.getDouble(resCursor.getColumnIndex(TidesContract.TidesEntry.COLUMN_STATION_LON));
            Log.d(TAG, "getCurrentStation: Current station is " + stationName);

            resCursor.close();
            return new Station(stationId, stationName, stationLat, stationLon);
        } else {
            Log.d(TAG, "getCurrentStation: Getting current station failed. Returning example station instead");
            return makeExampleStation();
        }


    }

    public static Map<String, Station> parseStationCursor(Cursor cursor) {
        Map<String, Station> stationMap = new LinkedHashMap<>();

        final int STATION_ID_INDEX = cursor.getColumnIndex(TidesEntry.COLUMN_STATION_ID);
        final int STATION_NAME_INDEX = cursor.getColumnIndex(TidesEntry.COLUMN_STATION_NAME);
        final int STATION_LAT_INDEX = cursor.getColumnIndex(TidesEntry.COLUMN_STATION_LAT);
        final int STATION_LON_INDEX = cursor.getColumnIndex(TidesEntry.COLUMN_STATION_LON);

        if(cursor.moveToFirst()) {
            do {
                Station station = new Station(
                        cursor.getString(STATION_ID_INDEX),
                        cursor.getString(STATION_NAME_INDEX),
                        cursor.getDouble(STATION_LAT_INDEX),
                        cursor.getDouble(STATION_LON_INDEX));
                stationMap.put(station.station_id, station);
            } while(cursor.moveToNext());
        }
        return stationMap;
    }

    // TODO: 10/22/2017 This won't pull all of the necessary info (only the database info, not the "is this data valid" metadata). This might not be the best architecture for this task...

    /**
     * This helper method parses the data returned from a search query, and places the
     * relevant information into a CV ready to be added to the WL15 database.
     *
     * @param searchResult ResultSet returned from a predictionsService.search query.
     * @return ContentValues containing all database-necessary data.
     */
    public static ContentValues[] parseSearchResultSet(ResultSet searchResult) {
        // TODO: 10/22/2017 Check for status. If not 200, throw error.
        int size = searchResult.size;
        ContentValues[] cvs = new ContentValues[size];

        for (int i = 0; i < size; i++) {
            Data d = searchResult.data.get(i);
            ContentValues cv = new ContentValues();
            cv.put(TidesContract.TidesEntry.COLUMN_VALUE, d.value);

            SimpleDateFormat sdf = new SimpleDateFormat(SEARCH_DATE_FORMAT, Locale.CANADA);
            try {
                Date date = sdf.parse(d.boundaryDate.min); // min and max always contain the same info
                cv.put(TidesContract.TidesEntry.COLUMN_DATE, date.getTime());
            } catch (ParseException e) {
                e.printStackTrace();
            }

            Station station = extractStationFromData(d);
            cv.put(TidesContract.TidesEntry.COLUMN_STATION_ID, station.station_id);
            cvs[i] = cv;
        }
        return cvs;
    }

    private static Station extractStationFromData(Data data) {
        Station station = new Station();
        int vectorSize = data.metadata.getPropertyCount();

        for (int i = 0; i < vectorSize; i++) {
            String name = data.metadata.get(i).name;
            String value = data.metadata.get(i).value;

            switch (name) {
                case "station_id":
                    station.station_id = value;
                case "station_name":
                    station.station_name = value;
                    break;
                default:
                    throw new UnsupportedOperationException("Data.metadata should not contain this name: " + name);
            }
        }
        return station;
    }

    // TODO: 11/5/2017 This is ripe for a unit test
    public static List<HiloDay> organizeHiloCursorIntoDays(Cursor cursor) {

        int valueColumnIndex = cursor.getColumnIndex(TidesContract.TidesEntry.COLUMN_VALUE);
        int dateColumnIndex = cursor.getColumnIndex(TidesContract.TidesEntry.COLUMN_DATE);

        List<HiloDay> hiloDays = new ArrayList<>();
        if (cursor.moveToFirst()) {
            Long currentDay = DateUtils.getStartOfDay(new Date(cursor.getLong(dateColumnIndex)).getTime());
            Long lastDay = currentDay;
            List<Double> todayList = new ArrayList<>();
            todayList.add(cursor.getDouble(valueColumnIndex));
            cursor.moveToNext();

            do {
                currentDay = DateUtils.getStartOfDay(new Date(cursor.getLong(dateColumnIndex)).getTime());
                if(currentDay.equals(lastDay)) {
                    // Same day, simply add to existing list
                    todayList.add(cursor.getDouble(valueColumnIndex));
                } else {
                    // New day, start new list
                    hiloDays.add(new HiloDay(currentDay, new ArrayList<>(todayList)));
                    todayList.clear();
                    todayList.add(cursor.getDouble(valueColumnIndex));
                }

                lastDay = currentDay;
            } while (cursor.moveToNext());

            // Handle last entry
            hiloDays.add(new HiloDay(currentDay, new ArrayList<>(todayList)));
        }

        return hiloDays;
    }

    /**
     * This helper method parses the data returned from a getMetadata webservice request, and places
     * the relevant information into a CV ready to be added to the STATION_INFO database.
     */
    public static ContentValues[] parseVectorMetadata(VectorMetadata vm) {
        int numMetadatas = vm.getPropertyCount();

        // Split the data into components
        String[] ids = null;
        String[] posStrings = null;
        String[] nameStrings = null;


        for (int i = 0; i < numMetadatas; i++) {
            Metadata md = vm.get(i);

            switch (md.name) {
                case "station_id_list":
                    // 05310,
                    ids = Pattern.compile(",").split(md.value);
                    break;
                case "station_id_position":
                    // [05310,69.7833,-82.1167]
                    // First, split into individual stations ( ][ or [ or ] )
                    String[] posStringsTemp = Pattern.compile("\\]\\[|\\[|\\]").split(md.value);
                    posStrings = Arrays.copyOfRange(posStringsTemp, 1, posStringsTemp.length); // Remove leading empty string
                    break;
                case "station_id_name_list":
                    // [05310;;Sévigny Point]
                    // First, split into individual stations ( ][ or [ or ] )
                    String[] nameStringsTemp = Pattern.compile("\\]\\[|\\[|\\]").split(md.value);
                    nameStrings = Arrays.copyOfRange(nameStringsTemp, 1, nameStringsTemp.length); // Remove leading empty string
                    break;
                default:
                    // Do nothing for all other metadata entries
                    break;
            }
        }

        // A couple assumptions:
        //   - The length of ids, posStrings, nameStrings are the same
        //   - They are in the same order

        if (ids == null || posStrings == null || nameStrings == null || ids.length != posStrings.length || ids.length != nameStrings.length) {
            throw new Error("Metadata must all exist and lengths should be the same.");
            // TODO: 10/26/2017 How to handle this type of error? It's more like an unexpected data error than anything else.
        }
        ContentValues[] cvs = new ContentValues[ids.length];

        for (int i = 0; i < ids.length; i++) {
            ContentValues cv = new ContentValues();

            // ID
            String id = ids[i];

            // POSITION: Split into id,lat,lon
            String[] positions = Pattern.compile(",").split(posStrings[i]);
            Double lat = null;
            Double lon = null;
            String posId = positions[0];
            try {
                lat = Double.parseDouble(positions[1]);
                lon = Double.parseDouble(positions[2]);
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }

            // NAME: Split into id,name
            String[] names = Pattern.compile(";;").split(nameStrings[i]);
            String nameId = names[0];
            String name = names[1];

            if (!id.equals(posId) || !id.equals(nameId)) {
                throw new Error("Metadata must be in the same order.");
            }

            cv.put(TidesContract.TidesEntry.COLUMN_STATION_ID, id);
            cv.put(TidesContract.TidesEntry.COLUMN_STATION_NAME, name);
            cv.put(TidesContract.TidesEntry.COLUMN_STATION_LAT, lat);
            cv.put(TidesContract.TidesEntry.COLUMN_STATION_LON, lon);
            cvs[i] = cv;
        }

        return cvs;
    }
}
