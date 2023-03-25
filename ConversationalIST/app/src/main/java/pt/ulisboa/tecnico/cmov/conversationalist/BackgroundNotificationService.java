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

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.NOTIFICATION_SERVICE;

public class BackgroundNotificationService extends Service {

    private Handler mHandler;
    Integer id = 1;
    // default interval for syncing data
    public static final long DEFAULT_SYNC_INTERVAL = 30 * 1000;

    BackgroundNotificationService getThis() {
        return this;
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
    // task to be run here
    private Runnable runnableService = new Runnable() {
        @Override
        public void run() {

            String new_url = MainActivity.url + "/"+ MainActivity.username+ "/notify";
            URL obj = null;
            {
                try {
                    obj = new URL(new_url);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }


            NotifyTask notifyTask = new NotifyTask(getThis());
            notifyTask.execute(obj);
            // Repeat this runnable code block again every ... min
            mHandler.postDelayed(runnableService, DEFAULT_SYNC_INTERVAL);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Create the Handler object
        Log.d("cervi", "here");
        mHandler = new Handler();
        // Execute a runnable task as soon as possible
        mHandler.post(runnableService);

        return START_STICKY;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

class NotifyTask extends AsyncTask<URL, Integer, JsonReader> {

    private BackgroundNotificationService bns;

    public NotifyTask(BackgroundNotificationService bns) {
        this.bns = bns;
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

        return jsonReader;
    }

    @Override
    protected void onPostExecute(JsonReader jsonReader) {
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
                        return;
                    }
                    if (name.equals("name")) {
                        chatName = jsonReader.nextString();
                    }
                    if(name.equals("type")) {
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
                NotificationManager.IMPORTANCE_LOW);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        NotificationManager manager = (NotificationManager) bns.getSystemService(NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        Log.d("notnum", ""+chatList.size());
        int i = 0;
        while (i < chatList.size()) {
            bns.id += 1;
            Intent intent = new Intent(bns, ChatActivity.class);
            intent.putExtra("chatname", chatList.get(i).get("name"));
            intent.putExtra("type", chatList.get(i).get("type"));
            intent.putExtra("url", MainActivity.url);
            intent.putExtra("username", MainActivity.username);
            PendingIntent pIntent = PendingIntent.getActivity(bns, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(bns, "MyChannelId");
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

            manager.notify(bns.id, notification);
            i+=1;
        }


    }
}
