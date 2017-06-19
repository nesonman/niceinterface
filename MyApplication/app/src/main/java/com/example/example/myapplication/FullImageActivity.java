package com.example.example.myapplication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.example.myapplication.db.ImageDatabase;
import com.example.example.myapplication.remote.DynRH2LevClientWrapper;
import com.example.example.myapplication.utils.Const;
import com.example.example.myapplication.utils.TouchImageView;
import com.example.example.myapplication.utils.Utils;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import org.bouncycastle.jce.exception.ExtIOException;
import org.crypto.sse.CryptoPrimitives;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import cz.msebera.android.httpclient.Header;

/**
 * An activity that allows a user to view and interact with a full image.
 */
public class FullImageActivity extends AppCompatActivity implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {

    @BindView(R.id.full_image) TouchImageView _fullImage;
    @BindView(R.id.full_image_tags) TextView _tags;
    @BindView(R.id.full_image_options) ImageButton _optionsButton;
    @BindView(R.id.full_image_options_wrapper) RelativeLayout _optionsWrapper;

    private Activity activity = this;
    private ProgressDialog progressDialog;
    private String imageName;
    private PopupMenu popup;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_image);
        ButterKnife.bind(this);
        _optionsButton.setOnClickListener(this);
        _fullImage.setOnClickListener(this);
        popup = new PopupMenu(this, _optionsButton);
        popup.getMenuInflater().inflate(R.menu.full_options_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        imageName = getIntent().getStringExtra(Const.PICTURE_NAME_LABEL);
        this.progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Fetching Image...");
        progressDialog.show();
        // if we aren't using local mode, we need to fetch the medium image
        if (!Utils.isUsingLocalMode(activity)) {
            new MediumImageFetcher().execute(imageName);
        }
        new FullImageFetcher().execute(imageName);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.full_image_options:
                showOptions();
                break;
            case R.id.full_image:
                toggleHeaders();
                break;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        DynRH2LevClientWrapper.delete(this, imageName, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                // delete image from local data and go back
                File fullImage = new File(Utils.getCurrentUserImageDir(activity), imageName + ".jpg");
                File thumb = new File(Utils.getCurrentUserThumbnailDir(activity), imageName + ".jpg");
                fullImage.delete();
                thumb.delete();

                // delete image from db
                ImageDatabase idb = new ImageDatabase(activity);
                idb.deleteImageData(imageName);
                idb.close();

                // delete image from cache
                try {
                    Multimap<String, String> multimap = Utils.getCurrentUserTagIndex(activity);
                    Iterator<Map.Entry<String, String>> iterator = multimap.entries().iterator();
                    while (iterator.hasNext()) {
                        if (iterator.next().getValue().equals(imageName)) {
                            iterator.remove();
                        }
                    }
                    Utils.saveCurrentUserTagIndex(activity, multimap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                FullImageActivity.super.onBackPressed();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(activity, new String(responseBody), Toast.LENGTH_SHORT).show();
            }
        });
        return true;
    }

    private void showOptions() {
        popup.show();
    }

    private void toggleHeaders() {
        if (_optionsWrapper.getVisibility() == View.GONE) {
            fadeInHeaders();
        } else {
            fadeOutHeaders();
        }
    }

    private void fadeInHeaders() {
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new AccelerateInterpolator());
        fadeIn.setDuration(200);
        fadeIn.setAnimationListener(new Animation.AnimationListener()
        {
            public void onAnimationEnd(Animation animation)
            {
                _optionsWrapper.setVisibility(View.VISIBLE);
                _tags.setVisibility(View.VISIBLE);
            }
            public void onAnimationRepeat(Animation animation) {}
            public void onAnimationStart(Animation animation) {}
        });
        _optionsWrapper.startAnimation(fadeIn);
        _tags.startAnimation(fadeIn);
    }

    private void fadeOutHeaders() {
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setDuration(200);
        fadeOut.setAnimationListener(new Animation.AnimationListener()
        {
            public void onAnimationEnd(Animation animation)
            {
                _optionsWrapper.setVisibility(View.GONE);
                _tags.setVisibility(View.GONE);
            }
            public void onAnimationRepeat(Animation animation) {}
            public void onAnimationStart(Animation animation) {}
        });
        _optionsWrapper.startAnimation(fadeOut);
        _tags.startAnimation(fadeOut);
    }

    public class MediumImageFetcher extends AsyncTask<String, Integer, File> {
        @Override
        protected File doInBackground(final String... params) {
            // if we already have the image then load it
            File imageFile = new File(Utils.getCurrentUserMediumDir(activity), params[0] + ".jpg");
            if (imageFile.exists()) {
                return imageFile;
            }

            // we fetch the file synchronously, must wait for both requests to finish
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    DynRH2LevClientWrapper.getMediumImage(activity, params[0], new FileAsyncHttpResponseHandler(activity) {
                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                            Log.i("FULL IMAGE", "FAILED");
                        }

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, File file) {
                            Log.i("FULL IMAGE", "SUCCESS");
                            try {
                                if (!Utils.isUsingLocalMode(activity)) {
                                    Utils.unpackMediumZip(activity, file);
                                }
                                countDownLatch.countDown();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            });
            try {
                countDownLatch.await(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // if we never downloaded anything
            if (!imageFile.exists()) {
                // TODO: get a placeholder image or something
                return null;
            }

            return imageFile;
        }

        @Override
        protected void onPostExecute(File file) {
            // if the full image already exists, don't replace with medium image
            if (new File(Utils.getCurrentUserImageDir(activity), imageName + ".jpg").exists()) {
                return;
            }

            byte[] sk = null;
            try {
                sk = Utils.getSk(activity);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            if (file != null) {
                Log.i("POST EXECUTE", file.toString());
                /*Glide.with(activity)
                        .load(file)
                        .placeholder(R.drawable.placeholder)
                        .dontAnimate()
                        .into(_fullImage);*/
                try {
                    byte[] encImage = Files.toByteArray(file);
                    byte[] decImage = CryptoPrimitives.decryptAES_CTR_String(encImage, sk);
                    Bitmap imageBitmap = BitmapFactory.decodeByteArray(decImage, 0, decImage.length);
                    _fullImage.setImageBitmap(imageBitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            ImageDatabase idb = new ImageDatabase(activity);
            _tags.setText(idb.getTags(imageName));
            idb.close();

            progressDialog.dismiss();
        }
    }

    public class FullImageFetcher extends AsyncTask<String, Integer, File> {
        @Override
        protected File doInBackground(final String... params) {
            // if we already have the image then load it
            File imageFile = new File(Utils.getCurrentUserImageDir(activity), params[0] + ".jpg");
            if (imageFile.exists()) {
                return imageFile;
            }

            // we fetch the file synchronously, must wait for both requests to finish
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    DynRH2LevClientWrapper.getFullImage(activity, params[0], new FileAsyncHttpResponseHandler(activity) {
                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                            Log.i("MEDIUM IMAGE", "FAILED");
                        }

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, File file) {
                            Log.i("MEDIUM IMAGE", "SUCCESS");
                            try {
                                if (!Utils.isUsingLocalMode(activity)) {
                                    Utils.unpackImageZip(activity, file);
                                }
                                countDownLatch.countDown();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            });
            try {
                countDownLatch.await(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // if we never downloaded anything
            if (!imageFile.exists()) {
                // TODO: get a placeholder image or something
                return null;
            }

            return imageFile;
        }

        @Override
        protected void onPostExecute(File file) {
            byte[] sk = null;
            try {
                sk = Utils.getSk(activity);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            if (file != null) {
                Log.i("POST EXECUTE", file.toString());
                /*Glide.with(activity)
                        .load(file)
                        .placeholder(R.drawable.placeholder)
                        .dontAnimate()
                        .into(_fullImage);*/
                try {
                    byte[] encImage = Files.toByteArray(file);
                    byte[] decImage = CryptoPrimitives.decryptAES_CTR_String(encImage, sk);
                    Bitmap imageBitmap = BitmapFactory.decodeByteArray(decImage, 0, decImage.length);
                    _fullImage.setImageBitmap(imageBitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            ImageDatabase idb = new ImageDatabase(activity);
            _tags.setText(idb.getTags(imageName));
            idb.close();

            progressDialog.dismiss();
        }
    }
}
