package com.hypergraph.mapapp;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
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
import com.google.android.material.button.MaterialButton;
import com.hypergraph.mapapp.utilities.AppDB;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpsTransportSE;

import static android.Manifest.permission.READ_PHONE_STATE;
import static com.hypergraph.mapapp.utilities.Constants.GET_USER_METHOD_NAME;
import static com.hypergraph.mapapp.utilities.Constants.PREF_IS_USER_IN_RANGE;
import static com.hypergraph.mapapp.utilities.Constants.PREF_USER_PHONE;
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
    private String response;

    private static final int PERMISSION_PHONE_STATE_REQUEST_CODE = 100;
    public final static int PERMISSION_LOCATION_REQUEST_CODE = 1;

    private MaterialButton enterBtn;
    private MaterialButton exitBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.gmap);
        mapFragment.getMapAsync(this);

        appDB = new AppDB(getApplicationContext());

        hasLocationPermission = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;


        if (ActivityCompat.checkSelfPermission(this,
                READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            TelephonyManager tMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
            String mPhoneNumber = tMgr.getLine1Number();
            Log.i(TAG, "onCreate: TLF" + mPhoneNumber);
            startLocationService();
        } else {
            requestPermission();
        }


//        new GetUserCall().execute();

        enterBtn = findViewById(R.id.btnEnter);
        exitBtn = findViewById(R.id.btnExit);

        setUpButtons();


    }

    public void setUpButtons() {

        if (appDB.getBoolean(PREF_IS_USER_IN_RANGE)) {
            enterBtn.setEnabled(true);
            exitBtn.setEnabled(true);
        } else {
            enterBtn.setEnabled(false);
            exitBtn.setEnabled(false);
        }

    }


    private void startLocationService() {
        Log.i(TAG, "startLocationService: " + isLocationServiceRunning());
        if (!isLocationServiceRunning()) {
            Intent serviceIntent = new Intent(this, LocationService.class);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

                MainActivity.this.startForegroundService(serviceIntent);
                Log.i(TAG, "startLocationService: STARTED OREO and above");

            } else {

                startService(serviceIntent);
                Log.i(TAG, "startLocationService: STARTED < LOWER THAN OREO");

            }
        }
    }

    private boolean isLocationServiceRunning() {

        Log.i(TAG, "isLocationServiceRunning: START");
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {

            Log.i(TAG, "isLocationServiceRunning: SERVICE: " + service.service.getClassName());

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
            Log.i(TAG, "doInBackground: RUN");
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
                SoapPrimitive xml = (SoapPrimitive) envelope.getResponse();
                Log.i(TAG, "doInBackground: XML" + xml.toString());
                response = "sasa";


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
    protected void onStart() {
        super.onStart();
        LocationService.isAppStartable = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocationService.isAppStartable = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocationService.isAppStartable = true;

    }



    @Override
    public void onBackPressed() {

        if (!appDB.getBoolean(PREF_IS_USER_IN_RANGE)) super.onBackPressed();
    }

    /**
     * *******************************************************************************
     * *******************************************************************************
     * *******************************************************************************
     * *******************************************************************************
     * *******************************************************************************
     */


    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{READ_PHONE_STATE}, PERMISSION_PHONE_STATE_REQUEST_CODE);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {

            case PERMISSION_LOCATION_REQUEST_CODE:
                startLocationService();
                break;

            case PERMISSION_PHONE_STATE_REQUEST_CODE:

                TelephonyManager tMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) !=
                        PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                String mPhoneNumber = tMgr.getLine1Number();
                Log.i(TAG, "onRequestPermissionsResult: TLF: " + mPhoneNumber);

                appDB.putString(PREF_USER_PHONE, mPhoneNumber);


                // After asking for phone number now ask for location permission
                if (!hasLocationPermission) {

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSION_LOCATION_REQUEST_CODE);

                }
                break;

        }
    }


}
