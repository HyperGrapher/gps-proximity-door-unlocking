package com.hypergraph.mapapp.mitch;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.hypergraph.mapapp.utilities.AppDB;

import static com.hypergraph.mapapp.utilities.Constants.PREF_IS_USER_IN_RANGE;


public class LocationService extends Service {

    public static final String TAG = LocationService.class.getSimpleName();
    ;

    private FusedLocationProviderClient mFusedLocationClient;
    private final static long UPDATE_INTERVAL = 4 * 1000;  /* 4 secs */
    private final static long FASTEST_INTERVAL = 2000; /* 2 sec */

    private AppDB appDB;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        appDB = new AppDB(getApplicationContext());

        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "my_channel_01";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Lokasyon güncellemeleri",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("").build();

            startForeground(1, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: called.");
        getLocation();
        return START_STICKY;
    }

    private void getLocation() {

        // Create the location request to start receiving updates
        LocationRequest mLocationRequestHighAccuracy = new LocationRequest();
        mLocationRequestHighAccuracy.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequestHighAccuracy.setInterval(UPDATE_INTERVAL);
        mLocationRequestHighAccuracy.setFastestInterval(FASTEST_INTERVAL);


        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "getLocation: stopping the location service.");
            stopSelf();
            return;
        }
        Log.d(TAG, "getLocation: getting location information.");
        mFusedLocationClient.requestLocationUpdates(mLocationRequestHighAccuracy, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {

                        Log.d(TAG, "onLocationResult: got location result.");

                        Location location = locationResult.getLastLocation();


                        if (location != null) {
                            float[] distance = new float[2];

                            Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                                    37.039142, 35.308955, distance);

                            Log.i(TAG, "onLocationResult 2: Distance[0] " + distance[0]);

                            // TODO: Get radius from the api
                            if (distance[0] > 50) {

                                Toast.makeText(getBaseContext(), "You are not in a bunker", Toast.LENGTH_LONG).show();
                                appDB.putBoolean(PREF_IS_USER_IN_RANGE, false);

                            } else {


                                appDB.putBoolean(PREF_IS_USER_IN_RANGE, true);

                                Toast.makeText(getBaseContext(), "You are inside a bunker", Toast.LENGTH_LONG).show();

                                Log.i(TAG, "onLocationResult: INSIDE RADIUS");

                                Intent in = new Intent(LocationService.this, MitchActivity.class);
                                in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(in);


                            }


                            saveUserLocation(location.getLatitude(), location.getLongitude());
                        }
                    }
                },
                Looper.myLooper()); // Looper.myLooper tells this to repeat forever until thread is destroyed
    }


    private void saveUserLocation(double latitude, double longitude) {

        /**
         * remote DB işlemleri
         */
        Log.i(TAG, "onLocationResult: " + latitude + " " + longitude);
    }


}