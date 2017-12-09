package com.moti.android;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

public class WelcomeLRActivity extends AppCompatActivity {

    WLRLoginRegister mWLRLoginRegister;
    boolean backeqr1 = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_welcomelr);
        mWLRLoginRegister = new WLRLoginRegister();
        ViewPager viewPager = (ViewPager) findViewById(R.id.wlrPager);
        WLRPagerAdapter pagerAdapter = new WLRPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);
        if (!OnlineDatabaseHandler.isConnectedToInternet(this)) {
            new android.app.AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setMessage("It seems that you have no internet connection!")
                    .setPositiveButton("Try again!", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(WelcomeLRActivity.this, WelcomeLRActivity.class));
                            finish();
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            finish();
                        }
                    }).create().show();
        }
    }

    @Override
    public void onBackPressed() {
        if (backeqr1)
            mWLRLoginRegister.gotor1();
        else
            super.onBackPressed();
    }

    public static class WLRWelcome1 extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.f_wlr_welcome1, container, false);
        }
    }

    public static class WLRLoginRegister extends Fragment {
        View r1;
        EditText r1username;
        EditText r1password;
        Button r1register;
        Button r1help;
        Button r1login;
        View r2;
        EditText r2name;
        Button r2pickBirthday;
        TextView r2birthday;
        RadioGroup r2gender;
        RadioButton r2nogender;
        RadioButton r2male;
        RadioButton r2female;
        Button r2ok;
        Button r2help;
        Button r2back;
        View l;
        EditText lusername;
        EditText lpassword;
        Button llogin;
        Button lforgotpassword;
        Button lhelp;
        Button lback;

        SharedPreferences sp;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.f_wlr_loginregister, container, false);
            sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            r1 = v.findViewById(R.id.r1);
            r1username = (EditText) v.findViewById(R.id.r1username);
            r1password = (EditText) v.findViewById(R.id.r1password);
            r1register = (Button) v.findViewById(R.id.r1register);
            r1help = (Button) v.findViewById(R.id.r1help);
            r1login = (Button) v.findViewById(R.id.r1login);
            r2 = v.findViewById(R.id.r2);
            r2name = (EditText) v.findViewById(R.id.r2name);
            r2pickBirthday = (Button) v.findViewById(R.id.r2pickBirthday);
            r2birthday = (TextView) v.findViewById(R.id.r2birthday);
            r2gender = (RadioGroup) v.findViewById(R.id.r2gender);
            r2nogender = (RadioButton) v.findViewById(R.id.r2nogender);
            r2male = (RadioButton) v.findViewById(R.id.r2male);
            r2female = (RadioButton) v.findViewById(R.id.r2female);
            r2ok = (Button) v.findViewById(R.id.r2ok);
            r2help = (Button) v.findViewById(R.id.r2help);
            r2back = (Button) v.findViewById(R.id.r2back);
            l = v.findViewById(R.id.l);
            lusername = (EditText) v.findViewById(R.id.lusername);
            lpassword = (EditText) v.findViewById(R.id.lpassword);
            llogin = (Button) v.findViewById(R.id.llogin);
            lforgotpassword = (Button) v.findViewById(R.id.lforgotPassword);
            lhelp = (Button) v.findViewById(R.id.lhelp);
            lback = (Button) v.findViewById(R.id.lback);
            r1register.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (r1username.getText().length() == 0 || r1password.getText().length() == 0)
                        Toast.makeText(getActivity(), "Please enter a username / email and a password.",
                                Toast.LENGTH_LONG).show();
                    else
                        new OnlineDatabaseHandler(getActivity()).get(new OnlineDatabaseHandler.WebDbUser() {
                            @Override
                            public void onResult(JSONObject json) {
                                try {
                                    if (!json.get("value").equals(JSONObject.NULL))
                                        Toast.makeText(getActivity(), "Username / email already registered!", Toast.LENGTH_SHORT).show();
                                    else
                                        gotor2();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, OnlineDatabaseHandler.USERS, OnlineDatabaseHandler.USERS_USERNAME, r1username.getText().toString(), OnlineDatabaseHandler.USERS_ID);
                }
            });
            r1help.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(getActivity()).setMessage("help text for r1").show();
                }
            });
            r1login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    gotol();
                }
            });
            final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            r2pickBirthday.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                            calendar.set(year, monthOfYear, dayOfMonth);
                            r2birthday.setText((new SimpleDateFormat("yyyyMMdd", Locale.UK)).format(calendar.getTime()));
                        }
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
                }
            });
            r2ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (r2name.getText().length() == 0)
                        Toast.makeText(getActivity(), "Please enter your name / nickname.",
                                Toast.LENGTH_LONG).show();
                    else
                        register();
                }
            });
            r2help.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(getActivity()).setMessage("help text for r2").show();
                }
            });
            r2back.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    gotor1();
                }
            });
            llogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (lusername.getText().length() == 0 || lpassword.getText().length() == 0)
                        Toast.makeText(getActivity(), "Please enter your username / email and password.", Toast.LENGTH_SHORT).show();
                    else
                        login();
                }
            });
            lforgotpassword.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    forgotpassword();
                }
            });
            lhelp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(getActivity()).setMessage("help text for l").show();
                }
            });
            lback.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    gotor1();
                }
            });
            return v;
        }

        public void gotor2() {
            r1.setVisibility(View.GONE);
            r2.setVisibility(View.VISIBLE);
            ((WelcomeLRActivity) getActivity()).backeqr1 = true;
        }

        public void gotol() {
            r1.setVisibility(View.GONE);
            l.setVisibility(View.VISIBLE);
            ((WelcomeLRActivity) getActivity()).backeqr1 = true;
        }

        public void gotor1() {
            r1.setVisibility(View.VISIBLE);
            r2.setVisibility(View.GONE);
            l.setVisibility(View.GONE);
            ((WelcomeLRActivity) getActivity()).backeqr1 = false;
        }

        public void register() {
            final String username = r1username.getText().toString();
            final String name = r2name.getText().toString();
            final String birthday = r2birthday.getText().toString();
            final String gender = r2male.isChecked() ? "m" : (r2female.isChecked() ? "f" : "n");
            final String pw = OnlineDatabaseHandler.sha256(r1password.getText().toString());
            new OnlineDatabaseHandler(getActivity()).register(new OnlineDatabaseHandler.WebDbUser() {
                @Override
                public void onResult(JSONObject json) {
                    try {
                        String uid = json.getString("id");
                        if (!uid.equals("*FALSE*")) {
                            sp.edit().putString(MainActivity.UID, uid)
                                    .putString(MainActivity.USERNAME, username)
                                    .putString(MainActivity.PASSWORD, pw)
                                    .putString(MainActivity.NAME, name)
                                    .putString(MainActivity.BIRTHDAY, birthday)
                                    .putString(MainActivity.GENDER, gender).apply();
                            startActivity(new Intent(getActivity(), MainActivity.class));
                            getActivity().finish();
                        } else {
                            Toast.makeText(getActivity(), R.string.error_sorry, Toast.LENGTH_SHORT).show();
                            gotor1();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, username, pw, name, birthday, gender);
        }

        public void login() {
            new OnlineDatabaseHandler(getActivity()).login(new OnlineDatabaseHandler.WebDbUser() {
                @Override
                public void onResult(JSONObject json) {
                    try {
                        if (json.getString("result").equals("*NULL*"))
                            Toast.makeText(getActivity(), "Account doesn't exist!", Toast.LENGTH_SHORT).show();
                        else if (json.getString("result").equals("*FALSE*"))
                            Toast.makeText(getActivity(), "Wrong Password!", Toast.LENGTH_SHORT).show();
                        else {
                            int radius;
                            try {
                                radius = Integer.parseInt(json.getJSONObject("row").getString("radius"));
                            } catch (NumberFormatException e) {
                                radius = 500;
                            }
                            sp.edit().putString(MainActivity.UID, json.getJSONObject("row").getString("id"))
                                    .putString(MainActivity.USERNAME, lusername.getText().toString())
                                    .putString(MainActivity.PASSWORD, OnlineDatabaseHandler.sha256(lpassword.getText().toString()))
                                    .putString(MainActivity.NAME, json.getJSONObject("row").getString("name"))
                                    .putString(MainActivity.BIRTHDAY, json.getJSONObject("row").getString("birthday"))
                                    .putString(MainActivity.GENDER, json.getJSONObject("row").getString("gender"))
                                    .putInt(MainActivity.RADIUS, radius).apply();
                            if (json.getString("notice").equals("RECOVERED"))
                                Toast.makeText(getActivity(), "Go to Settings to change your password.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getActivity(), MainActivity.class));
                            getActivity().finish();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, lusername.getText().toString(), OnlineDatabaseHandler.sha256(lpassword.getText().toString()));
        }

        public void forgotpassword() {
            if (lusername.getText().length() == 0)
                Toast.makeText(getActivity(), "Please enter your email", Toast.LENGTH_SHORT).show();
            else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(lusername.getText()).matches())
                Toast.makeText(getActivity(), "You can only use this function with an email address.", Toast.LENGTH_SHORT).show();
            else {
                new OnlineDatabaseHandler(getActivity()).forgotPassword(new OnlineDatabaseHandler.WebDbUser() {
                    @Override
                    public void onResult(JSONObject json) {
                        try {
                            if (json.getString("result").equals("*OK*")) {
                                if (json.getString("notice").equals("ANEW"))
                                    Toast.makeText(getActivity(), "You have been sent a new email with a new password! (Only the latest one is valid)", Toast.LENGTH_LONG).show();
                                else
                                    Toast.makeText(getActivity(), "You have been sent a email your new password!", Toast.LENGTH_LONG).show();
                            } else
                                Toast.makeText(getActivity(), R.string.error_sorry, Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, lusername.getText().toString());
            }
        }

//        public void forgotPassword(View v) {
//            if (et1.getText().length() == 0)
//                Toast.makeText(this, "Please enter your Username", Toast.LENGTH_SHORT).show();
//            else {
//                final OnlineDatabaseHandler dbo = new OnlineDatabaseHandler(this);
//                loading.setVisibility(View.VISIBLE);
//                dbo.getFromDB(new WebDbUser() {
//                    @Override
//                    public void gottenFromWeb(JSONObject json, boolean good, boolean success) {
//                        if (success) {
//                            if (!good) {
//                                Toast.makeText(ActivityStart.this, "User doesn't exist!", Toast.LENGTH_SHORT).show();
//                                loading.setVisibility(View.GONE);
//                            } else {
//                                dbo.getFromDB(new WebDbUser() {
//                                    @Override
//                                    public void gottenFromWeb(final JSONObject json, boolean good, boolean success) {
//                                        if (success) {
//                                            if (!good) {
//                                                Toast.makeText(ActivityStart.this, "We weren't able to send you your password. You apparently didn't link your account to a phone number. We're sorry.", Toast.LENGTH_LONG).show();
//                                                loading.setVisibility(View.GONE);
//                                            } else {
//                                                String chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
//                                                String new_pw = "";
//                                                Random r = new Random();
//                                                for (int i = 0; i < 5; i++)
//                                                    new_pw += chars.charAt(r.nextInt(chars.length()));
//                                                final String npw = new_pw;
//                                                try {
//                                                    dbo.inUpData(new WebDbUser() {
//                                                        @Override
//                                                        public void gottenFromWeb(JSONObject j, boolean good, boolean success) {
//                                                            if (success && good) {
//                                                                try {
//                                                                    SmsManager smsManager = SmsManager.getDefault();
//                                                                    smsManager.sendTextMessage(json.getString("value"), null,
//                                                                            "Your new password is: " + npw, null, null);
//                                                                    Toast.makeText(ActivityStart.this, "Your password was sent to you via SMS.", Toast.LENGTH_SHORT).show();
//                                                                } catch (JSONException e) {
//                                                                    e.printStackTrace();
//                                                                    Toast.makeText(ActivityStart.this, R.string.error_sorry, Toast.LENGTH_SHORT).show();
//                                                                }
//                                                            } else
//                                                                Toast.makeText(ActivityStart.this, R.string.error_sorry, Toast.LENGTH_SHORT).show();
//                                                            loading.setVisibility(View.GONE);
//                                                        }
//                                                    }, "accounts", "username", et1.getText().toString(), "data", "new_pw", "", "", "", "new_pw{{:}}" + OnlineDatabaseHandler.md5(new_pw));
//                                                } catch (NoSuchAlgorithmException e) {
//                                                    e.printStackTrace();
//                                                }
//                                            }
//                                        } else {
//                                            loading.setVisibility(View.GONE);
//                                            Toast.makeText(ActivityStart.this, R.string.error_sorry, Toast.LENGTH_SHORT).show();
//                                        }
//                                    }
//                                }, "account_links", "user", et1.getText().toString(), "phone");
//                            }
//                        } else {
//                            loading.setVisibility(View.GONE);
//                            Toast.makeText(ActivityStart.this, R.string.error_sorry, Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                }, "accounts", "username", et1.getText().toString(), "id");
//            }
//        }

    }

    public class WLRPagerAdapter extends FragmentPagerAdapter {
        public WLRPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) return new WLRWelcome1();
            return mWLRLoginRegister;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
