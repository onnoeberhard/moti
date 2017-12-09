package com.moti.android;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.Toast;

public class ProfileFragment extends Fragment {

    MainActivity mainActivity;
    LocalDatabaseHandler ldb;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainActivity = (MainActivity) getActivity();
        ldb = new LocalDatabaseHandler(mainActivity);
        View v = inflater.inflate(R.layout.f_profile, container, false);
        GridView grid = (GridView) v.findViewById(R.id.photos_grid);
        grid.setAdapter(new ListAdapter() {
            @Override
            public boolean areAllItemsEnabled() {
                return false;
            }

            @Override
            public boolean isEnabled(int position) {
                return false;
            }

            @Override
            public void registerDataSetObserver(DataSetObserver observer) {
            }

            @Override
            public void unregisterDataSetObserver(DataSetObserver observer) {
            }

            @Override
            public int getCount() {
                return 8;
            }

            @Override
            public Object getItem(int position) {
                return null;
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public boolean hasStableIds() {
                return false;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v;
                LayoutInflater inflater = (LayoutInflater)
                        mainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                if (position == getCount() - 1)
                    v = inflater.inflate(R.layout.v_profile_morephotos, null);
                else {
                    v = inflater.inflate(R.layout.v_profile_photo, null);
                    ((ImageView) v.findViewById(R.id.img)).setImageResource(R.drawable.profilepic);
                }
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(mainActivity, "Gallery", Toast.LENGTH_SHORT).show();
                    }
                });
                return v;
            }

            @Override
            public int getItemViewType(int position) {
                return position == 8 ? 1 : 0;
            }

            @Override
            public int getViewTypeCount() {
                return 2;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        });
        v.findViewById(R.id.profilepic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mainActivity, "Gallery", Toast.LENGTH_SHORT).show();
            }
        });
        v.findViewById(R.id.positive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mainActivity, "Postive Reviews", Toast.LENGTH_SHORT).show();
            }
        });
        v.findViewById(R.id.positive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mainActivity, "Negative Reviews", Toast.LENGTH_SHORT).show();
            }
        });
        return v;
    }

    public void fabClick() {

    }

}
