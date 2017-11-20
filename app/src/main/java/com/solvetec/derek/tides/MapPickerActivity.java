package com.solvetec.derek.tides;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.Wsdl2Code.WebServices.PredictionsService.Station;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.solvetec.derek.tides.data.TidesContract.TidesEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Derek on 11/11/2017.
 */

public class MapPickerActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapClickListener,
        LoaderManager.LoaderCallbacks,
        View.OnClickListener{

    private GoogleMap mMap;
    private Marker mSelectedMarker;
    private BottomSheetBehavior mBottomSheetBehavior;
    private TextView mTextViewStationName;
    private TextView mTextViewStationId;
    private TextView mTextViewStationLatLon;
    private Button mButtonSelectLocation;

    private Map<String, Station> mStationsMap;
    private String mPrefStationId;
    private Station mPrefStation;
    private Marker mOldPrefMarker;

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

        // Setup bottom sheet
        View bottomSheetFrame = findViewById(R.id.frame_bottom_sheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheetFrame);
        mBottomSheetBehavior.setHideable(true);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        mTextViewStationName = findViewById(R.id.tv_map_station_name);
        mTextViewStationId = findViewById(R.id.tv_map_station_id);
        mTextViewStationLatLon = findViewById(R.id.tv_map_station_latlon);
        mButtonSelectLocation = findViewById(R.id.button_map_select_location);
        mButtonSelectLocation.setOnClickListener(this);


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Disable quick settings that interfere with my bottom bar
        mMap.getUiSettings().setMapToolbarEnabled(false);

        // Load all markers
        List<Station> stationsList = (new ArrayList<>(mStationsMap.values()));
        mOldPrefMarker = null;
        for(int i = 0; i < mStationsMap.size(); i++) {
            Station station = stationsList.get(i);
            LatLng location = new LatLng(station.latitude, station.longitude);
            Marker marker = mMap.addMarker(new MarkerOptions().position(location).title(station.station_name));
            if(station.station_id.equals(mPrefStationId)) {
                mPrefStation = new Station(station);
                mOldPrefMarker = marker;
            }

            // Attach data to each marker
            marker.setTag(station);
        }

        // Change color of currently selected station marker
        if(mOldPrefMarker != null) {
            mOldPrefMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        }

        // Set marker click listeners
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);

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
                    mStationsMap = MainActivity.parseStationCursor(dataCursor);

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

    @Override
    public boolean onMarkerClick(Marker marker) {
        if(mSelectedMarker != null) {
            // Reset previously selected marker
            if(mSelectedMarker.equals(mOldPrefMarker)) {
                mSelectedMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            } else {
                mSelectedMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            }
        }

        // Set marker to SelectedColor
        mSelectedMarker = marker;
        mSelectedMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

        // Bring up bottom sheet
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // Load data for bottom sheet
        Station station = (Station) mSelectedMarker.getTag();
        if(station != null) {
            mTextViewStationName.setText(station.station_name);
            mTextViewStationId.setText(station.station_id);
            String latlon = Double.toString(station.latitude) + ", " + Double.toString(station.longitude);
            mTextViewStationLatLon.setText(latlon);
        }

        return false; // Don't consume markerClick. Allow camera to center on selected marker.
    }

    @Override
    public void onMapClick(LatLng latLng) {
        // Reset previously selected marker
        if(mSelectedMarker != null) {
            if(mSelectedMarker.equals(mOldPrefMarker)) {
                mSelectedMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            } else {
                mSelectedMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            }
            // Bring down bottom sheet
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
        mSelectedMarker = null;
    }

    // Button click listener
    @Override
    public void onClick(View v) {
        // If button pressed, and there is a marker selected
        if(v.equals(mButtonSelectLocation) && mSelectedMarker != null) {
            // Save new preferredLocation
            Context appContext = getApplicationContext();
            Station station = (Station) mSelectedMarker.getTag();
            if(station == null) {
                return;
            }

            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(appContext).edit();
            editor.putString(getString(R.string.pref_location_key), station.station_id);
            editor.commit();

            // Exit back to main page
            onBackPressed();
        }
    }
}
