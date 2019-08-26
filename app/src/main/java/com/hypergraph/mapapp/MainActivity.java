package com.hypergraph.mapapp;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";
    private Handler mHandler = new Handler();
    private Runnable mRunnable;
    private static final int LOCATION_UPDATE_INTERVAL = 3000;

    private MapView mMapView;
    private ProgressBar progressBar;
    private String MAPVIEW_BUNDLE_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMapView = findViewById(R.id.map);
        progressBar = findViewById(R.id.progressBar);

        MAPVIEW_BUNDLE_KEY = getString(R.string.google_maps_key);

        FusedLocationProviderClient locationServices = LocationServices.getFusedLocationProviderClient(this);

        // *** IMPORTANT ***
        // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
        // objects or sub-Bundles.
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mMapView = findViewById(R.id.map);
        mMapView.onCreate(mapViewBundle);

        mMapView.getMapAsync(this);

        startUserLocationsRunnable();
    }


    private void startUserLocationsRunnable() {
        Log.d(TAG, "startUserLocationsRunnable: starting runnable for retrieving updated locations.");
        mHandler.postDelayed(mRunnable = () -> {
            retrieveUserLocations();
            mHandler.postDelayed(mRunnable, LOCATION_UPDATE_INTERVAL);
        }, LOCATION_UPDATE_INTERVAL);
    }

    private void stopLocationUpdates() {
        mHandler.removeCallbacks(mRunnable);
    }

    private void retrieveUserLocations() {
        Log.d(TAG, "retrieveUserLocation");

    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }

        mMapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
        startUserLocationsRunnable();

    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
        stopLocationUpdates();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        LatLng ny = new LatLng(37.038772, 35.308586);
        map.addMarker(new MarkerOptions().position(ny).title("Marker"));
        map.setMinZoomPreference(18.0f);
        map.moveCamera(CameraUpdateFactory.newLatLng(ny));
        progressBar.setVisibility(View.GONE);


    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        stopLocationUpdates();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mMapView.onDestroy();
        stopLocationUpdates();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        stopLocationUpdates();
        mMapView.onLowMemory();
    }

}
