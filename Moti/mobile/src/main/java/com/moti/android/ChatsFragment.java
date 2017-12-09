package com.moti.android;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;

public class ChatsFragment extends Fragment {

    public static String RECEIVE = "MOTI_CHATS_FRAGMENT_RECEIVE";

    MainActivity mainActivity;

    boolean created = false;

    LocalDatabaseHandler ldb;

    RecyclerView recycler;
    View nochatsview;

    @Override
    public void onResume() {
        super.onResume();
        updateList();
        mainActivity.registerReceiver(receiver, new IntentFilter(RECEIVE));
    }

    @Override
    public void onPause() {
        super.onPause();
        mainActivity.unregisterReceiver(receiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainActivity = (MainActivity) getActivity();
        ldb = new LocalDatabaseHandler(mainActivity);
        created = true;
        View v = inflater.inflate(R.layout.f_chats, container, false);
        nochatsview = v.findViewById(R.id.nochats_view);
        recycler = (RecyclerView) v.findViewById(R.id.chatsRecycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mainActivity);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recycler.setLayoutManager(layoutManager);
        recycler.setAdapter(new ChatsAdapter());
        return v;
    }

    public void updateList() {
        if (recycler != null) {
            ((ChatsAdapter) recycler.getAdapter()).update();
            recycler.getAdapter().notifyDataSetChanged();
        }
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateList();
        }
    };

    public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ViewHolder> {

        ArrayList<ChatItem> items = new ArrayList<>();

        public ChatsAdapter() {
            update();
        }

        public void update() {
            items.clear();
            ArrayList<String> uids = new ArrayList<>();
            for (String id : ldb.getAll(LocalDatabaseHandler.CHAT, LocalDatabaseHandler.CHAT_UID))
                if (!uids.contains(id))
                    uids.add(id);
            ArrayList<String> names = new ArrayList<>();
            ArrayList<String> texts = new ArrayList<>();
            ArrayList<String> dates = new ArrayList<>();
            ArrayList<String> realdates = new ArrayList<>();
            ArrayList<Integer> whos = new ArrayList<>();
            ArrayList<Boolean> bolds = new ArrayList<>();
            for (String id : uids) {
                names.add(ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.CONTACTS_UID, id, LocalDatabaseHandler.CONTACTS_NAME));
                ArrayList<String> atexts = ldb.getAll(LocalDatabaseHandler.CHAT, LocalDatabaseHandler.CHAT_TEXT, LocalDatabaseHandler.CHAT_UID, id);
                texts.add(atexts.get(atexts.size() - 1));
                ArrayList<String> adates = ldb.getAll(LocalDatabaseHandler.CHAT, LocalDatabaseHandler.CHAT_TIME, LocalDatabaseHandler.CHAT_UID, id);
                realdates.add(adates.get(adates.size() - 1));
                try {
                    dates.add(LocalDatabaseHandler.getNiceDate(adates.get(adates.size() - 1)));
                } catch (ParseException e) {
                    dates.add("");
                    e.printStackTrace();
                }
                ArrayList<String> awhos = ldb.getAll(LocalDatabaseHandler.CHAT, LocalDatabaseHandler.CHAT_WHO, LocalDatabaseHandler.CHAT_UID, id);
                whos.add(Integer.parseInt(awhos.get(awhos.size() - 1)));
                bolds.add(ldb.getAll(LocalDatabaseHandler.NEW_CHAT_MESSAGES, LocalDatabaseHandler.NEW_CHAT_MESSAGES_UID).contains(id));
            }
            for (int i = 0; i < uids.size(); i++) {
                ChatItem item = new ChatItem(uids.get(i), names.get(i), texts.get(i), dates.get(i), realdates.get(i), whos.get(i), bolds.get(i));
                items.add(item);
            }
            Collections.sort(items);
            Collections.reverse(items);
            if (items.size() == 0)
                nochatsview.setVisibility(View.VISIBLE);
            else
                nochatsview.setVisibility(View.GONE);
        }

        public class ChatItem implements Comparable<ChatItem> {
            String uid;
            String name;
            String text;
            String date;
            String realdate;
            int who;
            boolean bold;

            public ChatItem(String _uid, String _name, String _text, String _date, String _realdate, int _who, boolean _bold) {
                super();
                uid = _uid;
                name = _name;
                text = _text;
                date = _date;
                realdate = _realdate;
                who = _who;
                bold = _bold;
            }

            @Override
            public int compareTo(@NonNull ChatItem another) {
                return realdate.compareTo(another.realdate);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.v_chats_item, parent, false);
            return new ViewHolder(v, viewType);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            if (items.size() > 0) {
                holder.nametv.setText(items.get(position).name);
                holder.msgtv.setText(items.get(position).text);
                holder.datetv.setText(items.get(position).date);
                if (items.get(position).bold) {
                    holder.msgtv.setTypeface(null, Typeface.BOLD);
                    holder.datetv.setTypeface(null, Typeface.BOLD);
                } else {
                    holder.msgtv.setTypeface(null, Typeface.NORMAL);
                    holder.datetv.setTypeface(null, Typeface.NORMAL);
                }
                holder.ll.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(mainActivity, ChatActivity.class);
                        i.putExtra("uid", items.get(position).uid);
                        startActivity(i);
                    }
                });
                holder.ll.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        new AlertDialog.Builder(mainActivity).setMessage("Do you really want to delete this chat?")
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ldb.inup(LocalDatabaseHandler.CHAT, LocalDatabaseHandler.CHAT_UID, items.get(position).uid, LocalDatabaseHandler.DELETE);
                                        ldb.inup(LocalDatabaseHandler.NEW_CHAT_MESSAGES, LocalDatabaseHandler.NEW_CHAT_MESSAGES_UID, items.get(position).uid, LocalDatabaseHandler.DELETE);
                                        update();
                                        notifyDataSetChanged();
                                        mainActivity.updateChatsBadge();
                                    }
                                })
                                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).create().show();
                        return true;
                    }
                });
            }
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            int type;

            TextView nametv;
            TextView msgtv;
            TextView datetv;
            ImageView iv;
            LinearLayout ll;

            public ViewHolder(View itemView, int viewType) {
                super(itemView);
                type = viewType;
                ll = (LinearLayout) itemView.findViewById(R.id.ll);
                nametv = (TextView) itemView.findViewById(R.id.nametv);
                iv = (ImageView) itemView.findViewById(R.id.profiv);
                msgtv = (TextView) itemView.findViewById(R.id.msgtv);
                datetv = (TextView) itemView.findViewById(R.id.datetv);
            }
        }
    }

}
