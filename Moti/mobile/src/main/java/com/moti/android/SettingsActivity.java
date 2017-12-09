package com.moti.android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.security.NoSuchAlgorithmException;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Settings");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getFragmentManager().beginTransaction()
                .add(R.id.settingsFrame, new SettingsFragment())
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            final LocalDatabaseHandler ldb = new LocalDatabaseHandler(getActivity());
            int radius = sp.getInt(MainActivity.RADIUS, 10000);
            findPreference("radius").setSummary(radius == 0 ? "No Radius" : radius == 100 ? "100m"
                    : radius == 300 ? "300m" : radius == 500 ? "500m" : radius == 1000 ? "1km"
                    : radius == 3000 ? "3km" : radius == 5000 ? "5km" : radius == 10000 ? "10km"
                    : radius == 12000 ? "12km" : radius == 14000 ? "14km" : radius == 16000 ? "16km"
                    : radius == 18000 ? "18km" : radius == 20000 ? "20km" : radius == 23000 ? "23km" : "25km");
            findPreference("radius").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    View v = View.inflate(getActivity(), R.layout.d_settings_radius, null);
                    final SeekBar sb = (SeekBar) v.findViewById(R.id.radiusSB);
                    final TextView tv = (TextView) v.findViewById(R.id.radiusTV);
                    sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            tv.setText(progress == 0 ? "No Radius" : progress == 1 ? "100m" : progress == 2 ? "300m"
                                    : progress == 3 ? "500m" : progress == 4 ? "1km" : progress == 5 ? "3km" : progress == 6 ? "5km"
                                    : progress == 7 ? "10km" : progress == 8 ? "12km" : progress == 9 ? "14km" : progress == 10 ? "16km"
                                    : progress == 11 ? "18km" : progress == 12 ? "20km" : progress == 13 ? "23km" : "25km");
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });
                    int radius = sp.getInt(MainActivity.RADIUS, 10000);
                    sb.setProgress(radius == 0 ? 0 : radius == 100 ? 1 : radius == 300 ? 2 : radius == 500 ? 3 : radius == 1000 ? 4
                            : radius == 3000 ? 5 : radius == 5000 ? 6 : radius == 10000 ? 7 : radius == 12000 ? 8 : radius == 14000 ? 9
                            : radius == 16000 ? 10 : radius == 18000 ? 11 : radius == 20000 ? 12 : radius == 23000 ? 13 : 14);
                    new AlertDialog.Builder(getActivity()).setTitle("Radius")
                            .setView(v)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    int progress = sb.getProgress();
                                    findPreference("radius").setSummary(progress == 0 ? "No Radius" : progress == 1 ? "100m"
                                            : progress == 2 ? "300m" : progress == 3 ? "500m" : progress == 4 ? "1km"
                                            : progress == 5 ? "3km" : progress == 6 ? "5km" : progress == 7 ? "10km"
                                            : progress == 8 ? "12km" : progress == 9 ? "14km" : progress == 10 ? "16km"
                                            : progress == 11 ? "18km" : progress == 12 ? "20km" : progress == 13 ? "23km" : "25km");
                                    sp.edit().putInt(MainActivity.RADIUS, progress == 0 ? 0 : progress == 1 ? 100
                                            : progress == 2 ? 300 : progress == 3 ? 500 : progress == 4 ? 1000
                                            : progress == 5 ? 3000 : progress == 6 ? 5000 : progress == 7 ? 10000
                                            : progress == 8 ? 12000 : progress == 9 ? 14000 : progress == 10 ? 16000
                                            : progress == 11 ? 18000 : progress == 12 ? 20000 : progress == 13 ? 23000 : 25000).apply();
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).create().show();
                    return false;
                }
            });
            findPreference("profvis").setEnabled(sp.getBoolean(MainActivity.PROFVIS, true));
            findPreference("profvis").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    sp.edit().putBoolean(MainActivity.PROFVIS, (boolean) newValue).apply();
                    return true;
                }
            });
            findPreference("logout").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    if (ldb.getAll(LocalDatabaseHandler.CHAT, LocalDatabaseHandler.CHAT_UID).size() > 0) {
                        builder.setMessage("Do you want to delete all chats too?")
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        new OnlineDatabaseHandler(getActivity()).inup(new OnlineDatabaseHandler.WebDbUser() {
                                            @Override
                                            public void onResult(JSONObject json) {
                                                sp.edit().clear().apply();
                                                for (String table : LocalDatabaseHandler.TABLES)
                                                    ldb.execSQL("DELETE FROM " + table);
                                                startActivity(new Intent(getActivity(), MainActivity.class));
                                                System.exit(0);
                                            }
                                        }, OnlineDatabaseHandler.USERS, OnlineDatabaseHandler.USERS_ID, sp.getString(MainActivity.UID, ""), OnlineDatabaseHandler.USERS_LOCATION, "", OnlineDatabaseHandler.USERS_FCM_RID, "");
                                    }
                                })
                                .setNeutralButton("No, just log out", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        new OnlineDatabaseHandler(getActivity()).inup(new OnlineDatabaseHandler.WebDbUser() {
                                            @Override
                                            public void onResult(JSONObject json) {
                                                sp.edit().clear().apply();
                                                for (String table : LocalDatabaseHandler.TABLES)
                                                    if (!table.equals(LocalDatabaseHandler.CHAT))
                                                        ldb.execSQL("DELETE FROM " + table);
                                                startActivity(new Intent(getActivity(), MainActivity.class));
                                                System.exit(0);
                                            }
                                        }, OnlineDatabaseHandler.USERS, OnlineDatabaseHandler.USERS_ID, sp.getString(MainActivity.UID, ""), OnlineDatabaseHandler.USERS_LOCATION, "", OnlineDatabaseHandler.USERS_FCM_RID, "");
                                    }
                                });
                    } else {
                        builder.setMessage("Do you really want to log out?")
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        new OnlineDatabaseHandler(getActivity()).inup(new OnlineDatabaseHandler.WebDbUser() {
                                            @Override
                                            public void onResult(JSONObject json) {
                                                sp.edit().clear().apply();
                                                for (String table : LocalDatabaseHandler.TABLES)
                                                    ldb.execSQL("DELETE FROM " + table);
                                                startActivity(new Intent(getActivity(), MainActivity.class));
                                                System.exit(0);
                                            }
                                        }, OnlineDatabaseHandler.USERS, OnlineDatabaseHandler.USERS_ID, sp.getString(MainActivity.UID, ""), OnlineDatabaseHandler.USERS_LOCATION, "", OnlineDatabaseHandler.USERS_FCM_RID, "");
                                    }
                                });
                    }
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create().show();
                    return false;
                }
            });
            findPreference("changepw").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    View v = View.inflate(getActivity(), R.layout.d_settings_changepw, null);
                    final EditText oldpw = (EditText) v.findViewById(R.id.oldpw);
                    final EditText newpw = (EditText) v.findViewById(R.id.newpw);
                    final EditText newpw2 = (EditText) v.findViewById(R.id.newpw2);
                    new AlertDialog.Builder(getActivity()).setTitle("Change Password")
                            .setView(v)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (!newpw.getText().toString().equals(newpw2.getText().toString()))
                                        Toast.makeText(getActivity(), "The two new passwords do not match!", Toast.LENGTH_SHORT).show();
                                    else {
                                        new OnlineDatabaseHandler(getActivity()).changePassword(new OnlineDatabaseHandler.WebDbUser() {
                                            @Override
                                            public void onResult(JSONObject json) {
                                                try {
                                                    if (json.get("result").equals("*OK*"))
                                                        sp.edit().putString(MainActivity.PASSWORD, OnlineDatabaseHandler.sha256(newpw.getText().toString())).apply();
                                                    else
                                                        Toast.makeText(getActivity(), R.string.error_sorry, Toast.LENGTH_SHORT).show();
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                    Toast.makeText(getActivity(), R.string.error_sorry, Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        }, oldpw.getText().toString(), newpw.getText().toString());
                                    }
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).create().show();
                    return false;
                }
            });
            findPreference("deleteaccount").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(getActivity()).setMessage("Are you sure you want to delete your account?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    new OnlineDatabaseHandler(getActivity()).inup(new OnlineDatabaseHandler.WebDbUser() {
                                        @Override
                                        public void onResult(JSONObject json) {
                                            sp.edit().clear().apply();
                                            for (String table : LocalDatabaseHandler.TABLES)
                                                ldb.execSQL("DELETE FROM " + table);
                                            startActivity(new Intent(getActivity(), MainActivity.class));
                                            System.exit(0);
                                        }
                                    }, OnlineDatabaseHandler.USERS, OnlineDatabaseHandler.USERS_ID, sp.getString(MainActivity.UID, ""), "*DELETE*");
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).create().show();
                    return false;
                }
            });
        }
    }
}
