package pt.ulisboa.tecnico.cmov.conversationalist;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.snackbar.Snackbar;

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

import static pt.ulisboa.tecnico.cmov.conversationalist.MainActivity.username;

public class SearchChatActivity extends AppCompatActivity {

    String url;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 200;
    private EditText filterText = null;
    SimpleAdapter adapter = null;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        url = MainActivity.url;
        super.onCreate(savedInstanceState);

        setContentView(R.layout.search_chat_layout);

        listView = findViewById(R.id.find_chat_list);

        String new_url = url + "/convos/" + username;
        URL obj = null;
        {
            try {
                obj = new URL(new_url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        GetPublicChatTask getPublicChatTask = new GetPublicChatTask(listView, this);
        getPublicChatTask.execute(obj);

        filterText = (EditText) findViewById(R.id.text_search_box);
        filterText.addTextChangedListener(filterTextWatcher);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String chatname = ((TextView)(view.findViewById(android.R.id.text1))).getText().toString();
                String new_url = url + "/convos/" + chatname + "/" + username;
                URL obj = null;
                {
                    try {
                        obj = new URL(new_url);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
                JoinChatTask joinChatTask = new JoinChatTask(getThis());
                joinChatTask.execute(obj);
                Intent myIntent = new Intent(getThis(), MainActivity.class);
                myIntent.putExtra("join", 1);
                myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(myIntent);
            }

        });

    }

    private SearchChatActivity getThis() {
        return this;
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
        ActivityCompat.requestPermissions(SearchChatActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
        boolean shouldProvideRationale2 =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION);
        ActivityCompat.requestPermissions(SearchChatActivity.this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
        boolean shouldProvideRationale3 =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        ActivityCompat.requestPermissions(SearchChatActivity.this,
                new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }


    private TextWatcher filterTextWatcher = new TextWatcher() {

        public void afterTextChanged(Editable s) {
        }

        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
            adapter.getFilter().filter(s);
        }

    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        filterText.removeTextChangedListener(filterTextWatcher);
    }
}



class GetPublicChatTask extends AsyncTask<URL, Integer, Boolean> {

    private final ListView rootView;
    private final SearchChatActivity mainActivity;
    private URL url_obj = null;
    List<Map<String, String>> chatList = new ArrayList<Map<String, String>>();



    public GetPublicChatTask(View rootView, SearchChatActivity mainActivity) {
        this.rootView = (ListView) rootView;
        this.mainActivity = mainActivity;
    }


    @Override
    protected Boolean doInBackground(URL... urls) {
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
        boolean empty = false;

        try {
            jsonReader.beginArray();
            while(jsonReader.hasNext()) {
                jsonReader.beginObject();
                while (jsonReader.hasNext()){
                    String name = jsonReader.nextName();
                    if (name.equals("no_chats")){
                        empty = true;
                        jsonReader.skipValue();
                        break;
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
                    else if (name.equals("latitude") || name.equals("longitude") || name.equals("read") || name.equals("radius") ){
                        jsonReader.skipValue();
                    }
                }
                if(empty){
                    break;
                }
                jsonReader.endObject();
                Map<String, String> datum = new HashMap<String, String>(2);
                datum.put("name", chatName);
                datum.put("type", chatType);
                chatList.add(datum);
            }
            if (!empty)
                jsonReader.endArray();

        } catch (IOException e) {
            e.printStackTrace();
        }



        return empty;
    }

    @Override
    protected void onPostExecute(Boolean empty) {
        super.onPostExecute(empty);

        if(!empty) {
            SimpleAdapter adapter = new SimpleAdapter(this.mainActivity, chatList, android.R.layout.simple_list_item_2
                    , new String[]{"name", "type"},
                    new int[]{android.R.id.text1,
                            android.R.id.text2});
            this.mainActivity.adapter = adapter;
            this.rootView.setAdapter(adapter);


            for (Map<String,String> chat: chatList){
                Log.d("permits", ""+ chat.get("type"));
                if (chat.get("type").equals("Geo-fenced")){
                    if (!this.mainActivity.checkPermissions()) {
                        this.mainActivity.requestPermissions();
                    }
                }
            }
        }
        else {
            EditText search;
            search = mainActivity.findViewById(R.id.text_search_box);
            search.setEnabled(false);
            search.setHint("There are no public chats!");
        }

    }

    public void setUrl_obj(URL url_obj) {
        this.url_obj = url_obj;
    }

    public URL getUrl_obj() {
        return url_obj;
    }

}

class JoinChatTask extends AsyncTask<URL, Integer, JsonReader> {

    private URL url_obj = null;
    private Activity ac;

    public JoinChatTask(Activity ac) {
        this.ac = ac;
    }

    @Override
    protected JsonReader doInBackground(URL... urls) {
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

        int success = -1;
        try {
            jsonReader.beginObject();
            while(jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                if(name.equals("success")) {
                    success = jsonReader.nextInt();
                    Log.d("response", "" +success);
                    if(success == 1) {
                        return null;
                    }
                    if(success == 2) {
                        Snackbar message_taken = Snackbar.make(ac.findViewById(R.id.chats), "You're already in this conversation!", Snackbar.LENGTH_SHORT );
                        message_taken.show();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return jsonReader;
    }

    @Override
    protected void onPostExecute(JsonReader jsonReader){



    }

    public void setUrl_obj(URL url_obj) {
        this.url_obj = url_obj;
    }

    public URL getUrl_obj() {
        return url_obj;
    }
}
