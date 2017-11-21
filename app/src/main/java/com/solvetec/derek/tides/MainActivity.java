package com.solvetec.derek.tides;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.Wsdl2Code.WebServices.PredictionsService.BoundaryDate;
import com.Wsdl2Code.WebServices.PredictionsService.BoundaryDepth;
import com.Wsdl2Code.WebServices.PredictionsService.BoundarySpatial;
import com.Wsdl2Code.WebServices.PredictionsService.PredictionsService;
import com.Wsdl2Code.WebServices.PredictionsService.ResultSet;
import com.Wsdl2Code.WebServices.PredictionsService.SearchParams;
import com.Wsdl2Code.WebServices.PredictionsService.Station;
import com.Wsdl2Code.WebServices.PredictionsService.VectorMetadata;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.Series;
import com.solvetec.derek.tides.data.TidesContract;
import com.solvetec.derek.tides.utils.DateUtils;
import com.solvetec.derek.tides.utils.GraphViewUtils;
import com.solvetec.derek.tides.utils.PredictionServiceHelper;
import com.solvetec.derek.tides.data.TidesContract.TidesEntry;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


// TODO: 10/31/2017 Google API: Remember to restrict the API to this app only.


public class MainActivity extends AppCompatActivity
        implements DayListAdapter.ListItemClickListener,
        LoaderManager.LoaderCallbacks {

    private static final String TAG = MainActivity.class.getSimpleName();
    private DayListAdapter mDayListAdapter;
    private RecyclerView mDayListRecyclerView;
    private GraphView mGraphView;
    private TextView mTextViewSelectedDay;
    private Cursor mGraphCursor;
    public Map<String, Station> mStationsMap;
    private static final int NUM_DAYS_TO_DISPLAY = 14;
    private static final int NUM_WL15_POINTS_TO_DISPLAY = 4 * 24 + 1;
    private Long mSelectedDay;
    private String mSelectedStationId;

    private Long mTimezoneOffset;

    private static final int ID_WL15_LOADER = 44;
    private static final int ID_HILO_LOADER = 45;
    private static final int ID_STATIONS_LOADER = 46;
    private static final String PROJECTION_KEY = "PROJECTION_KEY";
    private static final String SELECTION_KEY = "SELECTION_KEY";
    private static final String SELECTION_ARGS_KEY = "SELECTION_ARGS_KEY";
    private static final String SORT_BY_KEY = "SORT_BY_KEY";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: Started.");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Do any required "first run" initialization
        checkFirstRun();
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);


        // TODO: 11/13/2017 I need to separate the "build" parts of the UI from the "populate" parts of the UI. Leave the build in onCreate, move the populate to onResume.
        // Setup the GraphView
        mGraphView = (GraphView) findViewById(R.id.graph_main);
        mTextViewSelectedDay = findViewById(R.id.tv_displayed_date);

        // Setup the RecyclerView
        mDayListRecyclerView = (RecyclerView) findViewById(R.id.rv_list_of_days);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mDayListRecyclerView.setLayoutManager(layoutManager);
        mDayListRecyclerView.setHasFixedSize(true);

        // Set selected day to today
        mSelectedDay = DateUtils.getStartOfToday();

    }


    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: Started.");
        super.onResume();

        // Load sharedPreferences again, as they may have changed.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mSelectedStationId = sharedPreferences.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));


        // Create loaders
        // Stations Loader
        getSupportLoaderManager().initLoader(ID_STATIONS_LOADER, null, this);

        // HILO Loader
        Bundle hiloBundle = new Bundle();
        Long today = DateUtils.getStartOfToday();
        Long tomorrow = DateUtils.getStartOfDaySixMonthsFromNow();
        hiloBundle.putStringArray(SELECTION_ARGS_KEY, new String[] {mSelectedStationId, today.toString(), tomorrow.toString()});
        getSupportLoaderManager().restartLoader(ID_HILO_LOADER, hiloBundle, this);

        // TODO: 10/27/2017 Remember to clean up database on every start: If entry is for older than yesterday, remove it.

        // WL15 Loader
        Bundle bundle = new Bundle();
        Long selectedDayPlusOne = DateUtils.getStartOfDayAfterThis(mSelectedDay);
        String[] selectionArgs = {mSelectedStationId, mSelectedDay.toString(), selectedDayPlusOne.toString()};
        bundle.putStringArray(SELECTION_ARGS_KEY, selectionArgs);
        getSupportLoaderManager().restartLoader(ID_WL15_LOADER, bundle, this);

        // TODO: 10/31/2017 Immediately start a data sync here.

        // TODO: 11/15/2017 Connect up the timezone API, to actually get the selected station timezone, and use it in date calculations.
        Station exampleWhiteRockStation = PredictionServiceHelper.makeExampleStation();
        GetTimezoneOffset gto = new GetTimezoneOffset();
        gto.execute(exampleWhiteRockStation);


        // TODO: 10/21/2017 Remove the button, once I have a database and contentProvider to handle the transactions.
        Button buttonTestAll = (Button) findViewById(R.id.button_test_all_predictions);
        buttonTestAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new PredictionsTestAllAsync().execute();
            }
        });

        Button buttonTestWl15Search = (Button) findViewById(R.id.button_test_wl15_search_prediction);
        buttonTestWl15Search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setEnabled(false);
                SearchParams sp = PredictionServiceHelper.getWl15SearchParams(getApplicationContext());
                new PredictionsSearchAsync().execute(sp);
            }
        });

        Button buttonTestHiloSearch = (Button) findViewById(R.id.button_test_hilo_search_prediction);
        buttonTestHiloSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SearchParams sp = PredictionServiceHelper.getHILOSearchParams(getApplicationContext());
                new PredictionsSearchAsync().execute(sp);
            }
        });

        Button buttonTestMetadata = (Button) findViewById(R.id.button_test_metadata_prediction);
        buttonTestMetadata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new PredictionsStationInfoAsync().execute();
            }
        });


    }

    // DayListAdapter override methods
    /**
     * This is where we receive our callback from
     * {@link com.solvetec.derek.tides.DayListAdapter.ListItemClickListener}
     * @param clickedItemIndex
     */
    @Override
    public void onListItemClick(int clickedItemIndex, Long clickedItemDate ) {
        mSelectedDay = clickedItemDate;

        // TODO: 11/20/2017 Make a function
        // WL15 Loader
        Bundle bundle = new Bundle();
        Long selectedDayPlusOne = DateUtils.getStartOfDayAfterThis(mSelectedDay);
        String[] selectionArgs = {mSelectedStationId, mSelectedDay.toString(), selectedDayPlusOne.toString()};
        bundle.putStringArray(SELECTION_ARGS_KEY, selectionArgs);
        getSupportLoaderManager().restartLoader(ID_WL15_LOADER, bundle, this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_map:
                startActivity(new Intent (this, MapPickerActivity.class));
            default:
                return super.onOptionsItemSelected(item);

        }
    }


    /**
     * GraphView cursorLoader. Responsible for loading Cursor from database, and displaying it in
     * the current day's graph.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(TAG, "onCreateLoader: " + id);
        switch (id) {
            case ID_WL15_LOADER:
                Uri wl15QueryUri = TidesEntry.WL15_CONTENT_URI;
                return new CursorLoader(this,
                        wl15QueryUri,
                        new String[] {TidesEntry.COLUMN_DATE, TidesEntry.COLUMN_VALUE},
                        "(" + TidesEntry.COLUMN_STATION_ID + "=?) "
                                + "AND (" + TidesEntry.COLUMN_DATE + " BETWEEN ? AND ?)",
                        args.getStringArray(SELECTION_ARGS_KEY),
                        TidesEntry.COLUMN_DATE + " ASC");
            case ID_HILO_LOADER:
                Uri hiloQueryUri = TidesEntry.HILO_CONTENT_URI;
                return new CursorLoader(this,
                        hiloQueryUri,
                        new String[] {TidesEntry.COLUMN_DATE, TidesEntry.COLUMN_VALUE},
                        "(" + TidesEntry.COLUMN_STATION_ID + "=?) "
                                + "AND (" + TidesEntry.COLUMN_DATE + " BETWEEN ? AND ?)",
                        args.getStringArray(SELECTION_ARGS_KEY),
                        TidesEntry.COLUMN_DATE + " ASC");
            case ID_STATIONS_LOADER:
                Uri stationsQueryUri = TidesEntry.STATION_INFO_CONTENT_URI;
                return new CursorLoader(this,
                        stationsQueryUri,
                        new String[] {
                                TidesEntry.COLUMN_STATION_ID,
                                TidesEntry.COLUMN_STATION_NAME,
                                TidesEntry.COLUMN_STATION_LON,
                                TidesEntry.COLUMN_STATION_LAT },
                        null,
                        null,
                        TidesEntry.COLUMN_STATION_ID + " ASC");
            default:
                throw new RuntimeException("Loader not implemented: " + id);
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        Cursor dataCursor = (Cursor) data;
        Log.d(TAG, "onLoadFinished: " + loader.getId() + ". Cursor size: " + dataCursor.getCount());

        switch(loader.getId()) {
            case ID_WL15_LOADER:
                if (dataCursor.getCount() == NUM_WL15_POINTS_TO_DISPLAY) {
                    mGraphCursor = dataCursor;
                    DataPoint[] newDataPoints = GraphViewUtils.getSeries(dataCursor);
                    // TODO: 11/1/2017 This is where I need to verify the timezone and current time. At this point, I've guarenteed that the timezone async has completed.
                    // TODO: 10/31/2017 How should I correctly get the cursorLoader to repopulate the cursor when I click on a different day? Just call loader.reset or something?

                    List<Series> seriesList = mGraphView.getSeries();

                    if(seriesList.size() == 0) {
                        // First time
                        LineGraphSeries<DataPoint> newSeries = new LineGraphSeries<>(newDataPoints);
                        mGraphView.addSeries(newSeries);
                        GraphViewUtils.formatSeriesColor(mGraphView);
                        GraphViewUtils.formatGraphBounds(mGraphView);
                    } else {
                        // Series already created, just update
                        LineGraphSeries series = (LineGraphSeries) seriesList.get(0);
                        series.resetData(newDataPoints);
                        GraphViewUtils.formatGraphBounds(mGraphView);
                    }

                    // Set non-graph texts
                    dataCursor.moveToFirst();
                    Long date = dataCursor.getLong(dataCursor.getColumnIndex(TidesContract.TidesEntry.COLUMN_DATE));
                    String dateString = DateUtils.getDateString(date, getString(R.string.format_date_date_and_time));
                    mTextViewSelectedDay.setText(dateString);

                } else {
                    Toast.makeText(this, dataCursor.getCount() + " WL15 points, instead of " + NUM_WL15_POINTS_TO_DISPLAY, Toast.LENGTH_SHORT).show();
                }
                Log.d(TAG, "onLoadFinished: WL15 onLoadFinished complete.");
                break;
            case ID_HILO_LOADER:
                if (dataCursor.getCount() != 0) {
                    List<HiloDay> hiloDays = organizeHiloCursorIntoDays(dataCursor);
                    mDayListAdapter = new DayListAdapter(NUM_DAYS_TO_DISPLAY, this, hiloDays, this);
                    mDayListRecyclerView.setAdapter(mDayListAdapter);
                }
                Log.d(TAG, "onLoadFinished: HILO onLoadFinished complete.");
                break;
            case ID_STATIONS_LOADER:
                if (dataCursor.getCount() != 0) {
                    mStationsMap = parseStationCursor(dataCursor);
                    setupTitleBar(mStationsMap);
                }
                Log.d(TAG, "onLoadFinished: STATIONS onLoadFinished complete.");
                break;
            default:
                break;

            // TODO: 10/31/2017 else: throw up a toast or something?
        }

    }


    @Override
    public void onLoaderReset(Loader loader) {
        Log.d(TAG, "onLoaderReset: " + loader.getId());
        if (loader.getId() == ID_WL15_LOADER) {
            mGraphCursor = null;
        }
    }


    /**
     * Async task to query Predictions webservice.
     * Responsible for pulling data from webservice, parsing it, and placing it into the wl15
     * database table.
     */
    class PredictionsSearchAsync extends AsyncTask<SearchParams, Void, String[]> {
        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String[] doInBackground(SearchParams... params) {
            SearchParams sp = params[0];
            PredictionsService predictionsService = new PredictionsService();
            predictionsService.setTimeOut(10); // The default of 180ms was often timing out.

            ResultSet searchResult = predictionsService.search(sp);
            if (searchResult == null) {
                // Something went wrong, probably no cell reception.
                return new String[]{sp.dataName, "<Error, no data returned>."};
            }
            ContentValues[] cvs = PredictionServiceHelper.parseSearchResultSet(searchResult);
            String[] out = null;
            switch (sp.dataName) {
                case "wl15":
                    getContentResolver().bulkInsert(TidesEntry.WL15_CONTENT_URI, cvs);
                    // TODO: 10/26/2017 What to return? Should I do the database insert here?
                    out = new String[]{sp.dataName, cvs[0].get(TidesEntry.COLUMN_VALUE).toString()};
                    return out;
                case "hilo":
                    getContentResolver().bulkInsert(TidesEntry.HILO_CONTENT_URI, cvs);
                    // TODO: 10/26/2017 What to return? Should I do the database insert here?
                    out = new String[]{sp.dataName, cvs[0].get(TidesEntry.COLUMN_VALUE).toString()};
                    return out;
                default:
                    throw new UnsupportedOperationException("Unsupported searchParam dataName: " + sp.dataName);
            }

        }

        @Override
        protected void onProgressUpdate(Void... values) {

        }

        @Override
        protected void onPostExecute(String[] s) {
            Button buttonTestWl15Search = (Button) findViewById(R.id.button_test_wl15_search_prediction);
            buttonTestWl15Search.setEnabled(true);
            Toast.makeText(MainActivity.this, s[0] + " search completed. First entry is: " + s[1], Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Async task to query station metadata from the webservice.
     */
    class PredictionsStationInfoAsync extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            PredictionsService predictionsService = new PredictionsService();
            predictionsService.setTimeOut(10); // The default of 180ms was often timing out.
            VectorMetadata vectorMetadata = predictionsService.getMetadata();
            if (vectorMetadata == null) {
                // Something went wrong, webservice didn't return data. Probably have no cell reception.
                return "<Error. Stations not returned>.";
            }
            ContentValues[] cvs = PredictionServiceHelper.parseVectorMetadata(vectorMetadata);
            getContentResolver().bulkInsert(TidesEntry.STATION_INFO_CONTENT_URI, cvs);
            return cvs[0].get(TidesEntry.COLUMN_STATION_NAME).toString();
        }

        @Override
        protected void onPostExecute(String s) {
            Toast.makeText(MainActivity.this, "Station download complete. First station is: " + s, Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Testing calls to PredictionsService, and verify each SOAP_ACTION actually works.
     */
    class PredictionsTestAllAsync extends AsyncTask<String, Void, String> {

        @Override
        protected void onPostExecute(String s) {
            Toast.makeText(MainActivity.this, "TestAllPredictions returned: " + s, Toast.LENGTH_SHORT).show();
        }
        @Override
        protected String doInBackground(String... params) {
            PredictionsService predictionsService = new PredictionsService();
            predictionsService.setTimeOut(10); // The default of 180ms was often timing out.
            String info = predictionsService.getInfo();
            String version = predictionsService.getVersion();
            BoundaryDepth boundaryDepth = predictionsService.getBoundaryDepth();
            BoundarySpatial boundarySpatial = predictionsService.getBoundarySpatial();
            String name = predictionsService.getName();
            VectorMetadata vectorMetadata = predictionsService.getMetadata();
            BoundaryDate boundaryDate = predictionsService.getBoundaryDate();
            VectorMetadata vectorDataInfo = predictionsService.getDataInfo();
            VectorMetadata vectorMetaDataInfo = predictionsService.getMetadataInfo();

            SearchParams sp = PredictionServiceHelper.makeExampleWl15SearchParams();
            ResultSet searchResult = predictionsService.search(sp);

            return info;
        }
    }

    class GetTimezoneOffset extends AsyncTask<Station, Void, Long> {
        @Override
        protected void onPreExecute() {
            mTimezoneOffset = null; // Reset timezone offset
        }

        @Override
        protected Long doInBackground(Station... params) {
            Station s = params[0];
            Long currentTimestamp = DateUtils.getRightNow();
            Long timezoneOffset = DateUtils.getTimezoneOffset(s.latitude, s.longitude, currentTimestamp);
            return timezoneOffset;
        }

        @Override
        protected void onPostExecute(Long offset) {
            mTimezoneOffset = offset; // Set timezone offset to valid offset
        }
    }

    private void checkFirstRun() {
        // TODO: 11/5/2017 Currently not using this method, but might be useful in the future.

        final String PREF_VERSION_CODE_KEY = "version_code";
        final int DOESNT_EXIST = -1;

        // Get current version code
        int currentVersionCode = BuildConfig.VERSION_CODE;

        // Get saved version code
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int savedVersionCode = prefs.getInt(PREF_VERSION_CODE_KEY, DOESNT_EXIST);

        // Check for first run or upgrade
        if (currentVersionCode == savedVersionCode) {
            // This is just a normal run
            return;
        } else if (savedVersionCode == DOESNT_EXIST) {
            // TODO This is a new install (or the user cleared the shared preferences)

        } else if (currentVersionCode > savedVersionCode) {
            // TODO This is an upgrade

        }

        // Update the shared preferences with the current version code
        prefs.edit().putInt(PREF_VERSION_CODE_KEY, currentVersionCode).apply();
    }

    // TODO: 11/11/2017 Move this to a new file?
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

    // TODO: 11/5/2017 Move this to a new file?
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

    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    private void setupTitleBar(Map<String, Station> stationMap) {
        // Get info from preferred station
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String preferredLocationString = sp.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));

        Station prefStation = stationMap.get(preferredLocationString);
        String title = getString(R.string.app_name) + ": " + prefStation.station_name;

        getSupportActionBar().setTitle(title);
    }


}
