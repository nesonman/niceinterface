package com.example.example.myapplication;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.example.myapplication.remote.DynRH2LevClientWrapper;
import com.example.example.myapplication.utils.Const;
import com.example.example.myapplication.utils.Utils;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.impl.client.cache.ExponentialBackOffSchedulingStrategy;

/**
 * Activity that allows users to set up a new device
 */
public class SetupNewDeviceActivity extends AppCompatActivity implements View.OnClickListener {

    @BindView(R.id.input_email_setup_new) EditText _email;
    @BindView(R.id.input_code_setup_new) EditText _code;
    @BindView(R.id.setup_device_next) Button _next;
    @BindView(R.id.setup_device_final) Button _setupDevice;
    @BindView(R.id.setup_device_email_wrapper) TextInputLayout _emailWrapper;
    private String userEmail;
    private Activity mActivity = this;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_new_device);
        ButterKnife.bind(this);
        _next.setOnClickListener(this);
        _setupDevice.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.setup_device_next:
                next();
                break;
            case R.id.setup_device_final:
                setupDevice();
                break;
        }
    }

    private void next() {
        final String email = _email.getText().toString();
        DynRH2LevClientWrapper.requestSetupNewDevice(this, email, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Toast.makeText(mActivity, "Check your email for your code", Toast.LENGTH_SHORT).show();
                userEmail = email;
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(mActivity, "Could not send email", Toast.LENGTH_SHORT).show();
                mActivity.onBackPressed();
            }
        });
        _email.setText("");
        _email.setHint("");
        _email.setVisibility(View.GONE);
        ((ViewGroup) _email.getParent()).removeView(_email);
        _emailWrapper.setVisibility(View.GONE);
        _code.setVisibility(View.VISIBLE);
        ((ViewGroup) _next.getParent()).removeView(_next);
        _setupDevice.setVisibility(View.VISIBLE);
    }

    private void setupDevice() {
        String code = _code.getText().toString();

        DynRH2LevClientWrapper.setupNewDevice(this, code, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String salt = response.getString("salt");
                    String encState = response.getString(Const.STATE_LABEL);
                    String clientSalt = response.getString(Const.LOCAL_PASSWORD_SALT_LABEL);
                    Utils.putCurrentEmailInSharedPref(mActivity, userEmail);
                    SharedPreferences sharedPref = Utils.getUserSharedPreference(mActivity);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(Const.SHARED_PREF_SALT, salt);
                    editor.putString(Const.ENC_STATE_SHARED_PREF_LABEL, encState);
                    editor.putString(Const.LOCAL_PASSWORD_SALT_LABEL, clientSalt);
                    editor.commit();
                    Toast.makeText(mActivity, "Your device has been setup", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(mActivity, "Could not parse response", Toast.LENGTH_SHORT).show();
                }
                mActivity.onBackPressed();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Toast.makeText(mActivity, responseString, Toast.LENGTH_SHORT).show();
                mActivity.onBackPressed();
            }
        });
    }
}
