package com.moti.android;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class LocalDatabaseHandler extends SQLiteOpenHelper {

    public static final String MOTI = "moti";
    public static final String CHAT = "chat";
    public static final String CHAT_UID = "uid";
    public static final String CHAT_TEXT = "text";
    public static final String CHAT_TIME = "time";
    public static final String CHAT_CONTEXT = "context";
    public static final String CHAT_WHO = "who";
    public static final String CHAT_WHO_ME = "0";
    public static final String CHAT_WHO_THEM = "1";
    public static final String CONTACTS = "contacts";
    public static final String CONTACTS_UID = "uid";
    public static final String CONTACTS_NAME = "name";
    public static final String NEW_CHAT_MESSAGES = "new_chat_messages";
    public static final String NEW_CHAT_MESSAGES_UID = "uid";
    public static final String NEW_CHAT_MESSAGES_TIME = "time";
    public static final String NEW_CHAT_MESSAGES_TEXT = "text";
    public static final String NEAR = "near";
    public static final String NEAR_ID = "id";

    public static final String[] TABLES = {CHAT, CONTACTS, NEW_CHAT_MESSAGES, NEAR};

    public static final String FALSE = "*FALSE*";
    public static final String DELETE = "*DELETE*";

    boolean close = false;

    public LocalDatabaseHandler(Context context) {
        this(context, false);
    }

    public LocalDatabaseHandler(Context context, boolean _close) {
        super(context, MOTI, null, 1);
        close = _close;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + CHAT + "(" + CHAT_UID + " TEXT, " + CHAT_TEXT + " TEXT, " + CHAT_TIME + " TEXT, " + CHAT_CONTEXT + " TEXT, " + CHAT_WHO + " TEXT)");
        db.execSQL("CREATE TABLE " + CONTACTS + "(" + CONTACTS_UID + " TEXT, " + CONTACTS_NAME + " TEXT)");
        db.execSQL("CREATE TABLE " + NEW_CHAT_MESSAGES + "(" + NEW_CHAT_MESSAGES_UID + " TEXT, " + NEW_CHAT_MESSAGES_TIME + " TEXT, " + NEW_CHAT_MESSAGES_TEXT + " TEXT)");
        db.execSQL("CREATE TABLE " + NEAR + "(" + NEAR_ID + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (String table : TABLES)
            db.execSQL("DROP TABLE IF EXISTS " + table);
        onCreate(db);
    }

    public String get(String table, String id_key, String id_value, String column) {
        String result;
        String countQuery = "SELECT * FROM `" + table + "` WHERE `" + id_key + "` = '" + id_value + "'";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            result = cursor.getString(cursor.getColumnIndex(column));
        } else
            result = FALSE;
        cursor.close();
        if (close) this.close();
        return result;
    }

    public Map<String, String> get(String table, String id_key, String id_value) {
        Map<String, String> result = new HashMap<>();
        String countQuery = "SELECT * FROM `" + table + "` WHERE `" + id_key + "` = '" + id_value + "'";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            for (String name : cursor.getColumnNames())
                result.put(name, cursor.getString(cursor.getColumnIndex(name)));
        }
        cursor.close();
        if (close) this.close();
        return result;
    }

    public ArrayList<String> getAll(String table, String column, String id_key, String id_value) {
        ArrayList<String> result = new ArrayList<>();
        String countQuery = "SELECT * FROM `" + table + "`";
        if (id_key != null)
            countQuery = "SELECT * FROM `" + table + "` WHERE `" + id_key + "` = '" + id_value + "'";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            result.add(cursor.getString(cursor.getColumnIndex(column)));
            while (cursor.moveToNext()) {
                result.add(cursor.getString(cursor.getColumnIndex(column)));
            }
        }
        cursor.close();
        if (close) this.close();
        return result;
    }

    public ArrayList<String> getAll(String table, String column) {
        return getAll(table, column, null, null);
    }

    public ArrayList<Map<String, String>> getAll(String table, String id_key, String id_value) {
        ArrayList<Map<String, String>> result = new ArrayList<>();
        String countQuery = "SELECT * FROM `" + table + "`";
        if (id_key != null)
            countQuery = "SELECT * FROM `" + table + "` WHERE `" + id_key + "` = '" + id_value + "'";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            Map<String, String> row0 = new HashMap<>();
            for (String name : cursor.getColumnNames())
                row0.put(name, cursor.getString(cursor.getColumnIndex(name)));
            result.add(row0);
            while (cursor.moveToNext()) {
                Map<String, String> row = new HashMap<>();
                for (String name : cursor.getColumnNames())
                    row.put(name, cursor.getString(cursor.getColumnIndex(name)));
                result.add(row);
            }
        }
        cursor.close();
        if (close) this.close();
        return result;
    }

    public ArrayList<Map<String, String>> getAll(String table) {
        return getAll(table, null, null);
    }

    /**
     * pairs[0] = id_key
     * pairs[1] = id_value
     */
    private void inup(boolean insert, String table, String... pairs) {
        SQLiteDatabase db = getWritableDatabase();
        boolean delete = pairs[0].equals(DELETE) || (pairs.length > 2 && pairs[2].equals(DELETE));
        boolean update = !delete && !pairs[0].equals("") && !(get(table, pairs[0], pairs[1], pairs[0]).equals(FALSE)) && !insert;
        String sql;
        if (delete)
            sql = "DELETE FROM `" + table + (pairs[0].equals(DELETE) ? "`" : "` WHERE `" + pairs[0] + "` = '" + pairs[1] + "'");
        else if (update) {
            String set = "";
            for (int i = 2; i < pairs.length; i++) {
                if (i % 2 == 0) {
                    if (i != 2)
                        set += ", ";
                    set += "`" + pairs[i] + "`";
                } else
                    set += " = '" + pairs[i] + "'";
            }
            if (pairs.length > 2)
                sql = "UPDATE `" + table + "` SET " + set + " WHERE `" + pairs[0] + "` = '" + pairs[1] + "'";
            else
                sql = "";
        } else {
            String names = "";
            String values = "";
            for (int i = 0; i < pairs.length; i++) {
                if (i % 2 == 0) {
                    if (!names.equals(""))
                        names += ", ";
                    names += "`" + pairs[i] + "`";
                } else {
                    if (!values.equals(""))
                        values += ", ";
                    values += "'" + pairs[i] + "'";
                }
            }
            sql = "INSERT INTO `" + table + "` (" + names + ")VALUES(" + values + ")";
        }
        if (!sql.equals("")) db.execSQL(sql);
        if (close) this.close();
    }

    public void inup(String table, String... pairs) {
        inup(false, table, pairs);
    }

    public void insert(String table, String... pairs) {
        inup(true, table, pairs);
    }

    public void execSQL(String sql) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(sql);
        if (close) this.close();
    }

    public static int calculateAge(String birthday) throws ParseException {
        String s_today = (new SimpleDateFormat("yyyyMMdd", Locale.UK)).format(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime());
        Date md_today = (new SimpleDateFormat("MMdd", Locale.UK)).parse(s_today.substring(4));
        Date md_birthday = (new SimpleDateFormat("MMdd", Locale.UK)).parse(birthday.substring(4));
        if (md_today.after(md_birthday))
            return Integer.parseInt(s_today.substring(0, 4)) - Integer.parseInt(birthday.substring(0, 4));
        else
            return Integer.parseInt(s_today.substring(0, 4)) - Integer.parseInt(birthday.substring(0, 4)) - 1;
    }

    static String getNiceDate(String timestamp) throws ParseException {
        String s_today = (new SimpleDateFormat("yyyyMMddHHmmss", Locale.UK)).format(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime());
        Date dtoday = (new SimpleDateFormat("yyyyMMddHHmmss", Locale.UK)).parse(s_today);
        Date dthen = (new SimpleDateFormat("yyyyMMddHHmmss", Locale.UK)).parse(timestamp);
        Calendar ctoday = Calendar.getInstance();
        ctoday.setTime(dtoday);
        Calendar cthen = Calendar.getInstance();
        cthen.setTime(dthen);
        if (ctoday.get(Calendar.DAY_OF_YEAR) == cthen.get(Calendar.DAY_OF_YEAR) && ctoday.get(Calendar.YEAR) == cthen.get(Calendar.YEAR)) {
            DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
            return df.format(dthen);
        } else if (ctoday.get(Calendar.DAY_OF_YEAR) == (cthen.get(Calendar.DAY_OF_YEAR) + 1) && ctoday.get(Calendar.YEAR) == cthen.get(Calendar.YEAR)) {
            return "Yesterday";
        } else {
            DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
            return df.format(dthen);
        }
    }

    public static String getTimestamp() {
        return new SimpleDateFormat("yyyyMMddHHmmss", Locale.UK).format(Calendar.getInstance().getTime());
    }

}