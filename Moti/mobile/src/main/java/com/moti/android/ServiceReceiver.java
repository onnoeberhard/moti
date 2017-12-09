package com.moti.android;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ServiceReceiver extends BroadcastReceiver {

    public static final int NEW_MESSAGES_RECEIVED = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getExtras().containsKey("a") && intent.getExtras().getInt("a") == NEW_MESSAGES_RECEIVED) {
            new LocalDatabaseHandler(context, true).inup(LocalDatabaseHandler.NEW_CHAT_MESSAGES, LocalDatabaseHandler.DELETE);
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(0);
        } else
            context.startService(new Intent(context, MainService.class));
    }
}