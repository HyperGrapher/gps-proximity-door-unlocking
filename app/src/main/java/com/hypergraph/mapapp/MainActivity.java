package com.hypergraph.mapapp;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.hypergraph.mapapp.utilities.AppDB;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpsTransportSE;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;

import static com.hypergraph.mapapp.utilities.Constants.GET_USER_METHOD_NAME;
import static com.hypergraph.mapapp.utilities.Constants.PREF_IS_USER_IN_RANGE;
import static com.hypergraph.mapapp.utilities.Constants.SERVICE_BASE_URL;
import static com.hypergraph.mapapp.utilities.Constants.SERVICE_GET_USER_SOAP_ACTION;
import static com.hypergraph.mapapp.utilities.Constants.SERVICE_NAMESPACE;
import static com.hypergraph.mapapp.utilities.Constants.SERVICE_PASSWORD;
import static com.hypergraph.mapapp.utilities.Constants.SERVICE_PASSWORD_KEY;
import static com.hypergraph.mapapp.utilities.Constants.SERVICE_PHONE_KEY;
import static com.hypergraph.mapapp.utilities.Constants.TEST_PHONE_NO;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";

    private GoogleMap mMap;
    private AppDB appDB;
    private boolean hasLocationPermission;
    public final static int TAG_PERMISSION_CODE = 1;
    private String response;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mitch);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.gmap);
        mapFragment.getMapAsync(this);

        appDB = new AppDB(getApplicationContext());

        hasLocationPermission = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!hasLocationPermission) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    TAG_PERMISSION_CODE);

        }

        startLocationService();

//        new GetUserCall().execute();


    }

    private void startLocationService() {
        if (!isLocationServiceRunning()) {
            Intent serviceIntent = new Intent(this, LocationService.class);
            // this.startService(serviceIntent);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

                MainActivity.this.startForegroundService(serviceIntent);
                Log.i(TAG, "startLocationService: STARTED O");

            } else {

                startService(serviceIntent);
                Log.i(TAG, "startLocationService: STARTED < O");

            }
        }
    }

    private boolean isLocationServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {

            if (LocationService.TAG.equals(service.service.getClassName())) {

                Log.d(TAG, "isLocationServiceRunning: location service is already running.");
                return true;
            }
        }

        Log.d(TAG, "isLocationServiceRunning: location service is not running.");

        return false;
    }


    @Override
    public void onMapReady(GoogleMap map) {
//        LatLng ny = new LatLng(37.038772, 35.308586);
        LatLng ny = new LatLng(37.039142, 35.308955);
        map.addMarker(new MarkerOptions().position(ny).title("Merkez"));
        map.setMinZoomPreference(18.0f);
        map.moveCamera(CameraUpdateFactory.newLatLng(ny));

        Circle circle = map.addCircle(new CircleOptions()
//                .center(new LatLng(37.038772, 35.308586))
                        .center(new LatLng(37.039142, 35.308955))
                        .radius(50)
                        .strokeColor(Color.RED)
        );

        Log.i(TAG, "onMapReady: Circle Distance: " + circle.getRadius());


    }

    public class GetUserCall extends AsyncTask<String, Object, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            SoapObject req = new SoapObject(SERVICE_NAMESPACE, GET_USER_METHOD_NAME);
            req.addProperty(SERVICE_PASSWORD_KEY, SERVICE_PASSWORD);
            req.addProperty(SERVICE_PHONE_KEY, TEST_PHONE_NO);

            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.dotNet = true;
            envelope.setOutputSoapObject(req);
            HttpsTransportSE transportSE = new HttpsTransportSE(SERVICE_BASE_URL, 808, "Service1.svc", 30000);

            try {

                Log.i(TAG, "doInBackground: RESPONSE RUN");
                transportSE.call(SERVICE_GET_USER_SOAP_ACTION, envelope);
                response = (String) envelope.getResponse();


            } catch (SocketTimeoutException e) {
                Log.e(TAG, "timeout", e);
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException f) {
                f.printStackTrace();

            } catch (Exception e) {
                e.printStackTrace();
            }


            return response;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if (response != null) Log.i(TAG, "onPostExecute: RESPONSE: " + response);
            else Log.i(TAG, "onPostExecute: !! NULL Response!!");
        }
    }


    @Override
    public void onBackPressed() {

        if (!appDB.getBoolean(PREF_IS_USER_IN_RANGE)) super.onBackPressed();
    }


}
