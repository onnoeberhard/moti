package com.moti.android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.android.gms.maps.MapFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SearchActivity extends AppCompatActivity {

    public static final int MODE_NEW = 0;
    public static final int MODE_ACTIVE = 1;

    int mode = MODE_NEW;

    Button nbWho;
    TextView nwhoTV;
    Button nbWhere;
    TextView nwhereTV;
    Button nbMessage;
    TextView nmessageTV;
    MapFragment nmap;
    Button ngo;

    TextView aas;
    TextView amessage;
    TextView alocation;
    Button aeas;
    Button aemessage;
    Button aelocation;
    Button acancel;

    int radius = 500;
    int tolerance = 4;
    String whoAge = "a";
    String whoSex = "a";
    String where = "u";
    String message = "";

    String sid;

    LocalDatabaseHandler ldb;
    SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mode = getIntent().getIntExtra("mode", MODE_NEW);
        ldb = new LocalDatabaseHandler(this);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (mode == MODE_NEW) {
            setContentView(R.layout.a_search_new);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            toolbar.setTitle("New Search");
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            nbWho = (Button) findViewById(R.id.sWho);
            nwhoTV = (TextView) findViewById(R.id.sWhoTv);
            nbWhere = (Button) findViewById(R.id.sWhere);
            nwhereTV = (TextView) findViewById(R.id.sWhereTv);
            nbMessage = (Button) findViewById(R.id.sMessage);
            nmessageTV = (TextView) findViewById(R.id.sMessageTv);
            nmap = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
            ngo = (Button) findViewById(R.id.sGo);
            nbWho.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    who();
                }
            });
            nwhoTV.setText("anyone");
            nbWhere.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    where();
                }
            });
            radius = sp.getInt(MainActivity.RADIUS, 500);
            tolerance = sp.getInt(MainActivity.TOLERANCE, 4);
            nwhereTV.setText("r = " + Integer.toString(radius) + "m");
            nbMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    message();
                }
            });
            nmessageTV.setText("");
            ngo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final LocalDatabaseHandler ldb = new LocalDatabaseHandler(SearchActivity.this);
                    String ages = "a";
                    if (whoAge.equals("m")) {
                        try {
                            int age = LocalDatabaseHandler.calculateAge(sp.getString(MainActivity.BIRTHDAY, ""));
                            ages = Integer.toString(age - tolerance) + ";" + Integer.toString(age + tolerance);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                    // TODO: 07.02.2016 where should be <mpid> if not "u"
                    new OnlineDatabaseHandler(SearchActivity.this).inup(
                            new OnlineDatabaseHandler.WebDbUser() {
                                @Override
                                public void onResult(JSONObject json) {
                                    DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSSS", Locale.UK);
                                    Date today = Calendar.getInstance().getTime();
                                    final String date = df.format(today);
                                    new OnlineDatabaseHandler(SearchActivity.this).get(new OnlineDatabaseHandler.WebDbUser() {
                                        @Override
                                        public void onResult(JSONObject json) {
                                            try {
                                                ldb.inup("notifications", "type", "s", "id", json.getString("value"), "time", date);
                                                ldb.close();
                                                Intent i = new Intent(SearchActivity.this, SearchActivity.class);
                                                i.putExtra("mode", SearchActivity.MODE_ACTIVE);
                                                startActivity(i);
                                                finish();
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }, "requests", "uid", sp.getString(MainActivity.UID, ""), "id");
                                }
                            }, "requests", "uid", sp.getString(MainActivity.UID, ""), "ages", ages, "gender", whoSex, "message", message, "location", where, "radius", Integer.toString(radius)
                    );
                }
            });
        } else {
            setContentView(R.layout.a_search_active);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            toolbar.setTitle("Active Search");
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            aas = (TextView) findViewById(R.id.as_as);
            amessage = (TextView) findViewById(R.id.as_message);
            alocation = (TextView) findViewById(R.id.as_location);
            aeas = (Button) findViewById(R.id.as_eas);
            aemessage = (Button) findViewById(R.id.as_emessage);
            aelocation = (Button) findViewById(R.id.as_elocation);
            acancel = (Button) findViewById(R.id.as_cancel);
            new OnlineDatabaseHandler(this).get(new OnlineDatabaseHandler.WebDbUser() {
                @Override
                public void onResult(JSONObject json) {
                    try {
                        sid = json.getJSONObject("row").getString("id");
                        whoAge = json.getJSONObject("row").getString("ages");
                        whoSex = json.getJSONObject("row").getString("gender");
                        message = json.getJSONObject("row").getString("message");
                        radius = Integer.parseInt(json.getJSONObject("row").getString("radius"));
                        where = json.getJSONObject("row").getString("location");
                        aas.setText(whoAge + " - " + whoSex);
                        amessage.setText(message);
                        alocation.setText(where + " - " + Integer.toString(radius));
                    } catch (JSONException | NullPointerException e) {
                        e.printStackTrace();
                    }
                }
            }, "requests", "id", ldb.get("notifications", "type", "s", "id"));
            acancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new OnlineDatabaseHandler(SearchActivity.this).inup(new OnlineDatabaseHandler.WebDbUser() {
                        @Override
                        public void onResult(JSONObject json) {
                            ldb.inup("notifications", "type", "s", "*DELETE*");
                            finish();
                        }
                    }, "requests", "uid", sp.getString(MainActivity.UID, ""), "*DELETE*");
                }
            });
            aeas.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    who();
                }
            });
            aemessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    message();
                }
            });
            aelocation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    where();
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void who() {
        View whoView = View.inflate(this, R.layout.d_search_who, null);
        final RadioButton anyage = (RadioButton) whoView.findViewById(R.id.dswho_anyage);
        final RadioButton myage = (RadioButton) whoView.findViewById(R.id.dswho_myage);
        final RadioButton anygender = (RadioButton) whoView.findViewById(R.id.dswho_anygender);
        final RadioButton genderm = (RadioButton) whoView.findViewById(R.id.dswho_male);
        final RadioButton genderf = (RadioButton) whoView.findViewById(R.id.dswho_female);
        final Button settings = (Button) whoView.findViewById(R.id.dswho_settings);
        anyage.setChecked(whoAge.equals("a"));
        myage.setChecked(whoAge.equals("m"));
        anygender.setChecked(whoSex.equals("a"));
        genderm.setChecked(whoSex.equals("m"));
        genderf.setChecked(whoSex.equals("f"));
        new AlertDialog.Builder(this)
                .setTitle("Who do you want to meet?")
                .setView(whoView)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        whoAge = anyage.isChecked() ? "a" : "m";
                        whoSex = anygender.isChecked() ? "a" : (genderm.isChecked() ? "m" : "f");
                        if (mode == MODE_NEW)
                            nwhoTV.setText(
                                    whoAge.equals("a") ?
                                            (whoSex.equals("a") ?
                                                    "Anyone" :
                                                    (whoSex.equals("m") ?
                                                            "anyone male" :
                                                            "anyone female")) :
                                            (whoSex.equals("a") ?
                                                    "Anyone around my age" :
                                                    (whoSex.equals("m") ?
                                                            "male, around my age" :
                                                            "female, around my age")));
                        else {
                            String ages = "a";
                            if (whoAge.equals("m")) {
                                try {
                                    int age = LocalDatabaseHandler.calculateAge(sp.getString(MainActivity.BIRTHDAY, ""));
                                    ages = Integer.toString(age - tolerance) + ";" + Integer.toString(age + tolerance);
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                            new OnlineDatabaseHandler(SearchActivity.this).inup(new OnlineDatabaseHandler.WebDbUser() {
                                @Override
                                public void onResult(JSONObject json) {
                                    aas.setText(whoAge + " - " + whoSex);
                                }
                            }, "requests", "id", sid, "ages", ages, "gender", whoSex);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    public void where() {
        View whereView = View.inflate(this, R.layout.d_search_where, null);
        final RadioButton here = (RadioButton) whereView.findViewById(R.id.dswhere_here);
        final RadioButton radiusrb = (RadioButton) whereView.findViewById(R.id.dswhere_radius);
        final Button mp = (Button) whereView.findViewById(R.id.dswhere_mp);
        final Button settings = (Button) whereView.findViewById(R.id.dswhere_settings);
        final EditText radiuset = (EditText) whereView.findViewById(R.id.dswhere_radiusET);
        radiuset.setText(radius == 0 ? Integer.toString(sp.getInt(MainActivity.RADIUS, 500)) : Integer.toString(radius));
        here.setChecked(radius == 0 && where.equals("u"));
        radiusrb.setChecked(radius > 0 && where.equals("u"));
        new AlertDialog.Builder(this)
                .setTitle("Where do you want to meet?")
                .setView(whereView)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (here.isChecked())
                            radius = 0;
                        else {
                            try {
                                radius = Integer.parseInt(radiuset.getText().toString());
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                        if (mode == MODE_ACTIVE)
                            new OnlineDatabaseHandler(SearchActivity.this).inup(new OnlineDatabaseHandler.WebDbUser() {
                                @Override
                                public void onResult(JSONObject json) {
                                    alocation.setText(where + " - " + Integer.toString(radius));
                                }
                            }, "requests", "id", sid, "location", where, "radius", Integer.toString(radius));
                        else
                            nwhereTV.setText("r = " + Integer.toString(radius) + "m");
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    public void message() {
        View messageView = View.inflate(this, R.layout.d_search_message, null);
        final EditText messageET = (EditText) messageView.findViewById(R.id.dsm_message);
        messageET.setText(message);
        new AlertDialog.Builder(this)
                .setTitle("Add a message")
                .setView(messageView)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        message = messageET.getText().toString();
                        if (mode == MODE_ACTIVE) {
                            new OnlineDatabaseHandler(SearchActivity.this).inup(new OnlineDatabaseHandler.WebDbUser() {
                                @Override
                                public void onResult(JSONObject json) {
                                    amessage.setText(message);
                                }
                            }, "requests", "id", sid, "message", message);
                        } else
                            nmessageTV.setText(message);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .show();
    }
}
