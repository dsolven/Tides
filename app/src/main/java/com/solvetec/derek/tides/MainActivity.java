package com.solvetec.derek.tides;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.Wsdl2Code.WebServices.PredictionsService.BoundaryDate;
import com.Wsdl2Code.WebServices.PredictionsService.BoundaryDepth;
import com.Wsdl2Code.WebServices.PredictionsService.BoundarySpatial;
import com.Wsdl2Code.WebServices.PredictionsService.PredictionsService;
import com.Wsdl2Code.WebServices.PredictionsService.ResultSet;
import com.Wsdl2Code.WebServices.PredictionsService.VectorMetadata;


public class MainActivity extends AppCompatActivity {

    String URL = "https://ws-shc.qc.dfo-mpo.gc.ca/predictions";
    String NAMESPACE = "https://ws-shc.qc.dfo-mpo.gc.ca/predictions";
    String METHOD_NAME = "search";
    String SOAP_ACTION = NAMESPACE + METHOD_NAME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = (Button) findViewById(R.id.button_pushToSend);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new testPredictionsAsync().execute();

//                07577;;White Rock
//                07577,49.0211,-122.8058

            }
        });
    }

    class testPredictionsAsync extends AsyncTask<String, Void, String> {

        @Override
        protected void onPostExecute(String s) {
            TextView textView = (TextView) findViewById(R.id.tv_textview);
            textView.setText(s);
        }
        @Override
        protected String doInBackground(String... params) {
            PredictionsService predictionsService = new PredictionsService(null, URL);
            predictionsService.setTimeOut(10); //seconds
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
