package com.moti.android;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

public class MPMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    Circle circle;
    int radius = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_mpmap);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("MeetingPoints");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mpmap, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //TODO: maybe something?
            return;
        }
        googleMap.setMyLocationEnabled(true);
        radius = PreferenceManager.getDefaultSharedPreferences(this).getInt(MainActivity.RADIUS, 500);
        if (googleMap.getMyLocation() == null) {
            googleMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                @Override
                public void onMyLocationChange(Location location) {
                    googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                            .target(new LatLng(location.getLatitude(), location.getLongitude()))
                            .zoom(14.5f)
                            .build()));
                    circle = googleMap.addCircle(new CircleOptions().center(new LatLng(location.getLatitude(), location.getLongitude())).radius(radius).strokeColor(Color.RED).fillColor(Color.BLUE));
                    googleMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                        @Override
                        public void onMyLocationChange(Location location2) {
                            circle.setCenter(new LatLng(location2.getLatitude(), location2.getLongitude()));
                        }
                    });
                }
            });
        } else {
            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                    .target(new LatLng(googleMap.getMyLocation().getLatitude(), googleMap.getMyLocation().getLongitude()))
                    .zoom(15)
                    .build()));
        }
        googleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                new OnlineDatabaseHandler(MPMapActivity.this).getMeetingPoints(new OnlineDatabaseHandler.WebDbUser() {
                    @Override
                    public void onResult(JSONObject json) {
                        try {
                            for (int i = 0; i < json.getJSONArray("mps").length(); i++) {
                                String mploc = json.getJSONArray("mps").getJSONObject(i).getString("location");
                                double lat = Double.parseDouble(mploc.split(",")[0]);
                                double lng = Double.parseDouble(mploc.split(",")[1]);
                                googleMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(lat, lng))
                                        .title(json.getJSONArray("mps").getJSONObject(i).getString("name")));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, googleMap.getProjection().getVisibleRegion().latLngBounds.northeast, googleMap.getProjection().getVisibleRegion().latLngBounds.southwest);
            }
        });
    }

}
