package com.moti.android;

import android.animation.LayoutTransition;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

import java.util.ArrayList;


public class HomeFragment extends Fragment {

    public static final String RECEIVE_UPDATE = "MOTI_HOME_FRAGMENT_RECEIVE_UPDATE";

    MainActivity mainActivity;
    LocalDatabaseHandler ldb;
    SharedPreferences sp;

    NoInternetItem noInternetItem;
    NoLocationItem noLocationItem;
    NoVisibilityItem noVisibilityItem;
    MainItem mainItem;

    HomeItem[] items;

    LinearLayout container;
    LayoutTransition containerTransition;

    boolean location_available = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup vg,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.f_home, vg, false);
    }

    SwipeRefreshLayout refresh;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        ldb = new LocalDatabaseHandler(mainActivity);
        sp = PreferenceManager.getDefaultSharedPreferences(mainActivity);
        container = (LinearLayout) getView().findViewById(R.id.container);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            container.getLayoutTransition().disableTransitionType(LayoutTransition.DISAPPEARING);
            container.getLayoutTransition().disableTransitionType(LayoutTransition.APPEARING);
        }
        containerTransition = container.getLayoutTransition();
        refresh = (SwipeRefreshLayout) getView().findViewById(R.id.swiperefresh);
        refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
        final ScrollView scrollView = (ScrollView) getView().findViewById(R.id.scrollView);
        scrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                refresh.setEnabled(scrollView.getScrollY() == 0);
            }
        });
        noInternetItem = new NoInternetItem();
        noLocationItem = new NoLocationItem();
        noVisibilityItem = new NoVisibilityItem();
        mainItem = new MainItem();
        items = new HomeItem[]{noInternetItem, noLocationItem, noVisibilityItem, mainItem};
        update();
    }

    public void refresh() {
        mainActivity.sendBroadcast(new Intent(MainService.RECEIVE_UPDATE));
        update();
        mainActivity.updateOfflineMode();
        refresh.setRefreshing(false);
    }

    public void fabClick() {
        startActivity(new Intent(mainActivity, SearchActivity.class));
    }

    @Override
    public void onResume() {
        super.onResume();
        update();
        mainActivity.registerReceiver(update_receiver, new IntentFilter(RECEIVE_UPDATE));
    }

    @Override
    public void onPause() {
        super.onPause();
        mainActivity.unregisterReceiver(update_receiver);
    }

    BroadcastReceiver update_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            update();
        }
    };

    public void update() {
        if (items != null)
            for (HomeItem item : items)
                item.update();
    }

    public class HomeItem {
        View layout;
        View view;
        boolean initialized = false;
        boolean visible = false;

        public HomeItem(int id) {
            layout = HomeFragment.this.getView();
            view = layout.findViewById(id);
        }

        public void update() {
        }

        public void update(boolean show) {
            if (show)
                appear();
            else
                disappear();
            initialized = true;
        }

        public void appear() {
            if (!visible) {
                if (initialized) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        view.setVisibility(View.VISIBLE);
                        Animation a = new ScaleAnimation(0, 1, 0, 1, ScaleAnimation.RELATIVE_TO_SELF, .5f, ScaleAnimation.RELATIVE_TO_SELF, .5f);
                        a.setDuration(400);
                        view.startAnimation(a);
                    } else {
                        view.setVisibility(View.VISIBLE);
                    }
                } else {
                    container.setLayoutTransition(null);
                    view.setVisibility(View.VISIBLE);
                    container.setLayoutTransition(containerTransition);
                }
            }
            visible = true;
        }

        public void disappear() {
            if (visible) {
                if (initialized) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        Animation a = new ScaleAnimation(1, 0, 1, 0, ScaleAnimation.RELATIVE_TO_SELF, .5f, ScaleAnimation.RELATIVE_TO_SELF, .5f);
                        a.setDuration(400);
                        a.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                view.setVisibility(View.GONE);
                                view.clearAnimation();
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }
                        });
                        view.startAnimation(a);
                    } else {
                        view.setVisibility(View.GONE);
                    }
                } else {
                    container.setLayoutTransition(null);
                    view.setVisibility(View.GONE);
                    container.setLayoutTransition(containerTransition);
                }
            }
            visible = false;
        }
    }

    public class NoInternetItem extends HomeItem {
        Button retry;

        public NoInternetItem() {
            super(R.id.nointernet_view);
            retry = (Button) layout.findViewById(R.id.nointernet_retry);
            retry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    HomeFragment.this.update();
                    mainActivity.updateOfflineMode();
                }
            });
        }

        @Override
        public void update() {
            super.update(!OnlineDatabaseHandler.isConnectedToInternet(mainActivity));
        }
    }

    public class NoLocationItem extends HomeItem {
        Button retry;

        public NoLocationItem() {
            super(R.id.nolocation_view);
            retry = (Button) layout.findViewById(R.id.nolocation_retry);
            retry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    refresh();
                }
            });
        }

        @Override
        public void update() {
            super.update(!location_available);
        }
    }

    public class NoVisibilityItem extends HomeItem {
        Button button;


        public NoVisibilityItem() {
            super(R.id.novisibility_view);
            button = (Button) layout.findViewById(R.id.novisibility_button);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sp.edit().putBoolean(MainActivity.VISIBLE, true).apply();
                    mainActivity.sendBroadcast(new Intent(MainActivity.RECEIVE_VISBILITY));
                    HomeFragment.this.update();
                }
            });
        }

        @Override
        public void update() {
            super.update(OnlineDatabaseHandler.isConnectedToInternet(mainActivity) && location_available && !sp.getBoolean(MainActivity.VISIBLE, true));
        }
    }

    public class MainItem extends HomeItem {
        TextView title;
        View primeView;
        TextView quicktt;
        Button visibility;

        GoogleMap googleMap;

        public MainItem() {
            super(R.id.main_view);
            visibility = (Button) layout.findViewById(R.id.main_visibility);
            primeView = layout.findViewById(R.id.main_primeView);
            title = (TextView) layout.findViewById(R.id.main_title);
            quicktt = (TextView) layout.findViewById(R.id.main_quicktt);
            SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.main_map);
            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap map) {
                    googleMap = map;
                    googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                            .target(new LatLng(Double.parseDouble(sp.getString(MainActivity.LOCATION_LAT, "0")), Double.parseDouble(sp.getString(MainActivity.LOCATION_LNG, "0"))))
                            .zoom(14.5f)
                            .build()));
                    googleMap.getUiSettings().setAllGesturesEnabled(false);
                }
            });
            visibility.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sp.edit().putBoolean(MainActivity.VISIBLE, false).putString(MainActivity.LOCATION_LAT, "0").putString(MainActivity.LOCATION_LNG, "0").apply();
                    new OnlineDatabaseHandler(mainActivity).inup(null, OnlineDatabaseHandler.USERS, OnlineDatabaseHandler.USERS_ID, sp.getString(MainActivity.UID, ""), OnlineDatabaseHandler.USERS_LOCATION, "");
                    mainActivity.sendBroadcast(new Intent(MainActivity.RECEIVE_VISBILITY));
                    HomeFragment.this.update();
                }
            });
            primeView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mainActivity.startActivity(new Intent(mainActivity, SearchActivity.class));
                }
            });
        }

        String ut = "";
        int un = 0;

        @Override
        public void update() {
            boolean show = OnlineDatabaseHandler.isConnectedToInternet(mainActivity) && location_available && sp.getBoolean(MainActivity.VISIBLE, true);
            if (show) {
                int near_users = 0;
                ArrayList<String> near_mps = new ArrayList<>();
                for (String id : ldb.getAll(LocalDatabaseHandler.NEAR, LocalDatabaseHandler.NEAR_ID)) {
                    if (id.startsWith("u"))
                        near_users++;
                    else if (id.startsWith("m"))
                        near_mps.add(id);
                }
                title.setText(near_mps.size() > 0 && near_users > 0 ? "There are " + Integer.toString(near_mps.size()) + " MeetingPoints and " + Integer.toString(near_users) + " visible users near you" :
                near_mps.size() > 0 ? "There are " + Integer.toString(near_mps.size()) + " MeetingPoints near you" : near_users > 0 ? "There are " + Integer.toString(near_users) + " visible users near you" :
                "There are currently no visible users near you");
                String quickttstr = "";
                for (String mp : near_mps)
                    quickttstr += "\n" + mp;
                if (!ut.equals(Long.toString(sp.getLong(MainActivity.LOCATION_UPDATE_TIME, 0))))
                    un++;
                ut = Long.toString(sp.getLong(MainActivity.LOCATION_UPDATE_TIME, 0));
                quicktt.setText(sp.getString(MainActivity.LOCATION_LAT, "") + "," + sp.getString(MainActivity.LOCATION_LNG, "") + " - " + ut + "; " + Integer.toString(un) + quickttstr);
                if (googleMap != null)
                    googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                            .target(new LatLng(Double.parseDouble(sp.getString(MainActivity.LOCATION_LAT, "")), Double.parseDouble(sp.getString(MainActivity.LOCATION_LNG, ""))))
                            .zoom(14.5f)
                            .build()));
            }
            super.update(show);
        }
    }

}
