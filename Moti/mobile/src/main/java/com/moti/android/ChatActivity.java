package com.moti.android;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {

    public static String RECEIVE = "MOTI_CHAT_ACTIVITY_RECEIVE";

    LocalDatabaseHandler ldb;
    SharedPreferences sp;

    public static ChatActivity chatActivity;

    RecyclerView chatRecycler;

    public static boolean isVisible = false;

    String uid;

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        isVisible = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isVisible = true;
        registerReceiver(receiver, new IntentFilter(RECEIVE));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        chatActivity = this;
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(0);
        setContentView(R.layout.a_chat);
        uid = getIntent().getStringExtra("uid");
        ldb = new LocalDatabaseHandler(this);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.CONTACTS_UID, uid, LocalDatabaseHandler.CONTACTS_NAME));
        chatRecycler = (RecyclerView) findViewById(R.id.chatrecycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        chatRecycler.setLayoutManager(layoutManager);
        chatRecycler.setAdapter(new ChatAdapter(uid));
        final EditText cet = (EditText) findViewById(R.id.cet);
        ImageButton sendButton = (ImageButton) findViewById(R.id.send);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!cet.getText().toString().equals("") && OnlineDatabaseHandler.isConnectedToInternet(ChatActivity.this)) {
                    new OnlineDatabaseHandler(ChatActivity.this).sendChatMessage(new OnlineDatabaseHandler.WebDbUser() {
                        @Override
                        public void onResult(JSONObject json) {
                            ldb.insert(LocalDatabaseHandler.CHAT,
                                    LocalDatabaseHandler.CHAT_UID, uid,
                                    LocalDatabaseHandler.CHAT_TEXT, cet.getText().toString(),
                                    LocalDatabaseHandler.CHAT_TIME, LocalDatabaseHandler.getTimestamp(),
                                    LocalDatabaseHandler.CHAT_WHO, LocalDatabaseHandler.CHAT_WHO_ME);
                            cet.setText("");
                            updateList();
                        }
                    }, sp.getString(MainActivity.UID, ""), uid, cet.getText().toString());
                } else if (!OnlineDatabaseHandler.isConnectedToInternet(ChatActivity.this)) {
                    Toast.makeText(ChatActivity.this, "You have no internet connection!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void updateList() {
        ((ChatAdapter) chatRecycler.getAdapter()).update();
        chatRecycler.getAdapter().notifyDataSetChanged();
        chatRecycler.smoothScrollToPosition(chatRecycler.getAdapter().getItemCount() - 1);
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateList();
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

        static final int TYPE_MY_BUBBLE = 0;
        static final int TYPE_THEIR_BUBBLE = 1;

        ArrayList<ArrayList> items = new ArrayList<>();
        String uid;

        public ChatAdapter(String _uid) {
            uid = _uid;
            update();
        }

        public void update() {
            ldb.inup(LocalDatabaseHandler.NEW_CHAT_MESSAGES, LocalDatabaseHandler.NEW_CHAT_MESSAGES_UID, uid, LocalDatabaseHandler.DELETE);
            items = new ArrayList<>();
            items.clear();
            ArrayList<String> senders = ldb.getAll(LocalDatabaseHandler.CHAT, LocalDatabaseHandler.CHAT_WHO, LocalDatabaseHandler.CHAT_UID, uid);
            ArrayList<String> texts = ldb.getAll(LocalDatabaseHandler.CHAT, LocalDatabaseHandler.CHAT_TEXT, LocalDatabaseHandler.CHAT_UID, uid);
            ArrayList<String> times = ldb.getAll(LocalDatabaseHandler.CHAT, LocalDatabaseHandler.CHAT_TIME, LocalDatabaseHandler.CHAT_UID, uid);
            for (int i = 0; i < texts.size(); i++) {
                ArrayList<String> item = new ArrayList<>();
                item.add(senders.get(i));
                item.add(texts.get(i));
                item.add(times.get(i));
                items.add(item);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_MY_BUBBLE) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.v_chat_mybubble, parent, false);
                return new ViewHolder(v, viewType);
            } else if (viewType == TYPE_THEIR_BUBBLE) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.v_chat_theirbubble, parent, false);
                return new ViewHolder(v, viewType);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.mainText.setText((String) items.get(position).get(1));
            if (holder.type == TYPE_MY_BUBBLE) {
                holder.bubble.getBackground().setColorFilter(Color.parseColor("#CED7DB"), PorterDuff.Mode.MULTIPLY);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (items.get(position).get(0).equals(LocalDatabaseHandler.CHAT_WHO_ME))
                return TYPE_MY_BUBBLE;
            return TYPE_THEIR_BUBBLE;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            int type;

            TextView mainText;
            View bubble;

            public ViewHolder(View itemView, int viewType) {
                super(itemView);
                type = viewType;
                bubble = itemView.findViewById(R.id.bubble);
                mainText = (TextView) itemView.findViewById(R.id.mainText);
            }
        }
    }
}
