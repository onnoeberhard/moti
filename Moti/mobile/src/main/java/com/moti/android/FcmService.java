package com.moti.android;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FcmService extends FirebaseMessagingService {

    LocalDatabaseHandler ldb;

    @Override
    public void onMessageReceived(RemoteMessage message) {
        ldb = new LocalDatabaseHandler(this);
        Map<String, String> _data = message.getData();
        ArrayList<Map<String, String>> datas = new ArrayList<>();
        for (int i = 0; i < _data.get("message").split(",").length; i++) {
            Map<String, String> map = new HashMap<>();
            for (String key : _data.keySet())
                map.put(key, _data.get(key).split(",")[i]);
            datas.add(map);
        }
        for (final Map<String, String> data : datas) {
            if (data.get("message").equals("chat")) {
                final String uid = data.get("uid");
                String text = data.get("text");
                ldb.insert(LocalDatabaseHandler.CHAT,
                        LocalDatabaseHandler.CHAT_UID, uid,
                        LocalDatabaseHandler.CHAT_TEXT, text,
                        LocalDatabaseHandler.CHAT_TIME, LocalDatabaseHandler.getTimestamp(),
                        LocalDatabaseHandler.CHAT_CONTEXT, (data.containsKey("context") ? data.get("context") : ""),
                        LocalDatabaseHandler.CHAT_WHO, LocalDatabaseHandler.CHAT_WHO_THEM);
                ldb.insert(LocalDatabaseHandler.NEW_CHAT_MESSAGES,
                        LocalDatabaseHandler.NEW_CHAT_MESSAGES_UID, uid,
                        LocalDatabaseHandler.NEW_CHAT_MESSAGES_TEXT, text,
                        LocalDatabaseHandler.NEW_CHAT_MESSAGES_TIME, LocalDatabaseHandler.getTimestamp());
                if (ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.CONTACTS_UID, uid, LocalDatabaseHandler.CONTACTS_NAME).equals(LocalDatabaseHandler.FALSE)) {
                    new OnlineDatabaseHandler(this).get(new OnlineDatabaseHandler.WebDbUser() {
                        @Override
                        public void onResult(JSONObject json) {
                            try {
                                ldb.inup(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.CONTACTS_UID, uid, LocalDatabaseHandler.CONTACTS_NAME, json.getString("value"));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            update_chats(data);
                        }
                    }, OnlineDatabaseHandler.USERS, OnlineDatabaseHandler.USERS_ID, uid, OnlineDatabaseHandler.USERS_NAME);
                } else
                    update_chats(data);
            } else if (data.get("message").equals("near")) {
                String id = data.get("near_id");
                ldb.inup(LocalDatabaseHandler.NEAR, LocalDatabaseHandler.NEAR_ID, id);
                sendBroadcast(new Intent(HomeFragment.RECEIVE_UPDATE));
            } else if (data.get("message").equals("not_near")) {
                String id = data.get("near_id");
                ldb.inup(LocalDatabaseHandler.NEAR, LocalDatabaseHandler.NEAR_ID, id, LocalDatabaseHandler.DELETE);
                sendBroadcast(new Intent(HomeFragment.RECEIVE_UPDATE));
            }
        }
    }

    public void update_chats(Map<String, String> data) {
        sendBroadcast(new Intent(ChatsFragment.RECEIVE));
        sendBroadcast(new Intent(MainActivity.RECEIVE_CHAT));
        sendBroadcast(new Intent(ChatActivity.RECEIVE));
        if (!MainActivity.isVisible && !ChatActivity.isVisible) {
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.notifications_small)
                    .setColor(getResources().getColor(R.color.colorPrimary))
                    .setAutoCancel(true)
                    .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);
            ArrayList<String> uids = ldb.getAll(LocalDatabaseHandler.NEW_CHAT_MESSAGES, LocalDatabaseHandler.NEW_CHAT_MESSAGES_UID);
            ArrayList<String> duids = new ArrayList<>();
            for (String uid : uids)
                if (!duids.contains(uid))
                    duids.add(uid);
            if (uids.size() > 1) {
                NotificationCompat.InboxStyle list = new NotificationCompat.InboxStyle();
                ArrayList<CharSequence> prelist = new ArrayList<>();
                if (duids.size() > 1) {
                    for (Map<String, String> row : ldb.getAll(LocalDatabaseHandler.NEW_CHAT_MESSAGES)) {
                        String name = ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.CONTACTS_UID, row.get(LocalDatabaseHandler.NEW_CHAT_MESSAGES_UID), LocalDatabaseHandler.CONTACTS_NAME);
                        Spannable s = new SpannableString(name + ": " + row.get(LocalDatabaseHandler.NEW_CHAT_MESSAGES_TEXT));
                        s.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, name.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        prelist.add(s);
                    }
                    String title = "";
                    for (int i = 0; i < duids.size(); i++) {
                        if (i > 0)
                            title += ", ";
                        title += ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.CONTACTS_UID, duids.get(i), LocalDatabaseHandler.CONTACTS_NAME);
                    }
                    mBuilder.setContentTitle(title);
                } else {
                    for (String s : ldb.getAll(LocalDatabaseHandler.NEW_CHAT_MESSAGES, LocalDatabaseHandler.NEW_CHAT_MESSAGES_TEXT))
                        prelist.add(s);
                    mBuilder.setContentTitle(ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.CONTACTS_UID, uids.get(0), LocalDatabaseHandler.CONTACTS_NAME));
                }
                Collections.reverse(prelist);
                for (CharSequence x : prelist)
                    list.addLine(x);
                mBuilder.setContentText(Integer.toString(uids.size()) + " Messages")
                        .setStyle(list);
            } else {
                mBuilder.setContentTitle(ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.CONTACTS_UID, uids.get(0), LocalDatabaseHandler.CONTACTS_NAME))
                        .setContentText(data.get("text"))
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(data.get("text")));
            }
            Intent resultIntent = new Intent(this, duids.size() == 1 ? ChatActivity.class : MainActivity.class);
            resultIntent.putExtra(duids.size() == 1 ? "uid" : "a", duids.size() == 1 ? uids.get(0) : "GOTO_CHATS");
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addParentStack(duids.size() == 1 ? ChatActivity.class : MainActivity.class);
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            Intent readIntent = new Intent(this, ServiceReceiver.class);
            readIntent.putExtra("a", ServiceReceiver.NEW_MESSAGES_RECEIVED);
            PendingIntent readPendingIntent = PendingIntent.getBroadcast(this, 0, readIntent, 0);
            mBuilder.setContentIntent(resultPendingIntent)
                    .addAction(0, "Seen", readPendingIntent)
                    .addAction(0, "Open", resultPendingIntent);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(0, mBuilder.build());
        }
    }
}
