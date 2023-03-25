package pt.ulisboa.tecnico.cmov.conversationalist;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.List;

public class SendLocationActivity extends AppCompatActivity
        implements
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener
{
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 200;
    private GoogleMap mMap;
    public double latitude;
    public double longitude;
    public Marker marker;
    Button currentLocation;
    private FusedLocationProviderClient fusedLocationClient;
    SendLocationActivity activity;
    LocationRequest locationRequest = new LocationRequest()
            .setNumUpdates(1)
            .setFastestInterval(0)
            .setSmallestDisplacement(0)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    LocationListener locationListener;


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_location);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync((OnMapReadyCallback) this);

        currentLocation = findViewById(R.id.currentLocation);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }
        };


        fusedLocationClient.requestLocationUpdates(locationRequest,
                new com.google.android.gms.location.LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        locationListener.onLocationChanged(locationResult.getLastLocation());
                    }
                },
                Looper.getMainLooper());

        TextView addressText = findViewById(R.id.enterAddress);
        Button searchAddressButton = findViewById(R.id.enter);

        Context context = this;

        searchAddressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Geocoder geocoder = new Geocoder(context);
                List<Address> addresses = null;
                String address = addressText.getText().toString();
                try {
                    addresses = geocoder.getFromLocationName(address, 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(addresses!= null && addresses.size() > 0) {
                    addressText.setText("");
                    latitude= addresses.get(0).getLatitude();
                    longitude= addresses.get(0).getLongitude();
                    mMap.clear();

                    LatLng latLng = new LatLng(latitude, longitude);
                    // Adds new marker and goes center the screen on it
                    marker = mMap.addMarker(new MarkerOptions().position(latLng).title("Geofence Location"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                }
                else{
                    Toast toast=Toast.makeText(getApplicationContext(),"Address not found",Toast.LENGTH_SHORT);
                    toast.setMargin(50,50);
                    toast.show();
                }
            }
        });

        Button submit = findViewById(R.id.done_button);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent data = new Intent();
                String coordinates = "" + latitude + "," + longitude;
                data.setData(Uri.parse(coordinates));
                setResult(RESULT_OK, data);
                finish();
            }
        });


    }



    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        // Gets location access permission
        enableMyLocation();

        // Wait for click on map to set a marker
        mMap.setOnMapClickListener((GoogleMap.OnMapClickListener) this);

        currentLocation.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View view) {
                if (!checkPermissions()) {
                    requestPermissions();
                } else {
                    mMap.setMyLocationEnabled(true);
                    getCurrentLocation();
                    mMap.clear();
                    LatLng latLng = new LatLng(latitude, longitude);
                    marker = mMap.addMarker(new MarkerOptions().position(latLng).title("Geofence Location"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                }
            }
        });

    }

    @SuppressLint("MissingPermission")
    public void getCurrentLocation(){
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                }
            }
        });
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        // set new lat and lng to marker
        latitude = latLng.latitude;
        longitude = latLng.longitude;

        // Clears all previous markers
        mMap.clear();

        // Adds new marker and goes center the screen on it
        marker = mMap.addMarker(new MarkerOptions().position(latLng).title("Geofence Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        // Display message with lat and lng
        Toast.makeText(this, "Latitude" + latLng.latitude + "Longitude" + latLng.longitude, Toast.LENGTH_LONG).show();
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocation() {
        /*
        // 1. Check if permissions are granted, if so, enable the my location layer
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            return;
        }

        // 2. Otherwise, request location permissions from the user.
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    */
        if (!checkPermissions()) {
            requestPermissions();
        } else {
            mMap.setMyLocationEnabled(true);
        }
    }

    public boolean checkPermissions(){

        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    public void requestPermissions(){
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);
        ActivityCompat.requestPermissions(SendLocationActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
        boolean shouldProvideRationale2 =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION);
        ActivityCompat.requestPermissions(SendLocationActivity.this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
        boolean shouldProvideRationale3 =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        ActivityCompat.requestPermissions(SendLocationActivity.this,
                new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }
        }
    }

}