package pt.ulisboa.tecnico.cmov.conversationalist;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.JsonReader;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static android.content.Context.NOTIFICATION_SERVICE;

public class ChatActivity extends AppCompatActivity {


    private static final int CAMERA_REQUEST = 1888;
    private static final int LOCATION_REQUEST = 1999;
    private static final int POLL_REQUEST = 1777;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private static final int PERMISSION_REQUEST_CODE = 200;

    ChatActivity.MessageListAdapter mMessageAdapter;
    List<BasicMessage> messageList;
    ImageView photoView;
    EditText textMessageBox;
    RecyclerView recycler;
    String chatName;
    String url;
    String username;
    Bitmap photoBitmap;
    ImageView pollView;
    Boolean photoTaken = false;
    Boolean pollMade = false;
    Runnable a;
    final Handler handler = new Handler();
    RecyclerView mRecyclerView;
    View scrollLoading;
    RecyclerView.OnScrollListener scrollListener;
    String ws;
    WebSocketClient webSocketClient;
    int newMessages = 0;
    int geoFlag = 0;
    double latitude = 0;
    double longitude = 0;
    int radius = 0;
    double userLatitude;
    double userLongitude;
    private FusedLocationProviderClient fusedLocationClient;
    LocationRequest locationRequest = new LocationRequest()
            .setInterval(2000)
            .setFastestInterval(0)
            .setSmallestDisplacement(0)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    LocationListener locationListener;


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_layout);

        Intent myIntent = getIntent();
        this.chatName = myIntent.getStringExtra("chatname");
        this.url = myIntent.getStringExtra("url");
        this.ws = myIntent.getStringExtra("ws");
        this.username = myIntent.getStringExtra("username");
        String type = myIntent.getStringExtra("type");
        this.newMessages = myIntent.getIntExtra("newMessages", 0);
        this.geoFlag = myIntent.getIntExtra("geoFlag", 0);
        if (geoFlag == 1) {
            latitude = Double.parseDouble(myIntent.getStringExtra("latitude"));
            longitude = Double.parseDouble(myIntent.getStringExtra("longitude"));
            radius = Integer.parseInt(myIntent.getStringExtra("radius"));
            Log.d("geofences", "" + latitude + " " + longitude + " " + radius);
        }

        try {
            connectWebSocket();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(Objects.equals(type, "Private")) {
            findViewById(R.id.share_chat).setVisibility(View.VISIBLE);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                userLatitude = location.getLatitude();
                userLongitude = location.getLongitude();
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

        photoView = findViewById(R.id.take_photo_icon);
        pollView = (ImageView) findViewById(R.id.make_poll);
        Toolbar tb = (Toolbar) findViewById(R.id.toolbar_gchannel);
        TextView title = (TextView) tb.findViewById(R.id.toolbar_title);
        Button sendButton = (Button) findViewById(R.id.button_gchat_send);
        textMessageBox = (EditText) findViewById(R.id.edit_gchat_message);

        title.setText(chatName);

        ChatActivity rootActivity = this;
        MessageListAdapter messageAdapter = this.mMessageAdapter;
        ChatActivity mainActivity = this;

        Conversationalist appState = ((Conversationalist)this.getApplication());
        List<BasicMessage> mList = appState.getMessagesFromCache(chatName);

        /*if (mList == null || this.newMessages == 1){
            String new_url = url + "/messages/" + chatName + "/" + username;
            URL obj = null;
            try {
                obj = new URL(new_url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            GetMessages getMessagesTask = new GetMessages(findViewById(R.id.consLayout), rootActivity, username, url);
            getMessagesTask.execute(obj);
        }
        else {
            this.setMessages(mList);
        }*/

        if (mList == null) {
            Log.d("kata", "NO CACHE");
            String new_url = url + "/messages/" + chatName + "/" + username;
            URL obj = null;
            try {
                obj = new URL(new_url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            GetMessages getMessagesTask = new GetMessages(findViewById(R.id.consLayout), rootActivity, username, url);
            getMessagesTask.execute(obj);
        }
        else {
            Log.d("kata", "CACHE");
            this.setMessages(mList);
            if (newMessages == 1) {
                Log.d("kata", "CACHE INCOMPLETE");
                int mID = mMessageAdapter.mMessageList.get(mMessageAdapter.mMessageList.size() - 1).getId();
                String new_url = url + "/messages/realtime/" + username + "/" + chatName + "/" + mID;
                URL obj = null;
                try {
                    obj = new URL(new_url);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                MessageUpdate messageUpdate = new MessageUpdate(findViewById(R.id.consLayout), rootActivity, username, url);
                messageUpdate.execute(obj);
            }
        }


        ImageView imageView = findViewById(R.id.share_chat);
        View.OnClickListener clickListener = new View.OnClickListener() {
            public void onClick(View v) {
                if (v.equals(imageView)) {
                    try {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "ConversationalIST");
                        String shareMessage= "\nJoin this private chat!\n\n";
                        shareMessage = shareMessage + "http://www.conv-ist.com/joinchat?chatName=" + chatName + "\n\n";
                        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                        startActivity(Intent.createChooser(shareIntent, "choose one"));
                    } catch(Exception e) {
                        e.toString();
                    }
                }
            }
        };

        ImageView goHome = findViewById(R.id.go_home);
        goHome.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                finish();
            }
        });

        imageView.setOnClickListener(clickListener);

        sendButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                String textMessage = null;
                URL obj = null;

                if (!mainActivity.photoTaken) {
                    textMessage = textMessageBox.getText().toString();
                    String urlTextMessage = URLEncoder.encode(textMessage);
                    String new_url = url + "/text_sent/" + urlTextMessage + "/" + username + "/" + chatName;
                    try {
                        obj = new URL(new_url);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    photoView.setImageBitmap(null);
                    photoView.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_menu_camera));
                    textMessageBox.setVisibility(View.VISIBLE);
                    String new_url = url + "/image_sent/image/"+ username + "/" + chatName;
                    try {
                        obj = new URL(new_url);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
                int aux_id= 0;
                List<BasicMessage> mList = mMessageAdapter.mMessageList;
                if (mMessageAdapter.mMessageList.size()>0)
                    aux_id = mList.get(mList.size()-1).getId()+1;
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
                LocalDateTime lt = LocalDateTime.now();
                LocalDateTime time = LocalDateTime.parse(dtf.format(lt), dtf);
                BasicMessage new_message;
                if (!mainActivity.photoTaken) {
                    SendTextTask sendtextTask = new SendTextTask();
                    sendtextTask.execute(obj);
                    new_message = new BasicMessage(username, textMessage, time, aux_id);
                }
                else{
                    SendImageTask sendImageTask = new SendImageTask(mainActivity.photoBitmap);
                    sendImageTask.execute(obj);
                    new_message = new BasicMessage(username, mainActivity.photoBitmap, time, aux_id);
                    mainActivity.photoTaken = false;
                }
                mainActivity.updateMessageAdapter(new_message, textMessageBox);
            }
        });

        Button leaveButton = (Button) findViewById(R.id.leave_chat);
        leaveButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragmentLeave, new ChatActivity.LeaveFragment(mainActivity));
                ft.addToBackStack(null);
                ft.commit();

            }
        });


        photoView.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
                }
                else
                {
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                }

            }
        });

        pollView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pollIntent = new Intent(ChatActivity.this, PollActivity.class);
                startActivityForResult(pollIntent, POLL_REQUEST);
            }
        });


        ImageView locationView = findViewById(R.id.send_location);
        locationView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent locationIntent = new Intent(ChatActivity.this, SendLocationActivity.class);
                startActivityForResult(locationIntent, LOCATION_REQUEST);
            }
        });

        mRecyclerView = findViewById(R.id.recycler_gchat);

        scrollLoading = findViewById(R.id.loadingPanel);
        this.scrollListener();


        final int delay = 3000; // 1000 milliseconds == 1 second

      /*handler.postDelayed(a =new Runnable() {
            public void run() {
                int mID = 0;
                if(mMessageAdapter != null && mMessageAdapter.mMessageList.size() > 0) {
                    mID = mMessageAdapter.mMessageList.get(mMessageAdapter.mMessageList.size() - 1).getId();
                }
                    String new_url = url + "/messages/realtime/" + username + "/" + chatName + "/" + mID;
                    URL obj = null;
                    try {
                        obj = new URL(new_url);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    MessageUpdate messageUpdate = new MessageUpdate(findViewById(R.id.consLayout), rootActivity, username, url);
                    messageUpdate.execute(obj);
                handler.postDelayed(this, delay);
            }
        }, delay);*/
    }



    private void connectWebSocket() throws InterruptedException {
        URI uri = null;
        String wsUri = ws + "/echo/" + username + "Chat";
        Log.d("Websocket", wsUri);
        try {
            uri = new URI(wsUri);
        } catch (URISyntaxException e) {
            Log.d("WebsocketExc", ""+e);
        }
        this.webSocketClient = new ChatActivityWebSocketClient(uri, this);
        webSocketClient.connectBlocking();
    }

    private void setMessages(List<BasicMessage> mList) {
        this.messageList = mList;
        Drawable placeholder = getResources().getDrawable(android.R.drawable.ic_menu_gallery);

        RecyclerView mMessageRecycler = (RecyclerView) findViewById(R.id.recycler_gchat);
        ChatActivity.MessageListAdapter mMessageAdapter = new ChatActivity.MessageListAdapter(this, this.messageList, this.username, placeholder, this.url, this);
        mMessageRecycler.setLayoutManager(new LinearLayoutManager(this));
        mMessageRecycler.setAdapter(mMessageAdapter);

        setMessageAdapter(mMessageAdapter);
        setRecycler(mMessageRecycler);


        if (messageList.size() > 0)
            mMessageRecycler.scrollToPosition(messageList.size()-1);
    }

    public void scrollListener(){
        ChatActivity rootActivity = this;

        mRecyclerView.addOnScrollListener(scrollListener = new RecyclerView.OnScrollListener() {
            int  currentScrollPosition= 0;
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                currentScrollPosition += dy;
                int firstId = mMessageAdapter.mMessageList.get(0).getId();



                if (dy < 0) {
                    if (!mRecyclerView.canScrollVertically(-1)){
                        String new_url = url + "/more/messages/" + chatName +"/" + firstId;
                        URL obj = null;
                        try {
                            obj = new URL(new_url);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                        GetMoreMessages getMoreMessagesTask = new GetMoreMessages(findViewById(R.id.consLayout), rootActivity, username, url, scrollLoading);
                        getMoreMessagesTask.execute(obj);
                        scrollLoading.setVisibility(View.VISIBLE);

                    }
                    else{
                        scrollLoading.setVisibility(View.GONE);
                    }
                }
                else{
                    scrollLoading.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(a);
        this.webSocketClient.close(1000, "close");
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
            else
            {
                this.requestPermission();
            }
        }

    }

    private void requestPermission() {

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            photoView.setImageBitmap(photo);
            this.photoTaken = true;
            findViewById(R.id.edit_gchat_message).setVisibility(View.GONE);
            this.photoBitmap = photo;
        }

        else if (requestCode == POLL_REQUEST && resultCode == Activity.RESULT_OK) {
            String returnedResult = data.getData().toString();
            String[] poll = returnedResult.split(";");

            String toUrl = poll[0] + ":0," + poll[1] + ":0," + poll[2] + ":0," + poll[3] + ":0," + poll[4] + ":0";

            URL obj = null;
            String new_url = url + "/send/poll/" + username + "/" + chatName + "/" + toUrl;
            try {
                obj = new URL(new_url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }


            int aux_id= 0;
            List<BasicMessage> mList = mMessageAdapter.mMessageList;
            if (mMessageAdapter.mMessageList.size()>0)
                aux_id = mList.get(mList.size()-1).getId()+1;
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
            LocalDateTime lt = LocalDateTime.now();
            LocalDateTime time = LocalDateTime.parse(dtf.format(lt), dtf);

            List<Pair<String, Integer>> pollValues= new ArrayList<>();
            for (String value: poll ) {
                Pair<String, Integer> pollValue = new Pair<>(value, 0);
                pollValues.add(pollValue);
            }
            BasicMessage new_message = new BasicMessage(username, pollValues, time, aux_id);

            this.updateMessageAdapter(new_message, textMessageBox);

            SendTextTask sendTextTask = new SendTextTask();
            sendTextTask.execute(obj);

        }
        else if (requestCode == LOCATION_REQUEST && resultCode == Activity.RESULT_OK) {

            String returnedResult = data.getData().toString();
            String[] coors = returnedResult.split(",");
            LatLng coordinates = new LatLng(Double.parseDouble(coors[0]), Double.parseDouble(coors[1]));

            int aux_id= 0;
            List<BasicMessage> mList = mMessageAdapter.mMessageList;
            if (mMessageAdapter.mMessageList.size()>0)
                aux_id = mList.get(mList.size()-1).getId()+1;
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
            LocalDateTime lt = LocalDateTime.now();
            LocalDateTime time = LocalDateTime.parse(dtf.format(lt), dtf);

            URL obj = null;
            String new_url = url + "/send/location/" + username + "/" + chatName + "/" + returnedResult;
            try {
                obj = new URL(new_url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            SendTextTask sendTextTask = new SendTextTask();
            sendTextTask.execute(obj);
            BasicMessage new_message = new BasicMessage(username, coordinates, time, aux_id);

            this.updateMessageAdapter(new_message, textMessageBox);

            Toast toast=Toast.makeText(getApplicationContext(),returnedResult,Toast.LENGTH_SHORT);
            toast.setMargin(50,50);
            toast.show();


        }
    else{
            Toast toast=Toast.makeText(getApplicationContext(),"Noopes",Toast.LENGTH_SHORT);
            toast.setMargin(50,50);
            toast.show();
        }
    }

    public void setMessageAdapter(MessageListAdapter mMessageAdapter) {
        this.mMessageAdapter = mMessageAdapter;
    }
    public void setMessageList(List<BasicMessage> messageList) {
        Conversationalist appState = ((Conversationalist)this.getApplication());
        appState.addMessagesToCache(this.chatName, messageList);
        this.messageList = messageList;
    }
    public void setRecycler(RecyclerView recycler) {
        this.recycler = recycler;
    }
    public void updateMessageAdapter(BasicMessage new_message, EditText textMessageBox){
        this.messageList.add(new_message);
        this.mMessageAdapter.notifyDataSetChanged();
        this.recycler.smoothScrollToPosition(this.messageList.size()-1);
        textMessageBox.getText().clear();
    }

    public void leaveChat(){
        String new_url = url + "/leave_chat/" + chatName + "/" + username;
        URL obj = null;
        try {
            obj = new URL(new_url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        LeaveChatTask leaveChatTask = new LeaveChatTask(this);
        leaveChatTask.execute(obj);

    }

    public void messageReceived(String username, String chatName) {

        if (geoFlag == 1) {
            getCurrentLocation();
            float[] distance = new float[2];
            Location.distanceBetween(userLatitude, userLongitude, latitude, longitude, distance);
            Log.d("ingeochat", "" + userLatitude + " " + userLongitude);
            if(distance[0] > radius) {
                Intent i = new Intent(this, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            }
        }



            if (!Objects.equals(this.username, username)) {

                int mID = 0;
                if (mMessageAdapter.mMessageList != null && mMessageAdapter.mMessageList.size() > 0) {
                    mID = mMessageAdapter.mMessageList.get(mMessageAdapter.mMessageList.size() - 1).getId();
                }
                String new_url = url + "/messages/realtime/" + this.username + "/" + chatName + "/" + mID;
                Log.d("Webmes", new_url);
                URL obj = null;
                try {
                    obj = new URL(new_url);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                Log.d("Webmes", "ola");
                MessageUpdate messageUpdate = new MessageUpdate(findViewById(R.id.consLayout), this, this.username, url);
                messageUpdate.execute(obj);
            }
    }

    @SuppressLint("MissingPermission")
    public void getCurrentLocation(){
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    userLatitude = location.getLatitude();
                    userLongitude = location.getLongitude();
                }
            }
        });
    }

    public static class MessageListAdapter extends RecyclerView.Adapter {
        private static final int VIEW_TYPE_MESSAGE_SENT = 1;
        private static final int VIEW_TYPE_IMAGE_SENT = 3;
        private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;
        private static final int VIEW_TYPE_IMAGE_RECEIVED = 4;
        private static final int VIEW_TYPE_LOCATION_SENT = 5;
        private static final int VIEW_TYPE_LOCATION_RECEIVED = 6;
        private static final int VIEW_TYPE_POLL_SENT = 7;
        private static final int VIEW_TYPE_POLL_RECEIVED = 8;
        private final MessageListAdapter adapter;
        private final ChatActivity rootActivity;
        private Object url;
        private Drawable placeholder;

        private Context mContext;
        private List<BasicMessage> mMessageList;
        private String username;

        public MessageListAdapter(Context context, List<BasicMessage> messageList, String username, Drawable placeholder, String url, ChatActivity activity) {
            mContext = context;
            mMessageList = messageList;
            this.username = username;
            this.placeholder = placeholder;
            this.url= url;
            this.adapter = this;
            this.rootActivity = activity;

        }

        public void insertDownloadedImage(int id, Bitmap bitmap){
            for (int i=0; i < this.mMessageList.size(); i++){
                BasicMessage bm = this.mMessageList.get(i);
                if (bm.getId() == id){
                    bm.setImage(bitmap);
                }
            }
            this.rootActivity.setMessageList(this.mMessageList);
        }

        @Override
        public int getItemCount() {
            return mMessageList.size();
        }

        // Determines the appropriate ViewType according to the sender of the message.
        @Override
        public int getItemViewType(int position) {
            BasicMessage message = (BasicMessage) mMessageList.get(position);

            if (message.getSender().equals(this.username)) {
                // If the current user is the sender of the message
                if (message.isImage()){
                    return VIEW_TYPE_IMAGE_SENT;
                }
                else if (message.isMap()){
                    return  VIEW_TYPE_LOCATION_SENT;
                }
                else if (message.isPoll()) {
                    return VIEW_TYPE_POLL_SENT;
                }

                return VIEW_TYPE_MESSAGE_SENT;
            } else {
                if (message.isImage()){
                    return VIEW_TYPE_IMAGE_RECEIVED;
                }
                else if (message.isMap()){
                    return  VIEW_TYPE_LOCATION_RECEIVED;
                }
                else if (message.isPoll()) {
                    return VIEW_TYPE_POLL_RECEIVED;
                }
                // If some other user sent the message
                return VIEW_TYPE_MESSAGE_RECEIVED;
            }
        }

        // Inflates the appropriate layout according to the ViewType.
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;

            if (viewType == VIEW_TYPE_MESSAGE_SENT) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.me_perspective_layout, parent, false);
                return new SentMessageHolder(view, this.placeholder);
            } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.other_perspective_layout, parent, false);
                return new ReceivedMessageHolder(view, this.placeholder);
            } else if (viewType == VIEW_TYPE_IMAGE_SENT) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.me_perpective_image_layout, parent, false);
                return new SentImageHolder(view, this.placeholder);
            } else if (viewType == VIEW_TYPE_IMAGE_RECEIVED) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.other_perspective_image_layout, parent, false);
                return new ReceivedImageHolder(view, this.placeholder);
            } else if (viewType == VIEW_TYPE_LOCATION_SENT) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.me_map_perspective_layout, parent, false);
                return new SentMapHolder(view);
            } else if (viewType == VIEW_TYPE_LOCATION_RECEIVED) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.other_perspective_map_layout, parent, false);
                return new ReceivedMapHolder(view);
            } else if (viewType == VIEW_TYPE_POLL_SENT) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.me_perspective_poll_layout, parent, false);
                return new SentPollHolder(view);
            } else if (viewType == VIEW_TYPE_POLL_RECEIVED) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.other_perspective_poll_layout, parent, false);
                return new ReceivedPollHolder(view);
            }
            return null;
        }

        // Passes the message object to a ViewHolder so that the contents can be bound to UI.
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

            BasicMessage message = (BasicMessage) mMessageList.get(position);
            Log.d("posix", ""+ message.isImage());
            switch (holder.getItemViewType()) {
                case VIEW_TYPE_MESSAGE_SENT:
                    ((SentMessageHolder) holder).bind(message);
                    break;
                case VIEW_TYPE_MESSAGE_RECEIVED:
                    ((ReceivedMessageHolder) holder).bind(message);
                    break;
                case VIEW_TYPE_IMAGE_SENT:
                    ((SentImageHolder) holder).bind(message);
                    break;
                case VIEW_TYPE_IMAGE_RECEIVED:
                    ((ReceivedImageHolder) holder).bind(message);
                    break;
                case VIEW_TYPE_LOCATION_SENT:
                    ((SentMapHolder) holder).bind(message);
                    break;
                case VIEW_TYPE_LOCATION_RECEIVED:
                    ((ReceivedMapHolder) holder).bind(message);
                    break;
                case VIEW_TYPE_POLL_SENT:
                    ((SentPollHolder) holder).bind(message);
                    break;
                case VIEW_TYPE_POLL_RECEIVED:
                    ((ReceivedPollHolder) holder).bind(message);
            }

            if (message.isImage()){
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ImageView imageView = holder.itemView.findViewById(R.id.text_image);
                        TextView textView = holder.itemView.findViewById(R.id.text_gchat_message);
                        if (imageView.getDrawable() == placeholder){
                            int id = message.getId();
                            String new_url = url + "/get_image/" + id;
                            URL obj = null;
                            try {
                                obj = new URL(new_url);
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            }
                            GetImageTask getImageTask = new GetImageTask(id, imageView, textView, adapter);
                            getImageTask.execute(obj);
                        }
                        else{
                            Bitmap bm=((BitmapDrawable)imageView.getDrawable()).getBitmap();
                            FragmentTransaction ft = rootActivity.getSupportFragmentManager().beginTransaction();
                            ft.replace(R.id.popImage, new ChatActivity.PopupImage(rootActivity, bm));
                            ft.addToBackStack(null);
                            ft.commit();
                        }
                    }
                });
            }
        }

        private class SentMessageHolder extends RecyclerView.ViewHolder {
            TextView messageText, timeText;

            SentMessageHolder(View itemView, Drawable placeholder) {
                super(itemView);
                messageText = (TextView) itemView.findViewById(R.id.text_gchat_message);
                timeText = (TextView) itemView.findViewById(R.id.text_gchat_timestamp_me);
            }

            void bind(BasicMessage message) {
                String messageContent = URLDecoder.decode(message.getContent());
                messageText.setText(messageContent);
                // Format the stored timestamp into a readable String using method.
                timeText.setText(message.getTime().toString().replace("T", " "));
            }
        }
        private class SentImageHolder extends RecyclerView.ViewHolder {
            private final Drawable placeHolder;
            TextView messageText, timeText;
            ImageView imageText;

            SentImageHolder(View itemView, Drawable placeholder) {
                super(itemView);
                this.placeHolder = placeholder;
                messageText = (TextView) itemView.findViewById(R.id.text_gchat_message);
                timeText = (TextView) itemView.findViewById(R.id.text_gchat_timestamp_me);
                imageText = (ImageView) itemView.findViewById(R.id.text_image);
            }

            void bind(BasicMessage message) {
                Bitmap image = message.getImage();
                if (image != null){
                    imageText.setImageBitmap(image);
                    messageText.setVisibility(View.GONE);
                }
                else{
                    imageText.setImageDrawable(this.placeHolder);
                    messageText.setVisibility(View.VISIBLE);
                }
                // Format the stored timestamp into a readable String using method.
                timeText.setText(message.getTime().toString().replace("T", " "));
            }
        }


        private class SentMapHolder extends RecyclerView.ViewHolder implements OnMapReadyCallback  {

            TextView  timeText;
            MapView mapText;
            GoogleMap map;
            LatLng coordinates;

            SentMapHolder(View itemView) {
                super(itemView);
                timeText = (TextView) itemView.findViewById(R.id.text_gchat_timestamp_me);
                mapText = (MapView) itemView.findViewById(R.id.mapText);

                if (mapText != null)
                {
                    mapText.onCreate(null);
                    mapText.onResume();
                    mapText.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(@NonNull GoogleMap googleMap) {
                            map = googleMap;
                            map.addMarker(new MarkerOptions().position(coordinates).title("Location"));
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates, 16.0f));
                        }
                    });

                    MapsInitializer.initialize((Context) rootActivity);
                }
            }

            void bind(BasicMessage message) {
                //this.map.clear();
                coordinates = message.getCoordinates();

                // Format the stored timestamp into a readable String using method.
                timeText.setText(message.getTime().toString().replace("T", " "));
            }


            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                this.map = googleMap;
                this.map.addMarker(new MarkerOptions().position(coordinates).title("Geofence Location"));
                this.map.moveCamera(CameraUpdateFactory.newLatLng(coordinates));
            }
        }

        private class SentPollHolder extends RecyclerView.ViewHolder {

            TextView timeText, pollTitle, opt1, opt2, opt3, opt4, votes1, votes2, votes3, votes4;

            SentPollHolder(View itemView) {
                super(itemView);
                timeText = (TextView) itemView.findViewById(R.id.text_gchat_timestamp_me);
                pollTitle = (TextView) itemView.findViewById(R.id.pollQuest);
                opt1 = (TextView) itemView.findViewById(R.id.Opt1Txt);
                opt2 = (TextView) itemView.findViewById(R.id.Opt2Txt);
                opt3 = (TextView) itemView.findViewById(R.id.Opt3Txt);
                opt4 = (TextView) itemView.findViewById(R.id.Opt4Txt);
                votes1 = (TextView) itemView.findViewById(R.id.Opt1Votes);
                votes2 = (TextView) itemView.findViewById(R.id.Opt2Votes);
                votes3 = (TextView) itemView.findViewById(R.id.Opt3Votes);
                votes4 = (TextView) itemView.findViewById(R.id.Opt4Votes);
            }

            void bind(BasicMessage message) {
                List<Pair<String, Integer>> poll = message.getPoll();

                pollTitle.setText(""+poll.get(0).first);
                opt1.setText(String.format("%s", poll.get(1).first));
                votes1.setText(""+ poll.get(1).second);
                opt2.setText(String.format("%s", poll.get(2).first));
                votes2.setText(""+ poll.get(2).second);
                opt3.setText(String.format("%s", poll.get(3).first));
                votes3.setText(""+ poll.get(3).second);
                opt4.setText(String.format("%s", poll.get(4).first));
                votes4.setText(""+ poll.get(4).second);

                timeText.setText(message.getTime().toString().replace("T", " "));
            }
        }


        private class ReceivedMessageHolder extends RecyclerView.ViewHolder {
            TextView messageText, timeText, nameText;

            ReceivedMessageHolder(View itemView, Drawable placeholder) {
                super(itemView);

                messageText = (TextView) itemView.findViewById(R.id.text_gchat_message);
                timeText = (TextView) itemView.findViewById(R.id.text_gchat_timestamp_other);
                nameText = (TextView) itemView.findViewById(R.id.text_gchat_user_other);
            }

            void bind(BasicMessage message) {
                String messageContent = URLDecoder.decode(message.getContent());
                messageText.setText(messageContent);
                // Format the stored timestamp into a readable String using method.
                timeText.setText(message.getTime().toString().replace("T", " "));
                nameText.setText(message.getSender());
            }
        }

        private class ReceivedImageHolder extends RecyclerView.ViewHolder {
            private final Drawable placeHolder;
            TextView messageText, timeText, nameText;
            ImageView imageText;

            ReceivedImageHolder(View itemView, Drawable placeholder) {
                super(itemView);
                this.placeHolder = placeholder;
                messageText = (TextView) itemView.findViewById(R.id.text_gchat_message);
                timeText = (TextView) itemView.findViewById(R.id.text_gchat_timestamp_other);
                imageText = (ImageView) itemView.findViewById(R.id.text_image);
                nameText = (TextView) itemView.findViewById(R.id.text_gchat_user_other);
            }

            void bind(BasicMessage message) {
                Bitmap image = message.getImage();
                if (image != null){
                    imageText.setImageBitmap(image);
                    messageText.setVisibility(View.GONE);
                }
                else{
                    imageText.setImageDrawable(this.placeHolder);
                    messageText.setVisibility(View.VISIBLE);
                }
                // Format the stored timestamp into a readable String using method.
                timeText.setText(message.getTime().toString().replace("T", " "));
                nameText.setText(message.getSender());
            }
        }

        private class ReceivedMapHolder extends RecyclerView.ViewHolder implements OnMapReadyCallback  {

            TextView timeText, nameText;
            MapView mapText;
            GoogleMap map;
            LatLng coordinates;

            ReceivedMapHolder(View itemView) {
                super(itemView);
                timeText = (TextView) itemView.findViewById(R.id.text_gchat_timestamp_other);
                mapText = (MapView) itemView.findViewById(R.id.mapText);
                nameText = (TextView) itemView.findViewById(R.id.text_gchat_user_other);

                if (mapText != null)
                {
                    mapText.onCreate(null);
                    mapText.onResume();
                    mapText.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(@NonNull GoogleMap googleMap) {
                            map = googleMap;
                            map.addMarker(new MarkerOptions().position(coordinates).title("Location"));
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates, 16.0f));
                        }
                    });

                    MapsInitializer.initialize((Context) rootActivity);
                }
            }

            void bind(BasicMessage message) {

                coordinates = message.getCoordinates();
                nameText.setText(message.getSender());
                // Format the stored timestamp into a readable String using method.
                timeText.setText(message.getTime().toString().replace("T", " "));
            }


            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                this.map = googleMap;
                this.map.addMarker(new MarkerOptions().position(coordinates).title("Geofence Location"));
                this.map.moveCamera(CameraUpdateFactory.newLatLng(coordinates));
            }
        }

        private class ReceivedPollHolder extends RecyclerView.ViewHolder {
            TextView nameText, timeText, pollTitle, opt1, opt2, opt3, opt4, votes1, votes2, votes3, votes4;

            ReceivedPollHolder(View itemView) {
                super(itemView);
                nameText = (TextView) itemView.findViewById(R.id.text_gchat_user_other);
                timeText = (TextView) itemView.findViewById(R.id.text_gchat_timestamp_other);

                pollTitle = (TextView) itemView.findViewById(R.id.pollQuest);
                opt1 = (TextView) itemView.findViewById(R.id.Opt1Txt);
                opt2 = (TextView) itemView.findViewById(R.id.Opt2Txt);
                opt3 = (TextView) itemView.findViewById(R.id.Opt3Txt);
                opt4 = (TextView) itemView.findViewById(R.id.Opt4Txt);
                votes1 = (TextView) itemView.findViewById(R.id.Opt1Votes);
                votes2 = (TextView) itemView.findViewById(R.id.Opt2Votes);
                votes3 = (TextView) itemView.findViewById(R.id.Opt3Votes);
                votes4 = (TextView) itemView.findViewById(R.id.Opt4Votes);
            }

            void bind(BasicMessage message) {
                List<Pair<String, Integer>> poll = message.getPoll();

                pollTitle.setText(""+poll.get(0).first);
                opt1.setText(String.format("%s", poll.get(1).first));
                votes1.setText(""+ poll.get(1).second);
                opt2.setText(String.format("%s", poll.get(2).first));
                votes2.setText(""+ poll.get(2).second);
                opt3.setText(String.format("%s", poll.get(3).first));
                votes3.setText(""+ poll.get(3).second);
                opt4.setText(String.format("%s", poll.get(4).first));
                votes4.setText(""+ poll.get(4).second);
                nameText.setText(message.getSender());
                timeText.setText(message.getTime().toString().replace("T", " "));

            }
        }

    }


    public static class PopupImage extends Fragment implements View.OnClickListener {


        private final Bitmap bm;
        private final ChatActivity mainActivity;
        private View myView;

        public PopupImage(ChatActivity mainActivity, Bitmap bm) {
            this.bm = bm;
            this.mainActivity = mainActivity;
        }


        @NonNull
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                                 @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            myView = inflater.inflate(R.layout.pop_image_layout, container, false);
            ImageView image = myView.findViewById(R.id.bigImage);
            ImageView backButton = myView.findViewById(R.id.closeBtn);
            image.setImageBitmap(bm);

            backButton.setOnClickListener(new View.OnClickListener(){
                public void onClick(View view){
                    mainActivity.finish();
                }
            });

            return myView;
        }

        @Override
        public void onClick(View view) {

        }
    }

    public static class LeaveFragment extends Fragment {
        private final ChatActivity mainActivity;
        private View myView;

        public LeaveFragment(ChatActivity mainActivity) {
            this.mainActivity = mainActivity;
        }

        @NonNull
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                                 @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            myView = inflater.inflate(R.layout.leave_chat_layout, container, false);

            Button yesButton = (Button) this.myView.findViewById(R.id.yes_button);
            Button cancelButton = (Button) this.myView.findViewById(R.id.cancel_button);


            yesButton.setOnClickListener(new View.OnClickListener(){
                public void onClick(View view){
                    mainActivity.leaveChat();
                }
            });

            cancelButton.setOnClickListener(new View.OnClickListener(){
                public void onClick(View view){
                    mainActivity.onBackPressed();
                }
            });
            return myView;
        }

    }
}


class GetMessages extends AsyncTask<URL, Integer, BasicMessageList> {


    private final View rootview;
    private final ChatActivity rootAcitvity;
    private final String username;
    private final String url;
    private URL url_obj = null;


    public GetMessages(View rootview, ChatActivity rootActivity, String username, String url) {
        this.rootview = rootview;
        this.rootAcitvity = rootActivity;
        this.username = username;
        this.url = url;
    }


    @Override
    protected BasicMessageList doInBackground(URL... urls) {
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

        this.rootAcitvity.setMessageList(messageList);

        Drawable placeholder =  this.rootAcitvity.getResources().getDrawable(android.R.drawable.ic_menu_gallery);

        RecyclerView mMessageRecycler = (RecyclerView) this.rootview.findViewById(R.id.recycler_gchat);
        ChatActivity.MessageListAdapter mMessageAdapter = new ChatActivity.MessageListAdapter(this.rootAcitvity, this.rootAcitvity.messageList, this.username, placeholder, this.url, this.rootAcitvity);
        mMessageRecycler.setLayoutManager(new LinearLayoutManager(this.rootAcitvity));
        mMessageRecycler.setAdapter(mMessageAdapter);

        this.rootAcitvity.setMessageAdapter(mMessageAdapter);
        this.rootAcitvity.setRecycler(mMessageRecycler);


        if (messageList.size() > 0)
            mMessageRecycler.smoothScrollToPosition(messageList.size()-1);

    }

    public void setUrl_obj(URL url_obj) {
        this.url_obj = url_obj;
    }

    public URL getUrl_obj() {
        return url_obj;
    }
}

class SendTextTask extends AsyncTask<URL, Integer, JsonReader> {

    public SendTextTask() {
    }

    @Override
    protected JsonReader doInBackground(URL... urls) {
        URL obj = urls[0];

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

        return null;
    }
}


class LeaveChatTask extends AsyncTask<URL, Integer, JsonReader> {

    private final ChatActivity activity;

    public LeaveChatTask(ChatActivity activity) {
        this.activity = activity;
    }

    @Override
    protected JsonReader doInBackground(URL... urls) {
        URL obj = urls[0];

        HttpURLConnection con = null;
        int responseCode = 0;
        try {
            con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "conversationalIST");
            responseCode = con.getResponseCode();

        } catch (Exception e) {
            Log.d("Response_Code_leave", "" + responseCode);
            Log.d("Exception_log_task", "" + e);
        }

        return null;
    }

    @Override
    protected void onPostExecute(JsonReader jsonReader) {
        super.onPostExecute(jsonReader);

        String username = this.activity.username;
        Conversationalist appState = ((Conversationalist)this.activity.getApplication());
        List<Map<String, String>> cList = appState.getChatsFromCache(username);
        int index=0;
        for (int i=0; i< cList.size(); i++){
            if (cList.get(i).get("name").equals(activity.chatName)){
                index = i;
                Log.d("cList", ""+ cList.get(index));
            }
        }
        cList.remove(index);

        appState.addChatsToCache(username, cList);

        Intent intent = new Intent(this.activity, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        this.activity.startActivity(intent);
    }
}


class SendImageTask extends AsyncTask<URL, Integer, JsonReader> {

    private final Bitmap imageBitmap;
    public SendImageTask(Bitmap photoBitmap) {
        this.imageBitmap = photoBitmap;

    }

    @Override
    protected JsonReader doInBackground(URL... urls) {
        URL obj = urls[0];

        HttpURLConnection con = null;
        int responseCode = 0;
        try {
            con = (HttpURLConnection) obj.openConnection();
            con.setRequestProperty("User-Agent", "conversationalIST");
            con.setRequestProperty("Content-Type", "image/jpeg"); // here you are setting the `Content-Type` for the data you are sending which is `application/json`
            con.setRequestMethod("POST");
            con.setDoOutput(true);


            DataOutputStream os = new DataOutputStream(con.getOutputStream());
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            this.imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            os.write(byteArray, 0, byteArray.length);

            os.flush();
            os.close();
            responseCode = con.getResponseCode();
            con.disconnect();


        } catch (Exception e) {
            Log.d("Response_Code", "" + responseCode);
            Log.d("Exception_log_task", "" + e);
        }

        return null;
    }
}


class GetImageTask extends AsyncTask<URL, Integer, Bitmap> {


    private final int id;
    private final ImageView imageView;
    private final TextView textView;
    private ChatActivity.MessageListAdapter mMessageAdapter;

    public GetImageTask(int id, ImageView imageView, TextView textView, ChatActivity.MessageListAdapter adapter) {
        this.id = id;
        this.imageView = imageView;
        this.textView = textView;
        this.mMessageAdapter = adapter;
    }

    @Override
    protected Bitmap doInBackground(URL... urls) {
        URL obj = urls[0];

        HttpURLConnection con = null;
        int responseCode = 0;
        try {
            con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
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
            responseBodyReader =
                    new InputStreamReader(responseBody, "UTF-8");
        } catch (Exception e) {
            Log.d("Exception_log_task", "" + e);
        }

        JsonReader jsonReader = new JsonReader(responseBodyReader);
        Bitmap decodedByte = null;
        try {
            jsonReader.beginObject();
            jsonReader.nextName();
            String encodedImage = jsonReader.nextString();
            byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
            decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

        } catch (IOException e) {
            e.printStackTrace();
        }


        return decodedByte;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);

        this.imageView.setImageBitmap(bitmap);
        this.textView.setVisibility(View.GONE);
        this.mMessageAdapter.insertDownloadedImage(this.id, bitmap);
    }
}



class GetMoreMessages extends AsyncTask<URL, Integer, BasicMessageList> {


    private final View rootview;
    private final ChatActivity rootAcitvity;
    private final String username;
    private final String url;
    private final View scrollLoading;
    private URL url_obj = null;


    public GetMoreMessages(View rootview, ChatActivity rootActivity, String username, String url, View scrollLoading) {
        this.rootview = rootview;
        this.rootAcitvity = rootActivity;
        this.username = username;
        this.url = url;
        this.scrollLoading = scrollLoading;
    }

    @Override
    protected BasicMessageList doInBackground(URL... urls) {
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
                    }
                    if(name.equals("content")) {
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
        int index = 0;
        if (messageList.size() > 0)
            index = messageList.size()-1;


        for (int i=messageList.size()-1; i>=0; i--){
            this.rootAcitvity.messageList.add(0, messageList.get(i));
            this.rootAcitvity.mMessageAdapter.notifyItemInserted(0) ;
        }

        this.rootAcitvity.setMessageList(this.rootAcitvity.messageList);

        this.rootAcitvity.recycler.smoothScrollToPosition(index);

        this.scrollLoading.setVisibility(View.GONE);
    }

    public void setUrl_obj(URL url_obj) {
        this.url_obj = url_obj;
    }

    public URL getUrl_obj() {
        return url_obj;
    }
}

class MessageUpdate extends AsyncTask<URL, Integer, BasicMessageList> {


    private final View rootview;
    private final ChatActivity rootAcitvity;
    private final String username;
    private final String url;
    private URL url_obj = null;


    public MessageUpdate(View rootview, ChatActivity rootActivity, String username, String url) {
        this.rootview = rootview;
        this.rootAcitvity = rootActivity;
        this.username = username;
        this.url = url;
    }

    @Override
    protected BasicMessageList doInBackground(URL... urls) {
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
                        is_map = jsonReader.nextInt();
                    }
                    if(name.equals("none")) {
                        return null;
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
                int same_user = 0;
                jsonReader.beginObject();
                while (jsonReader.hasNext()){
                    String name = jsonReader.nextName();
                    if(name.equals("sender")) {
                        sender = jsonReader.nextString();
                        if (sender.equals(username)) {
                            same_user = 1;
                        }
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
                if(same_user == 0) {
                    contentList.add(message);
                    sendersList.add(sender);
                    datesList.add(date);
                    idsList.add(id);
                }
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

        if(messageListO == null) {
            Log.d("cml" , "0");
            return;
        }

        Log.d("cml", "1");


        List<BasicMessage> messageList = messageListO.getMessageList();


        for (int i=0; i<messageList.size(); i++){
            this.rootAcitvity.messageList.add(messageList.get(i));
            this.rootAcitvity.mMessageAdapter.notifyItemInserted(this.rootAcitvity.messageList.size()-1) ;
        }

        this.rootAcitvity.recycler.smoothScrollToPosition(this.rootAcitvity.messageList.size()-1);

    }

    public void setUrl_obj(URL url_obj) {
        this.url_obj = url_obj;
    }

    public URL getUrl_obj() {
        return url_obj;
    }
}

class ChatActivityWebSocketClient extends WebSocketClient {
    private  java.net.URI URI;
    private final ChatActivity activity;

    public ChatActivityWebSocketClient(URI serverURI, ChatActivity chatActivity) {
        super(serverURI);
        this.URI = serverURI;
        this.activity = chatActivity;
    }

    @Override
    public void onOpen(ServerHandshake handShakeData) {
        Log.d("Websocket", "Opened chat");
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
        ChatActivityWebSocketClient aux = new ChatActivityWebSocketClient(this.URI, this.activity);
        this.activity.webSocketClient = aux;
        aux.connect();
    }

    @Override
    public void onError(Exception ex) {
        Log.d("Websocket", "Error " + ex.getMessage());
    }
}



