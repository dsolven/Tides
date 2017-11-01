package com.solvetec.derek.tides;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import com.Wsdl2Code.WebServices.PredictionsService.Data;
import com.Wsdl2Code.WebServices.PredictionsService.Metadata;
import com.Wsdl2Code.WebServices.PredictionsService.PredictionsService;
import com.Wsdl2Code.WebServices.PredictionsService.ResultSet;
import com.Wsdl2Code.WebServices.PredictionsService.SearchParams;
import com.Wsdl2Code.WebServices.PredictionsService.Station;
import com.Wsdl2Code.WebServices.PredictionsService.VectorMetadata;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.solvetec.derek.tides.Utils.DateUtils;
import com.solvetec.derek.tides.Utils.GraphViewUtils;
import com.solvetec.derek.tides.Utils.PredictionServiceHelper;
import com.solvetec.derek.tides.data.TidesContract.TidesEntry;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


// TODO: 10/31/2017 Google API: Remember to restrict the API to this app only.


public class MainActivity extends AppCompatActivity
        implements DayListAdapter.ListItemClickListener,
        LoaderManager.LoaderCallbacks {

    private static final String TAG = MainActivity.class.getSimpleName();
    private DayListAdapter mDayListAdapter;
    private RecyclerView mDayListRecyclerView;
    private GraphView mGraphView;
    private Cursor mGraphCursor;
    private static final int NUM_DAYS_TO_DISPLAY = 14;

    private Long mTimezoneOffset;

    private static final int ID_WL15_LOADER = 44;
    private static final String PROJECTION_KEY = "PROJECTION_KEY";
    private static final String SELECTION_KEY = "SELECTION_KEY";
    private static final String SELECTION_ARGS_KEY = "SELECTION_ARGS_KEY";
    private static final String SORT_BY_KEY = "SORT_BY_KEY";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup the graphView
        mGraphView = (GraphView) findViewById(R.id.graph_main);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(new DataPoint[]{
                new DataPoint(0, 1),
                new DataPoint(1, 5),
                new DataPoint(2, 3),
                new DataPoint(3, 2),
                new DataPoint(4, 6)
        });
        mGraphView.addSeries(series);

        // Setup the list of days
        mDayListRecyclerView = (RecyclerView) findViewById(R.id.rv_list_of_days);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mDayListRecyclerView.setLayoutManager(layoutManager);
        mDayListRecyclerView.setHasFixedSize(true);
        mDayListAdapter = new DayListAdapter(NUM_DAYS_TO_DISPLAY, this);
        mDayListRecyclerView.setAdapter(mDayListAdapter);

        // TODO: 10/27/2017 Remember to clean up database on every start: If entry is for older than yesterday, remove it.

        Station exampleWhiteRockStation = PredictionServiceHelper.makeExampleStation();
        GetTimezoneOffset gto = new GetTimezoneOffset();
        gto.execute(exampleWhiteRockStation);

        Bundle bundle = new Bundle();
        String[] projection = {TidesEntry.COLUMN_DATE, TidesEntry.COLUMN_VALUE};
        String selection = "(" + TidesEntry.COLUMN_STATION_ID + "=?) "
                + "AND (" + TidesEntry.COLUMN_DATE + " BETWEEN ? AND ?)";
        int stationId = 7577; // TODO: 10/31/2017 Grab this from the preferences instead
        Long today = DateUtils.getStartOfToday();
        Long tomorrow = DateUtils.getStartOfTomorrow();
        String[] selectionArgs = {Integer.toString(stationId), today.toString(), tomorrow.toString()};
        String sortBy = TidesEntry.COLUMN_DATE + " ASC";
        bundle.putStringArray(PROJECTION_KEY, projection);
        bundle.putString(SELECTION_KEY, selection);
        bundle.putStringArray(SELECTION_ARGS_KEY, selectionArgs);
        bundle.putString(SORT_BY_KEY, sortBy);
        getSupportLoaderManager().initLoader(ID_WL15_LOADER, bundle, this);

        // TODO: 10/31/2017 Immediately start a data sync here.


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
                SearchParams sp = PredictionServiceHelper.makeExampleWl15SearchParams();
                new PredictionsSearchAsync().execute(sp);
            }
        });

        Button buttonTestHiloSearch = (Button) findViewById(R.id.button_test_hilo_search_prediction);
        buttonTestHiloSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SearchParams sp = PredictionServiceHelper.makeExampleHiloSearchParams();
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
    public void onListItemClick(int clickedItemIndex) {
        // TODO: 10/21/2017 Make this do something... Change the main graphview? Open a new activity showing details?
        // TODO: 10/25/2017 Why isn't this showing a nice click animation?
        String toastMessage = "Item # " + clickedItemIndex + " clicked.";
        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
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
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case ID_WL15_LOADER:
                Uri wl15QueryUri = TidesEntry.WL15_CONTENT_URI;

                return new CursorLoader(this,
                        wl15QueryUri,
                        args.getStringArray(PROJECTION_KEY),
                        args.getString(SELECTION_KEY),
                        args.getStringArray(SELECTION_ARGS_KEY),
                        args.getString(SORT_BY_KEY));
            default:
                throw new RuntimeException("Loader not implemented: " + id);
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        if (loader.getId() == ID_WL15_LOADER) {
            mGraphCursor = (Cursor) data;
            if (mGraphCursor.getCount() != 0) {
                LineGraphSeries<DataPoint> series = GraphViewUtils.getSeries(mGraphCursor);
                // TODO: 11/1/2017 This is where I need to verify the timezone and current time. At this point, I've guarenteed that the timezone async has completed.
                // TODO: 10/31/2017 How should I correctly get the cursorLoader to repopulate the cursor when I click on a different day? Just call loader.reset or something?
                mGraphView.removeAllSeries();
                mGraphView.addSeries(series);

            }
            // TODO: 10/31/2017 else: throw up a toast or something?
        }
        
    }


    @Override
    public void onLoaderReset(Loader loader) {
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
            ContentValues[] cvs = null;
            String[] out = null;
            switch (sp.dataName) {
                case "wl15":
                    cvs = PredictionServiceHelper.parseSearchResultSet(searchResult);

                    getContentResolver().bulkInsert(TidesEntry.WL15_CONTENT_URI, cvs);
                    // TODO: 10/26/2017 What to return? Should I do the database insert here?
                    out = new String[]{sp.dataName, cvs[0].get(TidesEntry.COLUMN_VALUE).toString()};
                    return out;
                case "hilo":
                    cvs = PredictionServiceHelper.parseSearchResultSet(searchResult);
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
            ContentValues[] cvs = PredictionServiceHelper.parseVectorMetadata(vectorMetadata);
            // TODO: 10/26/2017 Parse vectorMetadata, to populate the station information database.
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


}
