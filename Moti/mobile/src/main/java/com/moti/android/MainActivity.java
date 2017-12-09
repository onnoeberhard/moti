package com.moti.android;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final String RECEIVE_CHAT = "MOTI_MAIN_ACTIVITY_RECEIVE_CHAT";

    public static final String RECEIVE_VISBILITY = "MOTI_RECEIVE_VISIBILITY";

    public static final String LOCATION_LAT = "locationLat";
    public static final String LOCATION_LNG = "locationLng";
    public static final String LOCATION_UPDATE_TIME = "locationUpdateTime";
    public static final String VISIBLE = "visible";
    public static final String UID = "uid";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String NAME = "name";
    public static final String BIRTHDAY = "birthday";
    public static final String GENDER = "gender";
    public static final String RADIUS = "radius";
    public static final String TOLERANCE = "tolerance";
    public static final String PROFVIS = "profvis";
    public static final String ANALYTICS = "analytics";

    SharedPreferences sp;
    LocalDatabaseHandler ldb;

    public static MainActivity mainActivity;

    HomeFragment homeFragment;
    ChatsFragment chatsFragment;
    ProfileFragment profileFragment;

    MyViewPager viewPager;
    TabLayout tabLayout;
    FloatingActionButton fab;
    Menu menu;

    boolean online = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = this;
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
        setContentView(R.layout.a_main);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        ldb = new LocalDatabaseHandler(this);
        online = OnlineDatabaseHandler.isConnectedToInternet(this);
        if (!sp.contains(UID)) {
            gotoLR();
        } else {
            new OnlineDatabaseHandler(this).login(new OnlineDatabaseHandler.WebDbUser() {
                @Override
                public void onResult(JSONObject json) {
                    try {
                        if (json.getString("result").equals("*OK*")) {
                            String fcm_token = FirebaseInstanceId.getInstance().getToken();
                            if (!json.getJSONObject("row").getString("fcm_rid").equals(fcm_token))
                                new OnlineDatabaseHandler(MainActivity.this).inup(null, OnlineDatabaseHandler.USERS, OnlineDatabaseHandler.USERS_ID, PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString(MainActivity.UID, ""), OnlineDatabaseHandler.USERS_FCM_RID, fcm_token);
                        } else {
                            sp.edit().remove(UID).remove(USERNAME).remove(PASSWORD).remove(NAME).remove(BIRTHDAY).remove(GENDER).apply();
                            gotoLR();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, sp.getString(USERNAME, ""), sp.getString(PASSWORD, ""));
            gotoMain();
        }
    }

    public void gotoLR() {
        startActivity(new Intent(this, WelcomeLRActivity.class));
        finish();
    }

    public void gotoMain() {
        startService(new Intent(this, MainService.class));
        final Toolbar mainToolbar = (Toolbar) findViewById(R.id.mainToolbar);
        setSupportActionBar(mainToolbar);
        homeFragment = new HomeFragment();
        chatsFragment = new ChatsFragment();
        profileFragment = new ProfileFragment();
        boolean gotochat = getIntent().hasExtra("a") && getIntent().getStringExtra("a").equals("GOTO_CHATS");
        viewPager = (MyViewPager) findViewById(R.id.mainViewPager);
        viewPager.setMainActivity(this);
        viewPager.setOffscreenPageLimit(2);
        viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:
                        return profileFragment;
                    case 1:
                        return homeFragment;
                    case 2:
                        return chatsFragment;
                }
                return null;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                switch (position) {
                    case 0:
                        return "Profile";
                    case 1:
                        return "Home";
                    case 2:
                        return "Chats";
                }
                return null;
            }

            @Override
            public int getCount() {
                return 3;
            }
        });
        tabLayout = (TabLayout) findViewById(R.id.mainTabs);
        tabLayout.setupWithViewPager(viewPager);
        for (int i = 0; i < tabLayout.getTabCount(); i++)
            tabLayout.getTabAt(i).setCustomView(new MyTabView(this, tabLayout.getTabAt(i)));
        updateChatsBadge();
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                ((MyTabView) tab.getCustomView()).select();
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                ((MyTabView) tab.getCustomView()).unselect();
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        viewPager.setCurrentItem(gotochat ? 2 : 1);
        fab = (FloatingActionButton) findViewById(R.id.mainFab);
        fab.setImageResource(R.drawable.notifications_small);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (viewPager.getCurrentItem()) {
                    case 0:
                        profileFragment.fabClick();
                        break;
                    case 1:
                        homeFragment.fabClick();
                        break;
                }
            }
        });
        updateOfflineMode();
    }

    public void updateOfflineMode() {
        online = OnlineDatabaseHandler.isConnectedToInternet(this);
        if (fab != null && viewPager != null)
            fab.setVisibility(online ? viewPager.getCurrentItem() < 2 ? View.VISIBLE : View.GONE : View.GONE);
        if (menu != null)
            for (int i = 0; i < menu.size(); i++)
                menu.getItem(i).setVisible(online);
    }

    public class MyTabView extends RelativeLayout {
        ImageView badge;

        public MyTabView(Context context, TabLayout.Tab tab) {
            super(context);
            inflate(context, R.layout.v_tab, this);
            TextView title = (TextView) findViewById(R.id.title);
            title.setText(tab.getText());
            setAlpha(tab.isSelected() ? 1f : 0.5f);
            badge = (ImageView) findViewById(R.id.badge);
        }

        public void select() {
            setAlpha(1f);
        }

        public void unselect() {
            setAlpha(0.5f);
        }

        public void setBadgeNumber(int n) {
            if (n > 0) {
                badge.setVisibility(VISIBLE);
                switch (n) {
                    case 1:
                        badge.setImageResource(R.drawable.badge_1);
                        break;
                    case 2:
                        badge.setImageResource(R.drawable.badge_2);
                        break;
                    case 3:
                        badge.setImageResource(R.drawable.badge_3);
                        break;
                    case 4:
                        badge.setImageResource(R.drawable.badge_4);
                        break;
                    case 5:
                        badge.setImageResource(R.drawable.badge_5);
                        break;
                    case 6:
                        badge.setImageResource(R.drawable.badge_6);
                        break;
                    case 7:
                        badge.setImageResource(R.drawable.badge_7);
                        break;
                    case 8:
                        badge.setImageResource(R.drawable.badge_8);
                        break;
                    case 9:
                        badge.setImageResource(R.drawable.badge_9);
                        break;
                    default:
                        badge.setImageResource(R.drawable.badge_9p);
                        break;
                }
            } else
                badge.setVisibility(GONE);
        }
    }

    public static class MyViewPager extends ViewPager {
        MainActivity mainActivity = null;

        public MyViewPager(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public void setMainActivity(MainActivity activity) {
            mainActivity = activity;
        }

        @Override
        public void setCurrentItem(final int item) {
            super.setCurrentItem(item);
            if (item == 2) {
                RecyclerView recycler = mainActivity.chatsFragment.recycler;
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        RecyclerView recycler = mainActivity.chatsFragment.recycler;
                        if (recycler == null)
                            handler.postDelayed(this, 5);
                        else {
                            LinearLayoutManager llm = (LinearLayoutManager) recycler.getLayoutManager();
                            if (llm.findLastCompletelyVisibleItemPosition() == recycler.getAdapter().getItemCount() - 1)
                                mainActivity.toggleToolbarScrolling(false);
                            else
                                mainActivity.toggleToolbarScrolling(true);
                        }
                    }
                }, recycler == null ? 5 : 0);
            } else
                mainActivity.toggleToolbarScrolling(false);
            final FloatingActionButton fab = mainActivity.fab;
            if (fab != null && mainActivity.online) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        final int cx = fab.getMeasuredWidth() / 2;
                        final int cy = fab.getMeasuredHeight() / 2;
                        int initialRadius = fab.getWidth() / 2;
                        final int finalRadius = Math.max(fab.getWidth(), fab.getHeight()) / 2;
                        switch (getCurrentItem()) {
                            case 0:
                            case 1: {
                                Animator anim1 = ViewAnimationUtils.createCircularReveal(fab, cx, cy, initialRadius, 0);
                                anim1.addListener(new AnimatorListenerAdapter() {
                                    @Override
                                    @SuppressLint("NewApi")
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        fab.setVisibility(View.INVISIBLE);
                                        fab.setImageResource(getCurrentItem() == 0 ? R.drawable.badge_9p :
                                                R.drawable.notifications_small);
                                        Animator anim2 = ViewAnimationUtils.createCircularReveal(fab, cx, cy, 0, finalRadius);
                                        fab.setVisibility(View.VISIBLE);
                                        anim2.setDuration(100);
                                        anim2.start();
                                    }
                                });
                                anim1.setDuration(100);
                                anim1.start();
                                break;
                            }
                            case 2:
                                Animator anim1 = ViewAnimationUtils.createCircularReveal(fab, cx, cy, initialRadius, 0);
                                anim1.addListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        fab.setVisibility(View.INVISIBLE);
                                    }
                                });
                                anim1.setDuration(100);
                                anim1.start();
                                break;
                        }
                    } catch (IllegalStateException e) {
                        boringFABChange(fab);
                    }
                } else
                    boringFABChange(fab);
            }
        }

        public void boringFABChange (FloatingActionButton fab) {
            switch (getCurrentItem()) {
                case 0:
                    fab.setImageResource(R.drawable.badge_9p);
                    fab.setVisibility(View.VISIBLE);
                    break;
                case 1:
                    fab.setImageResource(R.drawable.notifications_small);
                    fab.setVisibility(View.VISIBLE);
                    break;
                case 2:
                    fab.setVisibility(View.INVISIBLE);
                    break;
            }
        }

    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() != 1)
            viewPager.setCurrentItem(1);
        else
            super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        this.menu = menu;
        updateOfflineMode();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                viewPager.setCurrentItem(1);
                break;
            case R.id.mps:
                startActivity(new Intent(this, MPMapActivity.class));
                break;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void toggleToolbarScrolling(boolean on) {
        Toolbar mToolbar = (Toolbar) findViewById(R.id.mainToolbar);
        AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.mainAppBar);
        AppBarLayout.LayoutParams toolbarLayoutParams = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
        toolbarLayoutParams.setScrollFlags(on ? AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS : 0);
        mToolbar.setLayoutParams(toolbarLayoutParams);
        CoordinatorLayout.LayoutParams appBarLayoutParams = (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
        appBarLayoutParams.setBehavior(on ? new AppBarLayout.Behavior() : null);
        appBarLayout.setLayoutParams(appBarLayoutParams);
    }

    public void updateChatsBadge() {
        if (tabLayout != null) {
            ArrayList<String> uids = new ArrayList<>();
            for (String uid : ldb.getAll(LocalDatabaseHandler.NEW_CHAT_MESSAGES, LocalDatabaseHandler.NEW_CHAT_MESSAGES_UID))
                if (!uids.contains(uid))
                    uids.add(uid);
            ((MyTabView) tabLayout.getTabAt(2).getCustomView()).setBadgeNumber(uids.size());
        }
    }

    public static boolean isVisible = false;

    @Override
    protected void onPause() {
        super.onPause();
        isVisible = false;
        try {
            unregisterReceiver(chat_receiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isVisible = true;
        registerReceiver(chat_receiver, new IntentFilter(RECEIVE_CHAT));
        updateChatsBadge();
        updateOfflineMode();
    }

    BroadcastReceiver chat_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateChatsBadge();
            Toast.makeText(MainActivity.this, "New Message!", Toast.LENGTH_SHORT).show();
        }
    };
}
