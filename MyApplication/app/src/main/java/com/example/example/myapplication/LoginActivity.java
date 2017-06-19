package com.example.example.myapplication;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.example.example.myapplication.adapters.LoginPageAdapter;
import com.example.example.myapplication.remote.DynRH2LevClientWrapper;
import com.loopj.android.http.PersistentCookieStore;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private PersistentCookieStore cookies;

    @BindView(R.id.login_tab_layout) TabLayout _tabLayout;
    @BindView(R.id.login_view_pager) ViewPager _viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.cookies = new PersistentCookieStore(this);
        DynRH2LevClientWrapper.client.setCookieStore(this.cookies);
        setContentView(R.layout.activity_login_full);
        ButterKnife.bind(this);
        _tabLayout.addTab(_tabLayout.newTab().setText("Login Cloud"));
        _tabLayout.addTab(_tabLayout.newTab().setText("Login Local"));
        LoginPageAdapter adapter = new LoginPageAdapter(getSupportFragmentManager(), _tabLayout.getTabCount());
        _viewPager.setAdapter(adapter);
        _viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(_tabLayout));
        _tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                _viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true);
    }

}
