package pt.ulisboa.tecnico.cmov.conversationalist;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;

import androidx.fragment.app.FragmentActivity;

import android.os.Looper;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cmov.conversationalist.databinding.ActivityGeofenceBinding;

public class GeofenceActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        View.OnClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private GoogleMap mMap;
    private ActivityGeofenceBinding binding;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    public Marker marker;
    Button button;
    EditText rad;
    public double latitude;
    public double longitude;
    public ArrayList<Geofence> geoList = new ArrayList<>();
    private GeofencingClient geofencingClient;
    private PendingIntent geofencePendingIntent;
    double radius;
    String chatname;
    String username;
    GeofenceActivity activity = this;
    Button currentLocation;
    private FusedLocationProviderClient fusedLocationClient;
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

        binding = ActivityGeofenceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Get the intent that started the activity and extract string message
        Intent intent = getIntent();
        chatname = intent.getStringExtra(MainActivity.NewChatFragment.IDENTIFIER);
        username = intent.getStringExtra("username");

        geofencingClient = LocationServices.getGeofencingClient(this);


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

        searchAddressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Geocoder geocoder = new Geocoder(activity);
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


        currentLocation = findViewById(R.id.currentLocation);

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Lisbon, Portugal.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        // Gets location access permission
        enableMyLocation();

        // Wait for click on map to set a marker
        mMap.setOnMapClickListener(this);
        button = (Button) findViewById(R.id.done_button);
        button.setOnClickListener(this);


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

    @Override
    public void onClick(View v) {

        // Get Geofence radius
        rad = (EditText) findViewById(R.id.radius);
        Log.d("radCon", rad.getText().toString());
        if (rad.getText().toString().equals("")){
            radius = 500;

        }
        else{
            radius = Double.parseDouble(rad.getText().toString());
        }


        Log.d("Convers", ""+ radius);

        // Create a new Geofence and add it to the Geofence List
        //addGeofence();

        // Add to web server
        String new_url = MainActivity.url + "/store_geo_atts/"+ username + "/" + chatname + "/" + latitude + "/" + longitude + "/" + radius;
        //String urlString = URLEncoder.encode(new_url);
        Log.d("adas", new_url);
        URL obj = null;
        try {
            obj = new URL(new_url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        GeoChatTask gct = new GeoChatTask(activity, binding.getRoot());
        gct.execute(obj);

        //Finish this activity
        this.finish();
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

    /*
    @SuppressLint("MissingPermission")
    private void addGeofence() {

        geoList.add(new Geofence.Builder()
                .setRequestId(chatname)
                .setCircularRegion(latitude, longitude, (float) radius)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setNotificationResponsiveness(1000)
                .build());

        geofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnSuccessListener(this, aVoid -> {
                    Toast.makeText(getApplicationContext()
                            , "Geofencing has started", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(this, e -> {
                    Toast.makeText(getApplicationContext()
                            , "Geofencing failed", Toast.LENGTH_SHORT).show();

                });
    }

    private void removeGeofence(String chatname) {
        geofencingClient.removeGeofences(getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Geofences removed, remove from geoList
                        Geofence obj = getGeofence(chatname);
                        if (obj != null) {
                            geoList.remove(obj);
                            Toast.makeText(getApplicationContext(), "Geofence removed", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Chat name doesnt correspond to any active geofence", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), "Failed Geofence removal", Toast.LENGTH_LONG).show();
                    }
                });

    }

    // Returns the Geofence form the geoList given the chat name
    private Geofence getGeofence(String chatname) {
        for(Geofence g: geoList) {
            if (g.getRequestId().equals(chatname)){
                return g;
            }
        }
        return null;
    }


    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geoList);
        return builder.build();
    }

    // Starts a BroadcastReceiver
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        geofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return geofencePendingIntent;
    }
    */

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
        Log.d("enable my", "aqui");
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
        ActivityCompat.requestPermissions(GeofenceActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
        boolean shouldProvideRationale2 =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        permission.ACCESS_COARSE_LOCATION);
        ActivityCompat.requestPermissions(GeofenceActivity.this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
        boolean shouldProvideRationale3 =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        permission.ACCESS_BACKGROUND_LOCATION);
        ActivityCompat.requestPermissions(GeofenceActivity.this,
                new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }
        }
    }
}

/*

class GeofenceBroadcastReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceStatusCodes
                    .getStatusCodeString(geofencingEvent.getErrorCode());
            Log.e("TAG", errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            String locId = triggeringGeofences.get(0).getRequestId();
            sendNotification(locId, context);
        } else {
            // Log the error.
            Log.e("TAG", "error seding notification");
        }
    }

    private void sendNotification(String locId, Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "unique id")
                .setContentTitle("Location Reached")
                .setContentText(" you reached " + locId)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(1, builder.build());
    }
}


 */

class GeoChatTask extends AsyncTask<URL, Integer, Integer> {

    private final GeofenceActivity mainActivity;
    private View view;
    private URL url_obj;

    public GeoChatTask(GeofenceActivity activity, View view) {
        this.mainActivity = activity;
        this.view = view;
    }

    @Override
    protected Integer doInBackground(URL... urls) {

        URL obj = urls[0];
        this.setUrl_obj(obj);

        HttpURLConnection con = null;
        int responseCode = 0;

        try {
            con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "conversationalIST");
            responseCode = con.getResponseCode();
        } catch (Exception e) {
            Log.d("Response_Code", "" + responseCode);
            Log.d("Exception_log_task", "" + e);
        }

        InputStream responseBody = null;
        InputStreamReader responseBodyReader = null;

        try {
            responseBody = con.getInputStream();
            responseBodyReader = new InputStreamReader(responseBody, StandardCharsets.UTF_8);
        } catch (Exception e){
            Log.d("Execution_log_task", "" + e);
        }

        JsonReader jsonReader = new JsonReader(responseBodyReader);

        int response = -1;
        try {
            jsonReader.beginObject();
            while(jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                if(name.equals("name_free")) {
                    response = jsonReader.nextInt();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }




        return response;
    }

    @Override
    protected void onPostExecute(Integer response) {

                if (response == 0) {
                    Log.d("ERROR", "NAME ALREADY EXISTS");
                    Snackbar message_taken = Snackbar.make(view, "Conversation name already taken", Snackbar.LENGTH_SHORT );
                    message_taken.show();
                }
                else if (response == 1) {
                    Log.d("TAG", "added to the db");
                    Intent intent = new Intent(mainActivity, MainActivity.class);
                    intent.putExtra("join", 1);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    mainActivity.startActivity(intent);

                }
    }

    public void setUrl_obj(URL url_obj) {
        this.url_obj = url_obj;
    }
}