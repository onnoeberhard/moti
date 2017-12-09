package com.moti.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

public class RequestActivity extends AppCompatActivity {

    TextView nametv;
    TextView agetv;
    TextView gendertv;
    TextView msgtv;
    Button openchat;
    Button accept;
    Button decline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_request);
        nametv = (TextView) findViewById(R.id.ar_p2name);
        agetv = (TextView) findViewById(R.id.ar_p2age);
        gendertv = (TextView) findViewById(R.id.ar_p2gender);
        msgtv = (TextView) findViewById(R.id.ar_message);
        openchat = (Button) findViewById(R.id.ar_chat);
        accept = (Button) findViewById(R.id.ar_ok);
        decline = (Button) findViewById(R.id.ar_no);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Request");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        new OnlineDatabaseHandler(this).get(new OnlineDatabaseHandler.WebDbUser() {
            @Override
            public void onResult(JSONObject json) {
                try {
                    final String uid = json.getJSONObject("row").getString("uid");
                    msgtv.setText(json.getJSONObject("row").getString("message"));
                    new OnlineDatabaseHandler(RequestActivity.this).get(new OnlineDatabaseHandler.WebDbUser() {
                        @Override
                        public void onResult(JSONObject json) {
                            try {
                                nametv.setText(json.getJSONObject("row").getString("name"));
                                agetv.setText(Integer.toString(LocalDatabaseHandler.calculateAge(json.getJSONObject("row").getString("birthday"))));
                                gendertv.setText(json.getJSONObject("row").getString("gender"));
                            } catch (JSONException | ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }, "users", "id", uid);
                    openchat.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent i = new Intent(RequestActivity.this, ChatActivity.class);
                            i.putExtra("uid", uid);
                            startActivity(i);
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, "requests", "id", getIntent().getStringExtra("id"));
    }
}
