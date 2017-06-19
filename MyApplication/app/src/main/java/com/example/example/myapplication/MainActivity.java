package com.example.example.myapplication;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.example.myapplication.adapters.MainPageAdapter;
import com.example.example.myapplication.utils.KeyStoreHelper;

import org.crypto.sse.CryptoPrimitives;

import javax.crypto.SecretKey;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.main_view_pager) ViewPager _viewPager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_swipeview);
        ButterKnife.bind(this);
        MainPageAdapter adapter = new MainPageAdapter(getSupportFragmentManager(), 2);
        _viewPager.setAdapter(adapter);
        _viewPager.setCurrentItem(1);
    }

    @Override
    public void onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true);
    }

    public void switchAdapterItem(int position) {
        _viewPager.setCurrentItem(position);
    }
}
