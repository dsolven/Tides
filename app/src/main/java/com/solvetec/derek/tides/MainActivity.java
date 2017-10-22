package com.solvetec.derek.tides;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.Wsdl2Code.WebServices.PredictionsService.BoundaryDate;
import com.Wsdl2Code.WebServices.PredictionsService.BoundaryDepth;
import com.Wsdl2Code.WebServices.PredictionsService.BoundarySpatial;
import com.Wsdl2Code.WebServices.PredictionsService.PredictionsService;
import com.Wsdl2Code.WebServices.PredictionsService.ResultSet;
import com.Wsdl2Code.WebServices.PredictionsService.VectorMetadata;

// TODO: 10/21/2017 Fix compile support versions in gradle file.
public class MainActivity extends AppCompatActivity
        implements DayListAdapter.ListItemClickListener {

    private DayListAdapter mDayListAdapter;
    private RecyclerView mDayListRecyclerView;
    private static final int NUM_DAYS_TO_DISPLAY  = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
            predictionsService.setTimeOut(1); // The default of 180ms was often timing out.
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
                    "2017-10-19 00:00:00",
                    "2017-10-21 00:00:00",
                    1,
                    100,
                    true,
                    "",
                    "asc");
            return info;
        }
    }
}
