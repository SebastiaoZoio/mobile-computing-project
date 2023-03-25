package pt.ulisboa.tecnico.cmov.conversationalist;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.util.JsonReader;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static android.content.Context.NOTIFICATION_SERVICE;
import static pt.ulisboa.tecnico.cmov.conversationalist.MainActivity.ws;

public class NotificationService extends Service {

    WebSocketClient nWebSocketClient;
    Integer id = 1;

    public NotificationService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        NotificationChannel chan = new NotificationChannel(
                "MyChannelId",
                "My Foreground Service",
                NotificationManager.IMPORTANCE_LOW);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_SECRET);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
                this, "MyChannelId");
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_baseline_person_add_24)
                .setContentTitle("App is running on foreground")
                .setPriority(NotificationManager.IMPORTANCE_LOW)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setChannelId("MyChannelId")
                .build();

        startForeground(1, notification);
    }

    public void messageReceived(String user, String chatName) {
        String new_url = MainActivity.url + "/"+ MainActivity.username+ "/notify";
        URL obj = null;
        {
            try {
                obj = new URL(new_url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        NotifTask notifyTask = new NotifTask(this);
        notifyTask.execute(obj);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Create the Handler object
        Log.d("cervi", "here");
        try {
            Log.d("notif", "popo");
            connectWebSocket();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return START_STICKY;
    }

    private void connectWebSocket() throws InterruptedException {
        URI uri = null;
        String wsUri = ws + "/echo/" + MainActivity.username + "Notify";
        Log.d("Websocket", wsUri);
        try {
            uri = new URI(wsUri);
        } catch (URISyntaxException e) {
            Log.d("WebsocketExc", ""+e);
        }
        nWebSocketClient = new NotifyWebSocketClient(uri, this);
        nWebSocketClient.connectBlocking();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy(){
        if (nWebSocketClient != null)  nWebSocketClient.close(1000, "close");
        super.onDestroy();
    }
}

class NotifyWebSocketClient extends WebSocketClient {
    private final java.net.URI URI ;
    private final NotificationService ns;

    public NotifyWebSocketClient(URI serverURI, NotificationService ns) {
        super(serverURI);
        this.URI = serverURI;
        this.ns = ns;
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
        this.ns.messageReceived(response[0], response[1]);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d("Websocket", "Closed " + reason);
        if (reason.equals("close")) return;
        this.ns.nWebSocketClient = new NotifyWebSocketClient(this.URI, this.ns);
        this.ns.nWebSocketClient.connect();
    }

    @Override
    public void onError(Exception ex) {
        Log.d("Websocket", "Error " + ex.getMessage());
    }
}

class NotifTask extends AsyncTask<URL, Integer, JsonReader> {

    private final NotificationService ns;

    public NotifTask(NotificationService ns) {
        this.ns = ns;
    }


    @Override
    protected JsonReader doInBackground(URL... urls) {
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

        List<Map<String, String>> chatList = new ArrayList<>();
        String chatName = null;
        String chatType = null;

        try {
            jsonReader.beginArray();
            while (jsonReader.hasNext()) {
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    String name = jsonReader.nextName();
                    if (name.equals("no_chats")) {
                        return null;
                    }
                    if (name.equals("name")) {
                        chatName = jsonReader.nextString();
                    }
                    if (name.equals("type")) {
                        chatType = jsonReader.nextString();
                    }
                }
                jsonReader.endObject();
                Map<String, String> datum = new HashMap<String, String>(2);
                datum.put("name", chatName);
                datum.put("type", chatType);
                chatList.add(datum);
            }
            jsonReader.endArray();

        } catch (IOException e) {
            e.printStackTrace();
        }

        NotificationChannel chan = new NotificationChannel(
                "MyChannelId",
                "My Foreground Service",
                NotificationManager.IMPORTANCE_DEFAULT);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        NotificationManager manager = (NotificationManager) ns.getSystemService(NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        Log.d("notnum", "" + chatList.size());
        int i = 0;
        while (i < chatList.size()) {
            ns.id += 1;
            Intent intent = new Intent(ns, ChatActivity.class);
            intent.putExtra("chatname", chatList.get(i).get("name"));
            intent.putExtra("ws", ws);
            intent.putExtra("type", chatList.get(i).get("type"));
            intent.putExtra("url", MainActivity.url);
            intent.putExtra("username", MainActivity.username);
            intent.putExtra("newMessages", 1);
            PendingIntent pIntent = PendingIntent.getActivity(ns, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(ns, "MyChannelId");
            Notification notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.drawable.ic_baseline_person_add_24)
                    .setContentTitle(chatList.get(i).get("name"))
                    .setContentText("New messages!")
                    .setPriority(NotificationManager.IMPORTANCE_LOW)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setContentIntent(pIntent)
                    .setAutoCancel(true)
                    .setChannelId("MyChannelId")
                    .build();

            manager.notify(ns.id, notification);
            i += 1;
        }




        return jsonReader;
    }

    @Override
    protected void onPostExecute(JsonReader jsonReader) {

    }
}


