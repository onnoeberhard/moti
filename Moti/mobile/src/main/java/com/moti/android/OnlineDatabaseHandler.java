package com.moti.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OnlineDatabaseHandler {

    // todo: die datenbank wurde komplett verändert. aber erstmal nur offline arbeiten :)

    public static final String address = "http://moti.16mb.com/API/";

    public static final String USERS = "users";
    public static final String USERS_ID = "id";
    public static final String USERS_USERNAME = "username";
    public static final String USERS_PASSWORD = "password";
    public static final String USERS_NAME = "name";
    public static final String USERS_BIRTHDAY = "birthday";
    public static final String USERS_GENDER = "gender";
    public static final String USERS_LOCATION = "location";
    public static final String USERS_RADIUS = "radius";
    public static final String USERS_FCM_RID = "fcm_rid";
    public static final String USERS_UPDATE_NEAR = "update_near";

    public static final String REQUESTS = "requests";
    public static final String REQUESTS_ID = "id";
    public static final String REQUESTS_UID = "uid";
    public static final String REQUESTS_AGES = "ages";
    public static final String REQUESTS_GENDER = "gender";
    public static final String REQUESTS_MESSAGE = "message";
    public static final String REQUESTS_LOCATION = "location";
    public static final String REQUESTS_RADIUS = "radius";

    public static final String MEETINGPOINTS = "meetingpoints";
    public static final String MEETINGPOINTS_ID = "id";
    public static final String MEETINGPOINTS_NAME = "name";
    public static final String MEETINGPOINTS_TIMETABLE = "timetable";
    public static final String MEETINGPOINTS_LOCATION = "location";
    public static final String MEETINGPOINTS_ACTIVE = "active";


    public final boolean print_response = false;

    Context _context;
    boolean internet = true;
    SharedPreferences sp;

    public OnlineDatabaseHandler(Context context) {
        _context = context;
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (!isConnectedToInternet(context))
            internet = false;
    }

//    public static String md5(String input) {
//        String result = input;
//        if (input != null) {
//            try {
//                MessageDigest md = MessageDigest.getInstance("MD5");
//                md.update(input.getBytes());
//                BigInteger hash = new BigInteger(1, md.digest());
//                result = hash.toString(16);
//                while (result.length() < 32) {
//                    result = "0" + result;
//                }
//            } catch (NoSuchAlgorithmException e) {
//                e.printStackTrace();
//            }
//        }
//        return result;
//    }

    public static String sha256(String input) {
        String result = input;
        if (input != null) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] bytes = md.digest(input.getBytes());
                StringBuffer sb = new StringBuffer();
                for (byte aByte : bytes)
                    sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
                result = sb.toString();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public static boolean isConnectedToInternet(Context _context) {
        ConnectivityManager connectivity = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (NetworkInfo anInfo : info)
                    if (anInfo.getState() == NetworkInfo.State.CONNECTED)
                        return true;
        }
        return false;
    }

    public void execSQL(WebDbUser wdu, String sql) {
        if (internet) {
            WebDbTask wdt = new WebDbTask(wdu);
            wdt.execute("EXEC", sql);
        } else if (wdu != null)
            wdu.onResult(null);
    }

    public void register(WebDbUser wdu, String username, String password, String name, String birthday, String gender) {
        if (internet) {
            WebDbTask wdt = new WebDbTask(wdu);
            wdt.execute("REGISTER", username, password, name, birthday, gender);
        } else if (wdu != null)
            wdu.onResult(null);
    }

    public void login(WebDbUser wdu, String username, String password) {
        if (internet) {
            WebDbTask wdt = new WebDbTask(wdu);
            wdt.execute("LOGIN", username, password);
        } else if (wdu != null)
            wdu.onResult(null);
    }

    public void forgotPassword(WebDbUser wdu, String email) {
        if (internet) {
            WebDbTask wdt = new WebDbTask(wdu);
            wdt.execute("FORGOT_PASSWORD", email);
        } else if (wdu != null)
            wdu.onResult(null);
    }

    public void changePassword(WebDbUser wdu, String old_pw, String new_pw) {
        if (internet) {
            WebDbTask wdt = new WebDbTask(wdu);
            wdt.execute("CHANGE_PASSWORD", sp.getString(MainActivity.UID, ""), sha256(old_pw), sha256(new_pw));
        } else if (wdu != null)
            wdu.onResult(null);
    }

    public void getMeetingPoints(WebDbUser wdu, LatLng northeast, LatLng southwest) {
        if (internet) {
            String bounds = Double.toString(northeast.latitude) + "," + Double.toString(northeast.longitude) + ";" + Double.toString(southwest.latitude) + "," + Double.toString(southwest.longitude);
            WebDbTask wdt = new WebDbTask(wdu);
            wdt.execute("GETMEETINGPOINTS", bounds);
        } else if (wdu != null)
            wdu.onResult(null);
    }

    public void sendChatMessage(WebDbUser wdu, String from, String to, String message) {
        if (internet) {
            WebDbTask wdt = new WebDbTask(wdu);
            wdt.execute("SEND_CHAT", from, to, message);
        } else if (wdu != null)
            wdu.onResult(null);
    }

    public void mail(WebDbUser wdu, String body, String subject, String recipient) {
        if (internet) {
            WebDbTask wdt = new WebDbTask(wdu);
            wdt.execute("MAIL", body, subject, recipient);
        } else if (wdu != null)
            wdu.onResult(null);
    }

    public void get(WebDbUser wdu, String table, String id_key,
                    String id_value) {
        if (internet) {
            WebDbTask wdt = new WebDbTask(wdu);
            wdt.execute("GET", table, id_key, id_value);
        } else if (wdu != null)
            wdu.onResult(null);
    }

    public void get(WebDbUser wdu, String table, String id_key,
                    String id_value, String column) {
        if (internet) {
            WebDbTask wdt = new WebDbTask(wdu);
            wdt.execute("GET", table, id_key, id_value, column);
        } else if (wdu != null)
            wdu.onResult(null);
    }

    public void inup(WebDbUser wdu, String table, String... pairs) {
        if (internet) {
            WebDbTask wdt = new WebDbTask(wdu);
            String[] one = {"INUP", table};
            String[] all = concat(one, pairs);
            wdt.execute(all);
        } else if (wdu != null)
            wdu.onResult(null);
    }

    public interface WebDbUser {
        void onResult(JSONObject json);
    }

    public class WebDbTask extends AsyncTask<String, Void, JSONObject> {

        WebDbUser mwdu;

        public WebDbTask(WebDbUser wdu) {
            mwdu = wdu;
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            switch (params[0]) {
                case "EXEC": {
                    List<AbstractMap.SimpleEntry> list = new ArrayList<>();
                    list.add(new AbstractMap.SimpleEntry<>("a", params[0]));
                    list.add(new AbstractMap.SimpleEntry<>("sql", params[1]));
                    new JSONParser().query(address, list);
                    break;
                }
                case "GETMEETINGPOINTS": {
                    List<AbstractMap.SimpleEntry> list = new ArrayList<>();
                    list.add(new AbstractMap.SimpleEntry<>("a", params[0]));
                    list.add(new AbstractMap.SimpleEntry<>("bounds", params[1]));
                    return new JSONParser().getJSONFromUrl(address, list);
                }
                case "SEND_CHAT": {
                    List<AbstractMap.SimpleEntry> list = new ArrayList<>();
                    list.add(new AbstractMap.SimpleEntry<>("a", params[0]));
                    list.add(new AbstractMap.SimpleEntry<>("from", params[1]));
                    list.add(new AbstractMap.SimpleEntry<>("to", params[2]));
                    list.add(new AbstractMap.SimpleEntry<>("message", params[3]));
                    new JSONParser().query(address, list);
                    break;
                }
                case "REGISTER": {
                    List<AbstractMap.SimpleEntry> list = new ArrayList<>();
                    list.add(new AbstractMap.SimpleEntry<>("a", params[0]));
                    list.add(new AbstractMap.SimpleEntry<>("username", params[1]));
                    list.add(new AbstractMap.SimpleEntry<>("password", params[2]));
                    list.add(new AbstractMap.SimpleEntry<>("name", params[3]));
                    list.add(new AbstractMap.SimpleEntry<>("birthday", params[4]));
                    list.add(new AbstractMap.SimpleEntry<>("gender", params[5]));
                    return new JSONParser().getJSONFromUrl(address, list);
                }
                case "LOGIN": {
                    List<AbstractMap.SimpleEntry> list = new ArrayList<>();
                    list.add(new AbstractMap.SimpleEntry<>("a", params[0]));
                    list.add(new AbstractMap.SimpleEntry<>("username", params[1]));
                    list.add(new AbstractMap.SimpleEntry<>("password", params[2]));
                    return new JSONParser().getJSONFromUrl(address, list);
                }
                case "FORGOT_PASSWORD": {
                    List<AbstractMap.SimpleEntry> list = new ArrayList<>();
                    list.add(new AbstractMap.SimpleEntry<>("a", params[0]));
                    list.add(new AbstractMap.SimpleEntry<>("email", params[1]));
                    return new JSONParser().getJSONFromUrl(address, list);
                }
                case "CHANGE_PASSWORD": {
                    List<AbstractMap.SimpleEntry> list = new ArrayList<>();
                    list.add(new AbstractMap.SimpleEntry<>("a", params[0]));
                    list.add(new AbstractMap.SimpleEntry<>("uid", params[1]));
                    list.add(new AbstractMap.SimpleEntry<>("old_pw", params[1]));
                    list.add(new AbstractMap.SimpleEntry<>("new_pw", params[1]));
                    return new JSONParser().getJSONFromUrl(address, list);
                }
                case "MAIL": {
                    List<AbstractMap.SimpleEntry> list = new ArrayList<>();
                    list.add(new AbstractMap.SimpleEntry<>("a", params[0]));
                    list.add(new AbstractMap.SimpleEntry<>("body", params[1]));
                    list.add(new AbstractMap.SimpleEntry<>("subject", params[2]));
                    list.add(new AbstractMap.SimpleEntry<>("recipient", params[3]));
                    new JSONParser().query(address, list);
                    break;
                }
                case "GET": {
                    List<AbstractMap.SimpleEntry> list = new ArrayList<>();
                    list.add(new AbstractMap.SimpleEntry<>("a", params[0]));
                    list.add(new AbstractMap.SimpleEntry<>("table", params[1]));
                    list.add(new AbstractMap.SimpleEntry<>("id_key", params[2]));
                    list.add(new AbstractMap.SimpleEntry<>("id_value", params[3]));
                    if (params.length == 5)
                        list.add(new AbstractMap.SimpleEntry<>("column", params[4]));
                    return new JSONParser().getJSONFromUrl(address, list);
                }
                case "INUP": {
                    List<AbstractMap.SimpleEntry> list = new ArrayList<>();
                    list.add(new AbstractMap.SimpleEntry<>("a", params[0]));
                    list.add(new AbstractMap.SimpleEntry<>("table", params[1]));
                    int o = 0;
                    for (int i = 2; i < params.length; i++) {
                        if (i % 2 == 0) {
                            list.add(new AbstractMap.SimpleEntry<>("key_" + Integer.toString(o), params[i]));
                        } else {
                            list.add(new AbstractMap.SimpleEntry<>("value_" + Integer.toString(o), params[i]));
                            o++;
                        }
                    }
                    new JSONParser().query(address, list);
                    break;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject json) {
            if (mwdu != null)
                mwdu.onResult(json);
            cancel(true);
        }
    }

    public class JSONParser {

        public JSONParser() {
        }

        public JSONObject getJSONFromUrl(String _url, List<AbstractMap.SimpleEntry> params) {
            JSONObject jsonObject = null;
            try {
                URL url = new URL(_url);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(getQuery(params));
                writer.flush();
                writer.close();
                os.close();
                String response = "";
                if (print_response)
                    System.out.println("START: Get-Response");
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    String line;
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while ((line = br.readLine()) != null) {
                        response += line;
                        if (print_response)
                            System.out.println(line);
                    }
                }
                if (print_response)
                    System.out.println("END: Get-Response");
                jsonObject = new JSONObject(response);
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return jsonObject;
        }

        public String getQuery(List<AbstractMap.SimpleEntry> params) throws UnsupportedEncodingException {
            StringBuilder result = new StringBuilder();
            boolean first = true;
            for (AbstractMap.SimpleEntry pair : params) {
                if (first)
                    first = false;
                else
                    result.append("&");
                result.append(URLEncoder.encode((String) pair.getKey(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode((String) pair.getValue(), "UTF-8"));
            }
            return result.toString();
        }

        public boolean query(String _url, List<AbstractMap.SimpleEntry> params) {
            try {
                URL url = new URL(_url);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(getQuery(params));
                writer.flush();
                writer.close();
                os.close();
                String response = "";
                if (print_response)
                    System.out.println("START: Query-Response");
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    String line;
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while ((line = br.readLine()) != null) {
                        response += line;
                        if (print_response)
                            System.out.println(line);
                    }
                }
                if (print_response)
                    System.out.println("END: Query-Response");
                return response.length() > 0;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

    }

}