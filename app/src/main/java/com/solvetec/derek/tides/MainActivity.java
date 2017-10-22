package com.solvetec.derek.tides;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
import com.Wsdl2Code.WebServices.PredictionsService.Station;
import com.Wsdl2Code.WebServices.PredictionsService.VectorMetadata;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.solvetec.derek.tides.data.TidesContract.TidesEntry;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// TODO: 10/21/2017 Fix compile support versions in gradle file.
public class MainActivity extends AppCompatActivity
        implements DayListAdapter.ListItemClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private DayListAdapter mDayListAdapter;
    private RecyclerView mDayListRecyclerView;
    private static final int NUM_DAYS_TO_DISPLAY  = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup the graphView
        GraphView graph = (GraphView) findViewById(R.id.graph_main);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(new DataPoint[]{
                new DataPoint(0, 1),
                new DataPoint(1, 5),
                new DataPoint(2, 3),
                new DataPoint(3, 2),
                new DataPoint(4, 6)
        });
        graph.addSeries(series);

        // Setup the list of days
        mDayListRecyclerView = (RecyclerView) findViewById(R.id.rv_list_of_days);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mDayListRecyclerView.setLayoutManager(layoutManager);
        mDayListRecyclerView.setHasFixedSize(true);
        mDayListAdapter = new DayListAdapter(NUM_DAYS_TO_DISPLAY, this);
        mDayListRecyclerView.setAdapter(mDayListAdapter);


        // TODO: 10/21/2017 Remove the button, once I have a database and contentProvider to handle the transactions.
        Button button = (Button) findViewById(R.id.button_pushToSend);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 07577;;White Rock
                // 07577,49.0211,-122.8058
                new testPredictionsAsync().execute();
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
        String toastMessage = "Item # " + clickedItemIndex + " clicked.";
        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
    }

    /**
     * Testing calls to PredictionsService, and verify each SOAP_ACTION actually works.
     */
    class testPredictionsAsync extends AsyncTask<String, Void, String> {

        @Override
        protected void onPostExecute(String s) {
            TextView textView = (TextView) findViewById(R.id.tv_test_textview);
            if(s != null) {
                textView.setText(s);
            } else {
                textView.setText(getString(R.string.prediction_error_network));
            }
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


            ResultSet searchResult = predictionsService.search(
                    "wl15",
                    49.0,
                    49.1,
                    -122.9,
                    -122.7,
                    0.0,
                    0.0,
                    "2017-12-19 00:00:00",
                    "2017-12-21 00:00:00",
                    1,
                    100,
                    true,
                    "",
                    "asc");
            ContentValues[] cvs = parseSearchResultSet(searchResult);
            return info;
        }
    }

    // TODO: 10/22/2017 This won't pull all of the necessary info (only the database info, not the "is this data valid" metadata). This might not be the best architecture for this task...

    /**
     * This helper method parses the data returned from a search query, and places the
     * relevant information into a CV ready to be added to the database.
     *
     * @param searchResult ResultSet returned from a predictionsService.search query.
     * @return ContentValues containing all database-necessary data.
     */
    public ContentValues[] parseSearchResultSet(ResultSet searchResult) {
        // TODO: 10/22/2017 Check for status. If not 200, throw error.
        int size = searchResult.size;
        ContentValues[] cvs = new ContentValues[size];

        for (int i = 0; i < size; i++) {
            Data d = searchResult.data.get(i);
            ContentValues cv = new ContentValues();
            cv.put(TidesEntry.COLUMN_VALUE, d.value);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CANADA);
            try {
                Date date = sdf.parse(d.boundaryDate.min); // min and max always contain the same info
                cv.put(TidesEntry.COLUMN_DATE, date.getTime());
            } catch (ParseException e) {
                e.printStackTrace();
            }

            Station station = extractStationFromData(d);
            cv.put(TidesEntry.COLUMN_STATION_ID, station.getStation_id());
            cv.put(TidesEntry.COLUMN_STATION_NAME, station.getStation_name());
            cvs[i] = cv;
        }
        return cvs;
    }

    public Station extractStationFromData(Data data) {
        Station station = new Station();
        int vectorSize = data.metadata.getPropertyCount();

        for (int i = 0; i < vectorSize; i++) {
            String name = data.metadata.get(i).name;
            String value = data.metadata.get(i).value;
            switch (name) {
                case "station_id":
                    station.setStation_id(value);
                    break;
                case "station_name":
                    station.setStation_name(value);
                    break;
                default:
                    throw new UnsupportedOperationException("Data.metadata should not contain this name: " + name);
            }
        }
        return station;
    }
}
