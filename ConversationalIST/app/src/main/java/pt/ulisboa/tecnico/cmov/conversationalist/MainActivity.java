package pt.ulisboa.tecnico.cmov.conversationalist;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;


public class MainActivity extends AppCompatActivity {

    static String aux = "192.168.21.51:5000";
    static String url = "http://" + aux;
    static String ws = "ws://" + aux;
    static String username;
    public List<Map<String, String>> chatList;
    public CustomAdapter adapter;
    public static int flagNotif= 0;
    public static int flagCache = 0;
    final Handler handler = new Handler();
    public Runnable a;
    Conversationalist appState;
    WebSocketClient mWebSocketClient;
    private SharedPreferences settings;
    int join = 0;
    ArrayList<String> newMessages = new ArrayList<>();
    String geoChatName;
    public static final String IDENTIFIER = "chat name identifier";


    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 200;
    public double latitude;
    public double longitude;
    private FusedLocationProviderClient fusedLocationClient;
    LocationRequest locationRequest = new LocationRequest()
            .setInterval(2000)
            .setFastestInterval(0)
            .setSmallestDisplacement(0)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    LocationListener locationListener;
    public List<Map<String, String>> geofences = new ArrayList<Map<String, String>>();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.new_chat) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment, new NewChatFragment());
            ft.addToBackStack(null);
            ft.commit();
        }
        if (item.getItemId() == R.id.join_chat) {
            Intent myIntent = new Intent(this, SearchChatActivity.class);
            startActivity(myIntent);
        }
        if (item.getItemId() == R.id.dark_mode) {
            if(AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MainActivity activity = this;

        this.settings = getSharedPreferences("mySharedPref", 0);
        username = settings.getString("user_name", null);

        Intent intentJoin = getIntent();
        join = intentJoin.getIntExtra("join", 0);

        // Check if user doesn't exist, loads Login Activity to create new user
        if (username == null){
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
        // If user exists, loads the users chatrooms
        else {
            if (flagNotif == 0) {
                Intent i = new Intent(this, NotificationService.class);
                i.putExtra("chatList", (Serializable) chatList);

                this.startForegroundService(i);
                flagNotif = 1;

            }
            Intent link = getIntent();
            String appLinkAction = link.getAction();
            Uri appLinkData = link.getData();
            showDeepLinkOffer(appLinkAction, appLinkData);

            ListView chatListView = findViewById(R.id.chat_list);

            appState = ((Conversationalist)this.getApplication());
            List<Map<String, String>> cList = appState.getChatsFromCache(username);

            try {
                connectWebSocket();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (cList == null || join == 1 ){
                String new_url = url + "/" + username + "/convos";
                URL obj = null;
                try {
                    obj = new URL(new_url);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                GetChats getChatsTask = new GetChats(chatListView, this, 0);
                getChatsTask.execute(obj);
            }
            else{
                if (cList.size()>0){
                    chatList = cList;
                    adapter = new CustomAdapter(this, cList);
                    ((ListView) chatListView).setAdapter(adapter);
                    for (Map<String,String> chat: chatList){
                        Log.d("permits", ""+ chat);
                        if (chat.get("type").equals("Geo-fenced")){
                            if (!checkPermissions()) {
                                requestPermissions();
                            }
                        }

                    }
                }
                else{
                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.fragment2, new GetChats.NoChatFragment());
                    ft.commit();
                }
            }


            chatListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if(((TextView) (view.findViewById(R.id.txtRef))).getCurrentTextColor() != Color.GRAY) {
                        ((TextView) (view.findViewById(R.id.txtRef))).setTypeface(null, Typeface.NORMAL);
                        ((TextView) (view.findViewById(R.id.txtOf))).setTypeface(null, Typeface.NORMAL);
                        chatList.get(position).put("read", "1");
                        String chatname = ((TextView) (view.findViewById(R.id.txtRef))).getText().toString();
                        Intent intent = new Intent(activity, ChatActivity.class);
                        intent.putExtra("chatname", chatname);
                        intent.putExtra("ws", ws);
                        intent.putExtra("type", ((TextView) (view.findViewById(R.id.txtOf))).getText().toString());
                        intent.putExtra("url", url);
                        intent.putExtra("username", username);
                        int geoFlag = 0;
                        if (((TextView) (view.findViewById(R.id.txtOf))).getText().toString().equals("Geo-fenced")) {
                            Log.d("ingeochat", ((TextView) (view.findViewById(R.id.txtOf))).getText().toString());
                            geoFlag = 1;
                            for (Map<String, String> hashMap : geofences)
                            {
                                if (hashMap.get("name").equals(chatname)) {
                                    intent.putExtra("latitude", hashMap.get("latitude"));
                                    intent.putExtra("longitude", hashMap.get("longitude"));
                                    intent.putExtra("radius", hashMap.get("radius"));
                                    break;
                                }
                            }
                        }
                        intent.putExtra("geoFlag", geoFlag);

                        if (newMessages.contains(chatname)) {
                            intent.putExtra("newMessages", 1);
                            newMessages.remove(chatname);
                        }
                        else intent.putExtra("newMessages", 0);
                        startActivity(intent);
                    }

                }
            });

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
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
        boolean shouldProvideRationale2 =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION);
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
        boolean shouldProvideRationale3 =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void closeWebSocket() {
        mWebSocketClient.close();
    }

    private void connectWebSocket() throws InterruptedException {
        URI uri = null;
        String wsUri = ws + "/echo/" + username + "Main";
        Log.d("Websocket", wsUri);
        try {
            uri = new URI(wsUri);
        } catch (URISyntaxException e) {
            Log.d("WebsocketExc", ""+e);
        }
        mWebSocketClient = new MainActivityWebSocketClient(uri, this);
        mWebSocketClient.connectBlocking();
    }

    public void getMessagesToCache(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission") Network nw = connectivityManager.getActiveNetwork();
        @SuppressLint("MissingPermission") NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
        if(actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            if (flagCache == 0){
                Log.d("entrou", "cahing"+ chatList);
                String chatName = null;
                for (int i=0; i<chatList.size(); i++) {
                    chatName = chatList.get(i).get("name");
                    Log.d("entrou", "chatName= "+ chatName);
                    String new_url = url + "/getCacheMessages/" + chatName;
                    URL obj = null;
                    try {
                        obj = new URL(new_url);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    GetCacheMessagesTask getCacheMessagesTask = new GetCacheMessagesTask(this, username, appState, chatName);
                    getCacheMessagesTask.execute(obj);
                }
                flagCache = 1;
            }
        }
    }



    public void setChats(List<Map<String, String>> chatList){
        Conversationalist appState = ((Conversationalist)this.getApplication());
        appState.addChatsToCache(username, chatList);
        Log.d("ChatCache", "caching");
    }

    @Override
    public void onDestroy(){
        if (mWebSocketClient != null)  mWebSocketClient.close(1000, "close");
        super.onDestroy();
        handler.removeCallbacks(a);
    }

    private void showDeepLinkOffer(String appLinkAction, Uri appLinkData) {
        // 1
        if (Intent.ACTION_VIEW.equals(appLinkAction) && appLinkData != null) {
            // 2
            String chatName = appLinkData.getQueryParameter("chatName");
            String new_url = url + "/convos/" + chatName + "/" + username;
            URL obj = null;
            {
                try {
                    obj = new URL(new_url);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
            JoinChatTask joinChatTask = new JoinChatTask(this);
            joinChatTask.execute(obj);
            //this.recreate();
        }
    }

    public void messageReceived(String user, String chatName) {


        String new_url = url + "/" + username + "/convos";
        URL obj = null;
        {
            try {
                obj = new URL(new_url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        newMessages.add(chatName);
        Log.d("kata", "NEW MESSAGE AT " + chatName);
        ListView chatListView = findViewById(R.id.chat_list);
        GetChats gcTask = new GetChats(chatListView, this, 1);
        gcTask.execute(obj);
    }

    // Inflates the new_chat layout, to create a new chatroom
    public static class NewChatFragment extends Fragment implements View.OnClickListener {
        Button myButton;
        private RadioGroup radioGroup;
        private RadioButton radioButton;
        private View myView;
        private String chatname;
        private MainActivity mainActivity;
        public static final String IDENTIFIER = "chat name identifier";
        public static Fragment newInstance() {
            return new NewChatFragment();
        }
        @NonNull
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                                 @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            myView = inflater.inflate(R.layout.new_chat_layout, container, false);
            myButton = (Button) myView.findViewById(R.id.submit_button);
            radioGroup = (RadioGroup) myView.findViewById(R.id.RGroup);
            myButton.setOnClickListener(this);
            mainActivity = ((MainActivity) getActivity());
            return myView;
        }


        @Override
        public void onClick(View v) {
            EditText mEdit = (EditText) myView.findViewById(R.id.editTextTextPersonName);
            int selectedId = radioGroup.getCheckedRadioButtonId();
            radioButton = (RadioButton) myView.findViewById(selectedId);
            chatname = mEdit.getText().toString();
            Log.d("radiopressed", "" + radioButton.getText());

            if(radioButton.getText().toString().equals("Geo-fenced")){
                Intent intent = new Intent(getActivity(), GeofenceActivity.class);
                intent.putExtra(IDENTIFIER, chatname);
                intent.putExtra("username", username);
                startActivity(intent);

                return;
            }


            String new_url = url + "/"+ username + "/createConvo/" + chatname + "/" + radioButton.getText();
            URL obj = null;
            {
                try {
                    obj = new URL(new_url);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
            Activity myActivity = getActivity();
            String chatType = radioButton.getText().toString();
            CreateChatTask createChatTask = new CreateChatTask(myView.getRootView(), myActivity, chatname, chatType, mainActivity);
            createChatTask.execute(obj);
        }
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

}

class GetChats extends AsyncTask<URL, Integer, Integer> {

    private final ListView rootView;
    private final MainActivity mainActivity;
    private final int update;
    private URL url_obj = null;



    public GetChats(View rootView, MainActivity mainActivity, int update) {
        this.rootView = (ListView) rootView;
        this.mainActivity = mainActivity;
        //0 - Create list | 1 - Update list
        this.update = update;
        this.mainActivity.chatList = new ArrayList<>();
    }


    @Override
    protected Integer doInBackground(URL... urls) {
        URL obj = urls[0];
        this.setUrl_obj(obj);

        HttpURLConnection con = null;
        int responseCode = 0;
        try {
            con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "conversationalIST");
            responseCode = con.getResponseCode();
        } catch (Exception e) {
            Log.d("Response_Code", ""+responseCode);
            Log.d("Exception_log_task", "" + e);
        }

        InputStream responseBody = null;
        InputStreamReader responseBodyReader = null;
        try {
            responseBody = con.getInputStream();
            responseBodyReader =
                    new InputStreamReader(responseBody, "UTF-8");
        } catch (Exception e) {
            Log.d("Exception_log_task", "" + e);
        }

        JsonReader jsonReader = new JsonReader(responseBodyReader);

        String chatName = null;
        String chatType = null;
        String latitude = null;
        String longitude = null;
        int radius = 0;
        int read = 1;

        try {
            jsonReader.beginArray();
            while(jsonReader.hasNext()) {
                int geo = 0;
                jsonReader.beginObject();
                while (jsonReader.hasNext()){
                    String name = jsonReader.nextName();
                    if (name.equals("no_chats")){
                        FragmentTransaction ft = mainActivity.getSupportFragmentManager().beginTransaction();
                        ft.replace(R.id.fragment2, new NoChatFragment());
                        ft.commitAllowingStateLoss();
                        return null;
                    }
                    if(name.equals("name")) {
                        chatName = jsonReader.nextString();
                        Log.d("chatname", chatName);
                    }
                    if(name.equals("type")) {
                        chatType = jsonReader.nextString();
                    }
                    if(name.equals("id"))  {
                        jsonReader.nextInt();
                    }
                    if(name.equals("read")) {
                        read = jsonReader.nextInt();
                    }
                    if(name.equals("latitude")) {
                        if (jsonReader.peek() == JsonToken.NULL) {
                            jsonReader.nextNull();
                        }
                        else {
                            geo = 1;
                            latitude = jsonReader.nextString();
                        }
                    }
                    if(name.equals("longitude")) {
                        if (jsonReader.peek() == JsonToken.NULL) {
                            jsonReader.nextNull();
                        }
                        else {
                            longitude = jsonReader.nextString();
                        }
                    }
                    if(name.equals("radius")) {
                        if (jsonReader.peek() == JsonToken.NULL) {
                            jsonReader.nextNull();
                        }
                        else {
                            radius = jsonReader.nextInt();
                        }
                    }
                }
                jsonReader.endObject();
                Map<String, String> datum = new HashMap<String, String>(2);
                datum.put("name", chatName);
                datum.put("type", chatType);
                datum.put("read", String.valueOf(read));
                //mainActivity.chatList.add(datum);
                int inRadius = -1;
                if(geo == 1) {
                    inRadius = 0;
                    mainActivity.getCurrentLocation();
                    float[] distance = new float[2];
                    Location.distanceBetween(mainActivity.latitude, mainActivity.longitude, Double.parseDouble(latitude), Double.parseDouble(longitude), distance);
                    Log.d("distance", "distance to " + chatName + " " + distance[0]);
                    if (distance[0] < radius) {
                        inRadius = 1;
                    }

                    Map<String, String> geoDatum = new HashMap<String, String>(2);
                    geoDatum.put("name", chatName);
                    geoDatum.put("latitude", latitude);
                    geoDatum.put("longitude", longitude);
                    geoDatum.put("radius", String.valueOf(radius));
                    mainActivity.geofences.add(geoDatum);

                }
                Log.d("geoTest", chatName + " inRadius" + " " + inRadius);
                datum.put("inRadius", String.valueOf(inRadius));
                mainActivity.chatList.add(datum);
            }
            jsonReader.endArray();

        } catch (IOException e) {
            e.printStackTrace();
        }


        return 0;
    }

    @Override
    protected void onPostExecute(Integer integer) {
        super.onPostExecute(integer);

        if(update == 0) {
            mainActivity.setChats(mainActivity.chatList);
            mainActivity.adapter = new CustomAdapter(mainActivity, mainActivity.chatList);
            ((ListView) this.rootView).setAdapter(mainActivity.adapter);
            mainActivity.getMessagesToCache();
        }
        else {
            if(mainActivity.adapter != null) {
                mainActivity.adapter.updateList(mainActivity.chatList);
            }

        }

        for (Map<String,String> chat: mainActivity.chatList){
            Log.d("permits", ""+ chat.get("type"));
            if (chat.get("type").equals("Geo-fenced")){
                if (!mainActivity.checkPermissions()) {
                    mainActivity.requestPermissions();
                }
            }
        }
    }

    public static class NoChatFragment extends Fragment {
        @NonNull
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                                 @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.no_chat_layout, container, false);
        }
    }

    public void setUrl_obj(URL url_obj) {
        this.url_obj = url_obj;
    }

    public URL getUrl_obj() {
        return url_obj;
    }
}

class CreateChatTask extends AsyncTask<URL, Integer, Integer> {

    private final String chatName;
    private final String chatType;
    private final MainActivity mainActivity;
    private URL url_obj = null;
    private View rootView;
    private Activity activity;

    public CreateChatTask(View rootView, Activity myActivity, String chatName, String chatType, MainActivity mainActivity) {
        this.rootView = rootView;
        this.activity = myActivity;
        this.chatName = chatName;
        this.chatType = chatType;
        this.mainActivity = mainActivity;
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
            Log.d("Response_Code", ""+responseCode);
            Log.d("Exception_log_task", "" + e);
        }

        InputStream responseBody = null;
        InputStreamReader responseBodyReader = null;
        try {
            responseBody = con.getInputStream();
            responseBodyReader =
                    new InputStreamReader(responseBody, "UTF-8");
        } catch (Exception e) {
            Log.d("Exception_log_task", "" + e);
        }

        JsonReader jsonReader = new JsonReader(responseBodyReader);

        int response = -1;
        try {
            jsonReader.beginObject();
            while(jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                if(name.equals("name_free")) {
                    response = jsonReader.nextInt();
                    Log.d("response", "" +response);

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    @Override
    protected void onPostExecute(Integer response) {
        super.onPostExecute(response);

        if(response == 0) {
            Snackbar message_taken = Snackbar.make(rootView, "Conversation name already taken", Snackbar.LENGTH_SHORT );
            message_taken.show();
        }
        else if(response == 1) {
            Map<String, String> datum = new HashMap<String, String>(2);
            datum.put("name", chatName);
            datum.put("type", chatType);
            datum.put("read", "1");
            mainActivity.chatList.add(datum);
            mainActivity.setChats(mainActivity.chatList);
            this.activity.onBackPressed();
            Intent intent = activity.getIntent();
            activity.finish();
            activity.startActivity(intent);
        }
    }

    public void setUrl_obj(URL url_obj) {
        this.url_obj = url_obj;
    }

    public URL getUrl_obj() {
        return url_obj;
    }
}

class GetCacheMessagesTask extends AsyncTask<URL, Integer, BasicMessageList> {


    private final MainActivity rootAcitvity;
    private final String username;
    private final String chatName;
    private final Conversationalist appState;


    public GetCacheMessagesTask(MainActivity rootActivity, String username, Conversationalist appState, String chatName) {
        this.rootAcitvity = rootActivity;
        this.username = username;
        this.chatName = chatName;
        this.appState = appState;
    }


    @Override
    protected BasicMessageList doInBackground(URL... urls) {
        URL obj = urls[0];

        HttpURLConnection con = null;
        int responseCode = 0;


        try {
            con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "conversationalIST");
            responseCode = con.getResponseCode();
        } catch (Exception e) {
            Log.d("Response_Code", ""+responseCode);
            Log.d("Exception_log_task", "" + e);
        }

        InputStream responseBody = null;
        InputStreamReader responseBodyReader = null;
        try {
            responseBody = con.getInputStream();
            responseBodyReader =
                    new InputStreamReader(responseBody, "UTF-8");
        } catch (Exception e) {
            Log.d("Exception_log_task", "" + e);
        }

        JsonReader jsonReader = new JsonReader(responseBodyReader);
        List<Integer> is_photoList = new ArrayList<Integer>();
        List<Integer> is_mapList = new ArrayList<Integer>();
        List<Integer> is_pollList = new ArrayList<Integer>();
        int is_photo = 0;
        int is_map = 0;
        int is_poll = 0;

        try {
            jsonReader.beginArray();
            while(jsonReader.hasNext()) {
                jsonReader.beginObject();
                while (jsonReader.hasNext()){
                    String name = jsonReader.nextName();
                    if(name.equals("is_photo")) {
                        is_photo = jsonReader.nextInt();
                    }
                    if(name.equals("is_map")) {
                        is_map = jsonReader.nextInt();
                    }
                    if(name.equals("is_poll")) {
                        is_poll = jsonReader.nextInt();
                    }
                }
                jsonReader.endObject();
                is_photoList.add(is_photo);
                is_mapList.add(is_map);
                is_pollList.add(is_poll);
            }
            jsonReader.endArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "conversationalIST");
            responseCode = con.getResponseCode();
        } catch (Exception e) {
            Log.d("Response_Code", ""+responseCode);
            Log.d("Exception_log_task", "" + e);
        }


        responseBody = null;
        responseBodyReader = null;
        try {
            responseBody = con.getInputStream();
            responseBodyReader =
                    new InputStreamReader(responseBody, "UTF-8");
        } catch (Exception e) {
            Log.d("Exception_log_task", "" + e);
        }

        jsonReader = new JsonReader(responseBodyReader);
        ConnectivityManager connectivityManager = null;
        List<String> contentList = new ArrayList<String>();
        List<String> sendersList = new ArrayList<String>();
        List<String> datesList = new ArrayList<String>();
        List<Integer> idsList = new ArrayList<Integer>();
        String message= null;
        String sender= null;
        String date= null;
        int id = 0;

        try {
            jsonReader.beginArray();
            int i = 0;
            while(jsonReader.hasNext()) {
                jsonReader.beginObject();
                while (jsonReader.hasNext()){
                    String name = jsonReader.nextName();
                    if(name.equals("sender")) {
                        sender = jsonReader.nextString();
                    }
                    if(name.equals("time"))  {
                        date = jsonReader.nextString();
                    }
                    if(name.equals("id"))  {
                        id = jsonReader.nextInt();
                        Log.d("forax", ""+ id);
                    }
                    if(name.equals("content")) {
                        Log.d("connected", "aqui");
                        if (is_photoList.get(i) == 1) {
                            connectivityManager = (ConnectivityManager) rootAcitvity.getSystemService(Context.CONNECTIVITY_SERVICE);
                            @SuppressLint("MissingPermission") Network nw = connectivityManager.getActiveNetwork();
                            @SuppressLint("MissingPermission") NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
                            if(actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                                message = "placeholder";
                                jsonReader.skipValue();
                                //message = jsonReader.nextString();
                            }
                            else{
                                message = jsonReader.nextString();
                            }
                        }
                        else{
                            message = jsonReader.nextString();
                        }
                    }
                }
                i+=1;
                jsonReader.endObject();
                contentList.add(message);
                sendersList.add(sender);
                datesList.add(date);
                idsList.add(id);
            }
            jsonReader.endArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        BasicMessageList messageListO = new BasicMessageList();
        messageListO.setMessageList(sendersList, contentList, datesList, is_photoList, is_mapList, is_pollList, idsList);

        return messageListO;
    }

    @Override
    protected void onPostExecute(BasicMessageList messageListO){

        List<BasicMessage> messageList = messageListO.getMessageList();

        appState.addMessagesToCache(chatName, messageList);
    }
}

class MainActivityWebSocketClient extends WebSocketClient {
    private final java.net.URI URI ;
    private final MainActivity activity;

    public MainActivityWebSocketClient(URI serverURI, MainActivity mainActivity) {
        super(serverURI);
        this.URI = serverURI;
        this.activity = mainActivity;
    }

    @Override
    public void onOpen(ServerHandshake handShakeData) {
        Log.d("Websocket", "Opened");
        this.send("Hello world");
    }

    @Override
    public void onMessage(String message) {
        String[] response = message.split("//");
        Log.d("Websocket", "got message (user): "+ response[0]);
        Log.d("Websocket", "got message (convName): "+ response[1]);
        this.activity.messageReceived(response[0], response[1]);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d("Websocket", "Closed " + reason);
        if (reason.equals("close")) return;
        this.activity.mWebSocketClient = new MainActivityWebSocketClient(this.URI, this.activity);
        this.activity.mWebSocketClient.connect();
    }

    @Override
    public void onError(Exception ex) {
        Log.d("Websocket", "Error " + ex.getMessage());
    }
}