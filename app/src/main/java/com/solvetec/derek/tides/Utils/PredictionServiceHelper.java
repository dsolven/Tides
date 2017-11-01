package com.solvetec.derek.tides.Utils;

import android.content.ContentValues;

import com.Wsdl2Code.WebServices.PredictionsService.Data;
import com.Wsdl2Code.WebServices.PredictionsService.Metadata;
import com.Wsdl2Code.WebServices.PredictionsService.ResultSet;
import com.Wsdl2Code.WebServices.PredictionsService.SearchParams;
import com.Wsdl2Code.WebServices.PredictionsService.Station;
import com.Wsdl2Code.WebServices.PredictionsService.VectorMetadata;
import com.solvetec.derek.tides.data.TidesContract;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Created by dsolven on 10/26/2017.
 */

public class PredictionServiceHelper {

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
                "2017-11-03 00:00:00",
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
                "2018-12-21 00:00:00",
                1,
                100,
                true,
                "",
                "asc");
    }

    public static Station makeExampleStation() {
        double latitude = 49.0211;
        double longitude = -122.8058;
        int station_id = 7577;
        String station_name = "White Rock";
        return new Station(station_id, station_name, latitude, longitude);
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

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CANADA);
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
                    try {
                        station.station_id = Integer.parseInt(value);
                    } catch (NumberFormatException nfe) {
                        nfe.printStackTrace();
                    }
                    break;
                case "station_name":
                    station.station_name = value;
                    break;
                default:
                    throw new UnsupportedOperationException("Data.metadata should not contain this name: " + name);
            }
        }
        return station;
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

            int id = -1;
            try {
                id = Integer.parseInt(ids[i]);
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }

            // Split into id,lat,lon
            String[] positions = Pattern.compile(",").split(posStrings[i]);
            Double lat = null;
            Double lon = null;
            int posId = -1;
            try {
                posId = Integer.parseInt(positions[0]);
                lat = Double.parseDouble(positions[1]);
                lon = Double.parseDouble(positions[2]);
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }

            // Split into id,name
            String[] names = Pattern.compile(";;").split(nameStrings[i]);
            int nameId = -1;
            try {
                nameId = Integer.parseInt(names[0]);
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }
            String name = names[1];

            if (id != posId || id != nameId || posId == -1) {
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
