package com.moti.android;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

@SuppressWarnings("MissingPermission")
public class MainService extends Service implements GoogleApiClient.ConnectionCallbacks, LocationListener {


    public static final String RECEIVE_UPDATE = "MOTI_MAIN_SERVICE_RECEIVE_UPDATE";

    SharedPreferences sp;

    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest = new LocationRequest();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .build();
        mLocationRequest.setInterval(5 * 60 * 1000);
        mLocationRequest.setFastestInterval(20 * 1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (sp.getBoolean(MainActivity.VISIBLE, true)) {
            mGoogleApiClient.connect();
            handler.postDelayed(timer, 10 * 60 * 1000);
        }
        registerReceiver(visibility_receiver, new IntentFilter(MainActivity.RECEIVE_VISBILITY));
        registerReceiver(update_receiver, new IntentFilter(RECEIVE_UPDATE));
        return START_STICKY;
    }

    Handler handler = new Handler();
    Runnable timer = new Runnable() {
        @Override
        public void run() {
            long lastupdate = sp.getLong(MainActivity.LOCATION_UPDATE_TIME, -1);
            if (lastupdate < 0 || System.nanoTime() - lastupdate > 15 * 60 * 1000)
                update(LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient));
            handler.postDelayed(this, 10 * 60 * 1000);
        }
    };

    Location mCurrentLocation;
    long mLastUpdateTime;

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient) != null) {
            if (mCurrentLocation == null)
                update(LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient));
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else
            mGoogleApiClient.reconnect();
    }

    @Override
    public void onLocationChanged(Location location) {
        update(location);
    }

    public void update(Location location) {
        if (location.getLatitude() != 0 && location.getLongitude() != 0) {
            mCurrentLocation = location;
            mLastUpdateTime = System.nanoTime();
            sp.edit().putLong(MainActivity.LOCATION_UPDATE_TIME, mLastUpdateTime)
                    .putString(MainActivity.LOCATION_LAT, Double.toString(location.getLatitude()))
                    .putString(MainActivity.LOCATION_LNG, Double.toString(location.getLongitude()))
                    .apply();
            new OnlineDatabaseHandler(MainService.this).inup(null, OnlineDatabaseHandler.USERS,
                    OnlineDatabaseHandler.USERS_ID, sp.getString(MainActivity.UID, ""),
                    OnlineDatabaseHandler.USERS_LOCATION, Double.toString(location.getLatitude()) + "," + Double.toString(location.getLongitude()));
        }
        sendBroadcast(new Intent(HomeFragment.RECEIVE_UPDATE));
    }

    BroadcastReceiver update_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mGoogleApiClient.isConnected())
                update(LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient));
        }
    };

    BroadcastReceiver visibility_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (sp.getBoolean(MainActivity.VISIBLE, true) && !mGoogleApiClient.isConnected()) {
                mGoogleApiClient.connect();
                handler.postDelayed(timer, 10 * 60 * 1000);
            } else if (!sp.getBoolean(MainActivity.VISIBLE, true)) {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, MainService.this);
                handler.removeCallbacks(timer);
                mGoogleApiClient.disconnect();
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(timer);
        unregisterReceiver(visibility_receiver);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}