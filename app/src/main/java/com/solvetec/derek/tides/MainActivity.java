package com.solvetec.derek.tides;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.appcompat.app.AppCompatActivity;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.Wsdl2Code.WebServices.PredictionsService.BoundaryDate;
import com.Wsdl2Code.WebServices.PredictionsService.BoundaryDepth;
import com.Wsdl2Code.WebServices.PredictionsService.BoundarySpatial;
import com.Wsdl2Code.WebServices.PredictionsService.PredictionsService;
import com.Wsdl2Code.WebServices.PredictionsService.ResultSet;
import com.solvetec.derek.tides.dfo_REST.SearchParams;
import com.solvetec.derek.tides.dfo_REST.Station;
import com.Wsdl2Code.WebServices.PredictionsService.VectorMetadata;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.jjoe64.graphview.series.Series;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.solvetec.derek.tides.data.TidesContract;
import com.solvetec.derek.tides.dfo_REST.SomeCustomListener;
import com.solvetec.derek.tides.dfo_REST.predictionsREST;
import com.solvetec.derek.tides.sync.SyncUtils;
import com.solvetec.derek.tides.sync.TidesSyncIntentService;
import com.solvetec.derek.tides.utils.ConnectivityUtils;
import com.solvetec.derek.tides.utils.DateUtils;
import com.solvetec.derek.tides.utils.GraphViewUtils;
import com.solvetec.derek.tides.utils.PredictionServiceHelper;
import com.solvetec.derek.tides.data.TidesContract.TidesEntry;
import com.solvetec.derek.tides.utils.SunsetUtils;
import com.solvetec.derek.tides.utils.TimezoneUtils;

import org.json.JSONArray;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;


// TODO: 10/31/2017 Google API: Remember to restrict the API to this app only.


public class MainActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks,
        com.prolificinteractive.materialcalendarview.OnDateSelectedListener{

    private static final String TAG = MainActivity.class.getSimpleName();
    private Toast mProductionToast;
    private GraphView mGraphView;
    private ProgressBar mProgressBarGraph;
    private ImageView mSyncErrorImageView;
    private MaterialCalendarView mCalendarView;
    private OneDayDecorator mOneDayDecorator;
    private TextView mTextViewSelectedDay;
    private Cursor mGraphCursor;
    private static final int NUM_DAYS_TO_DISPLAY = 14;
    private static final int NUM_WL15_POINTS_TO_DISPLAY = 4 * 24 + 1;
    private static final long NUM_MILLIS_IN_15_MINUTES = 15 * 60 * 1000;
    private Calendar mSelectedDay;
    private String mSelectedStationId;
    private String mPrevSelectedStationId;
    private Station mSelectedStation;

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

        // Setup the network manager
        predictionsREST.getInstance(this);
        TimezoneUtils.getInstance(this);

        // Do any required "first run" initialization
        checkFirstRun();
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mSelectedStationId = sharedPreferences.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
        mPrevSelectedStationId = mSelectedStationId;

        // Setup the Toast objects
        mProductionToast = new Toast(this);

        // Setup the GraphView
        mGraphView = (GraphView) findViewById(R.id.graph_main);
        mTextViewSelectedDay = findViewById(R.id.tv_displayed_date);
        mProgressBarGraph = findViewById(R.id.pb_graph);
        mSyncErrorImageView = findViewById(R.id.iv_sync_error);

        // Set selected day to today
        mSelectedDay = Calendar.getInstance(TimeZone.getDefault());

        // Setup the calendarView
        mCalendarView = findViewById(R.id.cv_main_calendar);
        mCalendarView.setOnDateChangedListener(this);
        mCalendarView.setDateSelected(mSelectedDay, true);
        mOneDayDecorator = new OneDayDecorator();
        mCalendarView.addDecorator(mOneDayDecorator);

        // Schedule the background sync
        SyncUtils.scheduleBackgroundSync(this);

    }


    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: Started.");
        super.onResume();

        // Load sharedPreferences again, as they may have changed.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mSelectedStationId = sharedPreferences.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));

        if(!mPrevSelectedStationId.equals(mSelectedStationId)) {
            // Station has changed

            // Reset station information
            mPrevSelectedStationId = mSelectedStationId;
            mGraphView.removeAllSeries();

        }

        // Redraw the calendar decorators, in case the day changed.
        mOneDayDecorator.setDate(new Date()); // Set it to the current "today".
        mCalendarView.invalidateDecorators(); // Redraw all decorators.

        // Create loaders
        // Stations Loader
        Bundle stationBundle = new Bundle();
        String selection = "(" + TidesEntry.COLUMN_STATION_ID + "=?)";
        String[] selectionArgs = {mSelectedStationId};
        stationBundle.putString(SELECTION_KEY, selection);
        stationBundle.putStringArray(SELECTION_ARGS_KEY, selectionArgs);
        LoaderManager.getInstance(this).initLoader(ID_STATIONS_LOADER, stationBundle, this);

        // HILO Loader
//        Bundle hiloBundle = new Bundle();
//        Long today = DateUtils.getStartOfToday();
//        Long tomorrow = DateUtils.getStartOfDaySixMonthsFromNow();
//        hiloBundle.putStringArray(SELECTION_ARGS_KEY, new String[] {mSelectedStationId, today.toString(), tomorrow.toString()});
//        getSupportLoaderManager().restartLoader(ID_HILO_LOADER, hiloBundle, this);

        // TODO: 10/27/2017 Remember to clean up database on every start: If entry is for older than yesterday, remove it.


        // Sunrise and sunset data
        // TODO: 2/8/2018 Add sunrise and sunset data
        if(sharedPreferences.getBoolean(getString(R.string.pref_show_sunrise_key), false)) {
            GetSunriseSunset gss = new GetSunriseSunset();
            gss.execute();
        }

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
                LoaderManager.getInstance(this).destroyLoader(ID_STATIONS_LOADER);
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
                // Query database
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
                                TidesEntry.COLUMN_STATION_LAT,
                                TidesEntry.COLUMN_STATION_TIMEZONE_ID},
                        args.getString(SELECTION_KEY),
                        args.getStringArray(SELECTION_ARGS_KEY),
                        TidesEntry.COLUMN_STATION_ID + " ASC");
            default:
                throw new RuntimeException("Loader not implemented: " + id);
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        Cursor dataCursor = (Cursor) data;
        Log.d(TAG, "onLoadFinished: " + loader.getId() + ". Cursor size: " + dataCursor.getCount());

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        switch(loader.getId()) {
            case ID_WL15_LOADER:
                if (dataCursor.getCount() == NUM_WL15_POINTS_TO_DISPLAY) {
                    // Update progressbar UI and sync error UI
                    mProgressBarGraph.setVisibility(View.INVISIBLE);
                    mSyncErrorImageView.setVisibility(View.INVISIBLE);

                    // Load preferences
                    Boolean prefShowSunrise = sp.getBoolean(getString(R.string.pref_show_sunrise_key), false);

                    // Load data
                    mGraphCursor = dataCursor;
                    DataPoint[] newTidePoints = GraphViewUtils.getSeries(dataCursor);
                    // TODO: 11/1/2017 This is where I need to verify the timezone and current time. At this point, I've guarenteed that the timezone async has completed.
                    // TODO: 10/31/2017 How should I correctly get the cursorLoader to repopulate the cursor when I click on a different day? Just call loader.reset or something?

                    List<Series> seriesList = mGraphView.getSeries();

                    // Calculate current time
                    Long now = new Date().getTime();
                    Long xStart = (long) (newTidePoints[0].getX());
                    int currentTimeIndex = (int)((now - xStart) / NUM_MILLIS_IN_15_MINUTES);

                    if(seriesList.size() == 0) {
                        // First time
                        LineGraphSeries<DataPoint> newTideSeries = new LineGraphSeries<>(newTidePoints);

                        if(prefShowSunrise) {
                            // Add sunrise/sunset
                            // TODO: 11/22/2017 Have to do separate async task to get current sunrise/sunset. So, this needs to be in separate function that gets called by both.
                            DataPoint start = new DataPoint(newTidePoints[0].getX(), newTideSeries.getHighestValueY() * 2);
                            DataPoint end = new DataPoint(newTidePoints[newTidePoints.length - 1].getX(), newTideSeries.getHighestValueY() * 2);
                            DataPoint testSunrise = new DataPoint(newTidePoints[20].getX(), newTideSeries.getHighestValueY() * 2);
                            DataPoint testSunrise2 = new DataPoint(newTidePoints[21].getX(), newTideSeries.getHighestValueY() * 2);
                            DataPoint testSunset = new DataPoint(newTidePoints[70].getX(), newTideSeries.getHighestValueY() * 2);
                            DataPoint testSunset2 = new DataPoint(newTidePoints[71].getX(), newTideSeries.getHighestValueY() * 2);
//                        DataPoint[] sunriseDPs = {start, testSunrise};
                            DataPoint[] sunriseDPs = {testSunrise, testSunrise2};
//                        DataPoint[] sunsetDPs = {testSunset, end};
                            DataPoint[] sunsetDPs = {testSunset, testSunset2};
                            LineGraphSeries<DataPoint> sunriseSeries = new LineGraphSeries<>(sunriseDPs);
                            LineGraphSeries<DataPoint> sunsetSeries = new LineGraphSeries<>(sunsetDPs);
                            mGraphView.addSeries(sunriseSeries); //GRAPH_SUNRISE
                            mGraphView.addSeries(sunsetSeries); //GRAPH_SUNSET
                        } else {
                            // Add empty series, to keep seriesList array offset consistent
                            LineGraphSeries<DataPoint> sunriseSeries = new LineGraphSeries<>();
                            LineGraphSeries<DataPoint> sunsetSeries = new LineGraphSeries<>();
                            mGraphView.addSeries(sunriseSeries); //GRAPH_SUNRISE
                            mGraphView.addSeries(sunsetSeries); //GRAPH_SUNSET
                        }

                        // Tide data
                        mGraphView.addSeries(newTideSeries); //GRAPH_TIDE

                        // Add series for current time
                        if(currentTimeIndex >= 0 && currentTimeIndex < newTidePoints.length) {
                            // Current time is within this dataset
                            DataPoint closestDataPoint = newTidePoints[currentTimeIndex];
                            DataPoint[] currentTimeDataPoint = {new DataPoint(closestDataPoint.getX(), closestDataPoint.getY())};
                            PointsGraphSeries<DataPoint> currentTimeSeries = new PointsGraphSeries<>(currentTimeDataPoint);
                            mGraphView.addSeries(currentTimeSeries); //GRAPH_CURRENT_TIME
                        } else {
                            // Current time not within this dataset, but we still need to create it
                            DataPoint[] currentTimeDataPoint = {new DataPoint(now, 0)};
                            PointsGraphSeries<DataPoint> currentTimeSeries = new PointsGraphSeries<>(currentTimeDataPoint);
                            mGraphView.addSeries(currentTimeSeries); //GRAPH_CURRENT_TIME
                        }



                        GraphViewUtils.formatSeriesColorTide(mGraphView);
                        if(prefShowSunrise){
                            GraphViewUtils.formatSeriesColorSunrise(mGraphView);
                        }
                        GraphViewUtils.formatGraphBounds(mGraphView, TimeZone.getTimeZone(mSelectedStation.timezone_id));


                    } else {
                        // Series already created, just update
                        LineGraphSeries series = (LineGraphSeries) seriesList.get(GraphViewUtils.GRAPH_TIDE);
                        series.resetData(newTidePoints);
                        GraphViewUtils.formatGraphBounds(mGraphView, TimeZone.getTimeZone(mSelectedStation.timezone_id));

                        // Update current time
                        if(currentTimeIndex >= 0 && currentTimeIndex < newTidePoints.length) {
                            // Current time is within this dataset
                            DataPoint closestDataPoint = newTidePoints[currentTimeIndex];
                            DataPoint[] currentTimeDataPoint = {new DataPoint(closestDataPoint.getX(), closestDataPoint.getY())};
                            PointsGraphSeries series2 = (PointsGraphSeries) seriesList.get(GraphViewUtils.GRAPH_CURRENT_TIME);
                            series2.resetData(currentTimeDataPoint);
                        }

                        if(prefShowSunrise) {
                            // Update sunrise / sunset
                            // TODO: 11/22/2017 Make sunrise/sunset change per day
                        }
                    }

                    // Set non-graph texts
                    dataCursor.moveToFirst();
                    Long date = dataCursor.getLong(dataCursor.getColumnIndex(TidesContract.TidesEntry.COLUMN_DATE));
                    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(mSelectedStation.timezone_id));
                    cal.setTime(new Date(date));
                    String dateString = DateUtils.getDateString(cal, getString(R.string.format_date_weekday_date));
                    mTextViewSelectedDay.setText(dateString);

                } else {
                    // Need to get data from the server. If allowed, go get it.

                    boolean isConnected = ConnectivityUtils.isNetworkConnected(this); // Check for network connectivity. If not connected, don't bother with trying to sync.
                    boolean prefWifiOnly = sp.getBoolean(getString(R.string.pref_wifi_only_key), false); // Check is user has requested WifiOnly.
                    boolean isMetered = ConnectivityUtils.isActiveNetworkMetered(this);

                    if (!isConnected) {
                        // Set UI for unable to sync
                        mSyncErrorImageView.setVisibility(View.VISIBLE);
                        if(mProductionToast != null) {
                            mProductionToast.cancel();
                        }
                        mProductionToast = Toast.makeText(this, "Unable to sync data. Please connect to a data network.", Toast.LENGTH_SHORT);
                        mProductionToast.show();

                        Log.d(TAG, "syncTides: Network connection not found. Don't attempt sync.");
                        return;
                    } else if( prefWifiOnly && isMetered) {
                        // Set UI for unable to sync
                        mSyncErrorImageView.setVisibility(View.VISIBLE);
                        if(mProductionToast != null) {
                            mProductionToast.cancel();
                        }
                        mProductionToast = Toast.makeText(this, "Unable to sync data. Please connect to a Wifi network, or disable the \"Wifi Only\" option in Settings.", Toast.LENGTH_SHORT);
                        mProductionToast.show();

                        Log.d(TAG, "syncTides: Unmetered network found, and user requested wifi only. Don't attempt sync.");
                        return;
                    } else {
                        // Set UI for possible long data sync
                        mProgressBarGraph.setVisibility(View.VISIBLE);

                        //Toast.makeText(this, dataCursor.getCount() + " WL15 points, instead of " + NUM_WL15_POINTS_TO_DISPLAY, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "onLoadFinished: Cursor contains " + dataCursor.getCount() + " WL15 points, instead of " + NUM_WL15_POINTS_TO_DISPLAY);
                        Log.d(TAG, "onLoadFinished: Starting sync of single day.");

                        Intent syncSingleDayIntent = new Intent(this, TidesSyncIntentService.class);
                        syncSingleDayIntent.putExtra(TidesSyncIntentService.EXTRA_LONG_STARTING_DAY, mSelectedDay.getTimeInMillis());
                        syncSingleDayIntent.putExtra(TidesSyncIntentService.EXTRA_NUM_DAYS_TO_SYNC, 1);
                        startService(syncSingleDayIntent);
                    }
                }
                Log.d(TAG, "onLoadFinished: WL15 onLoadFinished complete.");
                break;
            case ID_HILO_LOADER:
                if (dataCursor.getCount() != 0) {
                    // TODO: 11/25/2017 I'm not using HILO info for anything right now, but maybe in the future.
                }
                Log.d(TAG, "onLoadFinished: HILO onLoadFinished complete.");
                break;
            case ID_STATIONS_LOADER:
                /*
                Strategy:
                - Init stations loader in onResume
                - Once station is loaded, get the timezoneId.
                    - If timezoneID exists, restart loader for wl15
                    - If timezoneID doesn't exist, kick off AsyncTask to go get it, and break. This loader will be kicked off again automatically once the db is updated.
                 */
                if (dataCursor.getCount() != 0) {
                    // Get info from preferred station
                    String preferredLocationString = sp.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
                    Map<String, Station> stationsMap = PredictionServiceHelper.parseStationCursor(dataCursor);
                    mSelectedStation = stationsMap.get(preferredLocationString);

                    if (mSelectedStation == null) {
                        Log.d(TAG, "ID_STATIONS_LOADER: Wrong station returned. Something went wrong setting up the loader, probably after changing the selected station.");
                        break;
                    }
                    // Setup title bar with the selected station name
                    setupTitleBar(mSelectedStation);

                    if (mSelectedStation.timezone_id != null) {
                        // Timezone_id is already populated, proceed normally.
                        Log.d(TAG, "Station onLoadFinished: Timezone_id is already populated, proceed normally.");

                        // WL15 Loader
                        TimeZone timeZone = TimeZone.getTimeZone(mSelectedStation.timezone_id);
                        DateUtils du = new DateUtils(timeZone);
                        mSelectedDay.setTimeZone(timeZone);

                        Bundle bundle = new Bundle();
                        Long selectedDay = DateUtils.getStartOfDay(mSelectedDay);
                        Long selectedDayPlusOne = DateUtils.getStartOfDayAfterThis(mSelectedDay);
                        String[] selectionArgs = {mSelectedStation.station_id, selectedDay.toString(), selectedDayPlusOne.toString()};
                        bundle.putStringArray(SELECTION_ARGS_KEY, selectionArgs);
                        getSupportLoaderManager().restartLoader(ID_WL15_LOADER, bundle, this);
                    } else {
                        // This is the first time this station has been selected. Go query the timezone.
                        Log.d(TAG, "Station onLoadFinished: This is the first time this station has been selected. Go query the timezone.");

                        DateUtils.getTimezoneOffset(this,
                                mSelectedStation.station_id,
                                mSelectedStation.latitude,
                                mSelectedStation.longitude,
                                DateUtils.getRightNow());

                    }
                } else {
                    Log.d(TAG, "onLoadFinished: ID_STATIONS_LOADER: Datacursor empty.");
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
     * MaterialCalendarView callback.
     * {@inheritDoc}
     */
    @Override
    public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
        TimeZone timeZone = TimeZone.getTimeZone(mSelectedStation.timezone_id);
        DateUtils du = new DateUtils(timeZone);
        mSelectedDay = Calendar.getInstance(timeZone);
        mSelectedDay.setTime(date.getDate());
        Log.d(TAG, "onSelectedDayChange: Selected day:" + DateUtils.getDateString(mSelectedDay, getString(R.string.format_date_date_and_time)));

        // TODO: 11/20/2017 Make a function
        // WL15 Loader
        Bundle bundle = new Bundle();
        Long selectedDay = DateUtils.getStartOfDay(mSelectedDay);
        Long selectedDayPlusOne = DateUtils.getStartOfDayAfterThis(mSelectedDay);
        String[] selectionArgs = {mSelectedStationId, selectedDay.toString(), selectedDayPlusOne.toString()};
        bundle.putStringArray(SELECTION_ARGS_KEY, selectionArgs);
        getSupportLoaderManager().restartLoader(ID_WL15_LOADER, bundle, this);
    }

    /**
     * Async task to query station metadata from the webservice.
     */
    class PredictionsStationInfoAsync extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            predictionsREST pr = predictionsREST.getInstance();
            pr.getJSONArrayFromDFOWebservice("stations", new SomeCustomListener<JSONArray>()
            {
                @Override
                public void getResult(JSONArray result)
                {
                    if (!result.isNull(0))
                    {
//                        Log.d(TAG, "REST message works: " + result.substring(0,500));

                        ContentValues[] cvs = predictionsREST.parseStationInfo(result);
                        Log.d(TAG, "PredictionsStationInfoAsync, doInBackground: " + cvs.length + " stations downloaded.");
                        getContentResolver().bulkInsert(TidesEntry.STATION_INFO_CONTENT_URI, cvs);
                    }
                }
            });
            return "test string";
        }

        @Override
        protected void onPostExecute(String s) {
            Toast.makeText(MainActivity.this, "Station download complete.", Toast.LENGTH_SHORT).show();
        }
    }


    class GetSunriseSunset extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... voids) {
            Log.d(TAG, "doInBackground: getSunriseSunset started.");
            Station exampleStation = PredictionServiceHelper.makeExampleStation();
            SunriseSunset result = SunsetUtils.getSunriseSunset(exampleStation.latitude, exampleStation.longitude, mSelectedDay, TimeZone.getTimeZone(mSelectedStation.timezone_id));

            // TODO: 11/26/2017 This now works. Need to integrate and display it.
            return null;
        }
    }

    private void checkFirstRun() {

        final String PREF_VERSION_CODE_KEY = "version_code";
        final int DOESNT_EXIST = -1;

        // Get current version code
        int currentVersionCode = BuildConfig.VERSION_CODE;

        // Get saved version code
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int savedVersionCode = prefs.getInt(PREF_VERSION_CODE_KEY, DOESNT_EXIST);
//        int savedVersionCode = DOESNT_EXIST;
        //TODO: test code to always do a fresh install

        // Check for first run or upgrade
        if (currentVersionCode == savedVersionCode) {
            // This is just a normal run
            Log.d(TAG, "checkFirstRun: Normal run.");
            return;
        } else if (savedVersionCode == DOESNT_EXIST) {
            // This is a new install (or the user cleared the shared preferences)
            Log.d(TAG, "checkFirstRun: Fresh install.");
            Toast.makeText(this, "Downloading initial data", Toast.LENGTH_LONG).show();

            // Download the station information
            new PredictionsStationInfoAsync().execute();

            // Download the next 5 days, just to have something
            Intent syncIntent = new Intent(this, TidesSyncIntentService.class);
            syncIntent.putExtra(TidesSyncIntentService.EXTRA_LONG_STARTING_DAY, new DateUtils(TimeZone.getTimeZone(getString(R.string.location_default_timezone))).getStartOfToday());
            syncIntent.putExtra(TidesSyncIntentService.EXTRA_NUM_DAYS_TO_SYNC, 5);
            startService(syncIntent);

            // Setup the shared pref for storing location
            // Note: This is not done in pref_general, because I don't want a UI for it. It's a hidden setting.
            prefs.edit().putString(getString(R.string.pref_location_key), getString(R.string.pref_location_default)).apply();

        } else if (currentVersionCode > savedVersionCode) {
            // TODO This is an upgrade
            Log.d(TAG, "checkFirstRun: Upgrade. Not implimented yet.");

        }

        // Update the shared preferences with the current version code
        prefs.edit().putInt(PREF_VERSION_CODE_KEY, currentVersionCode).apply();
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

    private void setupTitleBar(Station prefStation) {
        String title = getString(R.string.app_name) + ": " + prefStation.station_name;
        getSupportActionBar().setTitle(title);
    }


    /**
     * Decorate a day by making the text big and bold
     */
    public class OneDayDecorator implements DayViewDecorator {

        private CalendarDay date;

        public OneDayDecorator() {
            date = CalendarDay.today();
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return date != null && day.equals(date);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(new StyleSpan(Typeface.BOLD));
            view.addSpan(new RelativeSizeSpan(1.4f));
        }

        /**
         * We're changing the internals, so make sure to call {@linkplain MaterialCalendarView#invalidateDecorators()}
         */
        public void setDate(Date date) {
            this.date = CalendarDay.from(date);
        }
    }
}
