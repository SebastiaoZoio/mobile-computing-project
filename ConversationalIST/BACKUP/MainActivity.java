package pt.ulisboa.tecnico.cmov.conversationalist;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {


    String url = "http://10.0.2.2:5000";

    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        this.settings = getSharedPreferences("mySharedPref", 0);
        String user_name = settings.getString("user_name", null);

        Button newChatButton = (Button) findViewById(R.id.extended_fab);

        if (user_name == null){
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
        else{
            Log.d("tagame", user_name);
            String new_url = url + "/king/convos";
            Log.d("username", new_url);
            URL obj = null;
            {
                try {
                    obj = new URL(new_url);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }

            GetChats getChatsTask = new GetChats(findViewById(R.id.mobile_list), this);
            getChatsTask.execute(obj);
        }


        newChatButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment, new NewChatFragment());
                ft.addToBackStack(null);
                ft.commit();
            }
        });


    }

    public static class NewChatFragment extends Fragment {
        public static Fragment newInstance() {
            return new NewChatFragment();
        }
        @NonNull
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                                 @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.new_chat_layout, container, false);
        }
    }

}

class GetChats extends AsyncTask<URL, Integer, JsonReader> {

    private final ListView rootView;
    private final MainActivity mainActivity;
    private URL url_obj = null;



    public GetChats(View rootView, MainActivity mainActivity) {
        this.rootView = (ListView) rootView;
        this.mainActivity = mainActivity;
    }


    @Override
    protected JsonReader doInBackground(URL... urls) {
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

        return jsonReader;
    }

    @Override
    protected void onPostExecute(JsonReader jsonReader){
        List<String> chatList = new ArrayList<String>();
        String chatName = null;
        String chatType;

        try {
            jsonReader.beginArray();
            while(jsonReader.hasNext()) {
                jsonReader.beginObject();
                while (jsonReader.hasNext()){
                    String name = jsonReader.nextName();
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
                }
                jsonReader.endObject();
                chatList.add(chatName);
            }
            jsonReader.endArray();

        } catch (IOException e) {
            e.printStackTrace();
        }

        ArrayAdapter adapter = new ArrayAdapter<String>(this.mainActivity,
                R.layout.activity_listview, chatList);

        this.rootView.setAdapter(adapter);

    }

    public void setUrl_obj(URL url_obj) {
        this.url_obj = url_obj;
    }

    public URL getUrl_obj() {
        return url_obj;
    }
}

