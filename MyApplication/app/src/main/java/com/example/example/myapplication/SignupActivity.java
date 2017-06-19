package com.example.example.myapplication;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.example.example.myapplication.adapters.SignupPageAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SignupActivity extends AppCompatActivity {
    private static final String TAG = "SignupActivity";

    @BindView(R.id.signup_tab_layout) TabLayout _tabLayout;
    @BindView(R.id.signup_view_pager) ViewPager _viewPager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_full);
        ButterKnife.bind(this);
        _tabLayout.addTab(_tabLayout.newTab().setText("Signup Cloud"));
        _tabLayout.addTab(_tabLayout.newTab().setText("Signup Local"));
        SignupPageAdapter adapter = new SignupPageAdapter(getSupportFragmentManager(), _tabLayout.getTabCount());
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

}
