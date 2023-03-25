package pt.ulisboa.tecnico.cmov.conversationalist;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {

    String url;

    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        url = MainActivity.url;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);

        this.sp = getSharedPreferences("mySharedPref",0);

        Button mButton = (Button) findViewById(R.id.log_in_button);
        EditText mEdit   = (EditText)findViewById(R.id.editTextTextPersonName);

        LoginActivity activity = this;
        mButton.setOnClickListener(new View.OnClickListener(){

            public void onClick(View view){
                String user_name = mEdit.getText().toString();

                String new_url = url + "/add_user/" + user_name;
                Log.d("username", new_url);
                URL obj = null;
                {
                    try {
                        obj = new URL(new_url);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
                LoginTask logTask = new LoginTask(findViewById(R.id.relativeLayout), sp, user_name, activity);
                logTask.execute(obj);
            }
        });

    }
}


class LoginTask extends AsyncTask<URL, Integer, Integer> {

    private final String user_name;
    private final LoginActivity login_activity;
    private URL url_obj = null;

    private View rootView;

    private SharedPreferences sharedP;

    public LoginTask(View rootView, SharedPreferences sp, String user_name, LoginActivity activity) {
        this.rootView = rootView;
        this.sharedP = sp;
        this.user_name = user_name;
        this.login_activity = activity;
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

        int response = -1;
        try {
            jsonReader.beginObject();
            while(jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                if(name.equals("user")) {
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
    protected void onPostExecute(Integer response){
        super.onPostExecute(response);
        if(response == 0) {
            Snackbar message_taken = Snackbar.make(rootView, "Username already taken", Snackbar.LENGTH_SHORT );
            message_taken.show();
        }
        else if (response == 2){
            CreateUserTask cuTask = new CreateUserTask(this.sharedP, this.user_name, this.login_activity);
            cuTask.execute(this.getUrl_obj());
        }
    }

    public void setUrl_obj(URL url_obj) {
        this.url_obj = url_obj;
    }

    public URL getUrl_obj() {
        return url_obj;
    }
}


class CreateUserTask extends AsyncTask<URL, Integer, Integer> {

    private final String user_name;
    private final LoginActivity login_acivity;
    private SharedPreferences sp;

    public CreateUserTask(SharedPreferences sharedP, String user_name, LoginActivity login_acivity) {
        this.sp = sharedP;
        this.user_name = user_name;
        this.login_acivity = login_acivity;
    }

    @Override
    protected Integer doInBackground(URL... urls) {
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
            Log.d("Rest_server4", "" + e);
        }


        SharedPreferences.Editor editor = sp.edit();
        editor.putString("user_name", this.user_name);
        Boolean a = editor.commit();

        return null;
    }

    @Override
    protected void onPostExecute(Integer integer) {
        super.onPostExecute(integer);
        Intent intent = new Intent(this.login_acivity, MainActivity.class);
        this.login_acivity.finish();
        this.login_acivity.startActivity(intent);

    }
}