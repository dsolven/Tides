package com.solvetec.derek.tides;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.ksoap2.serialization.SoapObject;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SoapObject request = new SoapObject();
    }
}
