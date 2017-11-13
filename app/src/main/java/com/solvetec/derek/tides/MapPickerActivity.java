package com.solvetec.derek.tides;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;

import com.Wsdl2Code.WebServices.PredictionsService.Station;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.solvetec.derek.tides.data.TidesContract.TidesEntry;

import java.util.List;

/**
 * Created by Derek on 11/11/2017.
 */

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback, LoaderManager.LoaderCallbacks{

    private GoogleMap mMap;
    private List<Station> mStationsList;
    private String mPrefStationId;
    private Station mPrefStation;

    private static final String PROJECTION_KEY = "PROJECTION_KEY";
    private static final int ID_STATIONS_LOADER = 50;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        // Get settings
        Context appContext = getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        mPrefStationId = prefs.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));

        // Load stations from db
        Bundle stationsBundle = new Bundle();
        stationsBundle.putStringArray(PROJECTION_KEY, new String[] {
                TidesEntry.COLUMN_STATION_ID,
                TidesEntry.COLUMN_STATION_NAME,
                TidesEntry.COLUMN_STATION_LON,
                TidesEntry.COLUMN_STATION_LAT });
        getSupportLoaderManager().initLoader(ID_STATIONS_LOADER, stationsBundle, this);

//        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
//                .findFragmentById(R.id.map_picker_fragment);
//        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

//        // Add a marker in Sydney, Australia, and move the camera.
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        // Load all markers
        for(int i = 0; i < mStationsList.size(); i++) {
            Station station = mStationsList.get(i);
            LatLng location = new LatLng(station.latitude, station.longitude);
            mMap.addMarker(new MarkerOptions().position(location).title(station.station_name));
            if(station.station_id.equals(mPrefStationId)) {
                mPrefStation = new Station(station);
            }
        }

        // Pick starting camera
        float zoomLevel = 9;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(mPrefStation.latitude, mPrefStation.longitude),
                zoomLevel));

    }


    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case ID_STATIONS_LOADER:
                Uri stationsQueryUri = TidesEntry.STATION_INFO_CONTENT_URI;
                return new CursorLoader(this,
                        stationsQueryUri,
                        args.getStringArray(PROJECTION_KEY),
                        null,
                        null,
                        null);
            default:
                throw new RuntimeException("Loader not implemented: " + id);
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        Cursor dataCursor = (Cursor) data;
        switch(loader.getId()) {
            case ID_STATIONS_LOADER:
                if (dataCursor.getCount() != 0) {
                    mStationsList = MainActivity.parseStationCursor(dataCursor);

                    // Start map here, to ensure data is loaded
                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map_picker_fragment);
                    mapFragment.getMapAsync(this);
                }
                break;
            default:
                throw new RuntimeException("Loader not implemented: " + loader.getId());
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }
}
