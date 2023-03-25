package pt.ulisboa.tecnico.cmov.conversationalist;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class PollActivity extends AppCompatActivity {

    EditText subject, opt1, opt2, opt3, opt4;
    Button submit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poll);

        submit = (Button) findViewById(R.id.submit_button);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                subject = (EditText) findViewById(R.id.poll_subject);
                opt1 = (EditText) findViewById(R.id.opt1);
                opt2 = (EditText) findViewById(R.id.opt2);
                opt3 = (EditText) findViewById(R.id.opt3);
                opt4 = (EditText) findViewById(R.id.opt4);

                Intent intent = new Intent();
                String poll = "" + subject.getText() + ";" + opt1.getText() + ";" + opt2.getText() + ";" + opt3.getText() + ";" + opt4.getText();
                intent.setData(Uri.parse(poll));
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }





}