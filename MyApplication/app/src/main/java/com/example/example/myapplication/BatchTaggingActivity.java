package com.example.example.myapplication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.util.Util;
import com.example.example.myapplication.db.ImageDatabase;
import com.example.example.myapplication.fragments.GalleryFragment;
import com.example.example.myapplication.fragments.VerticalPagerFragment;
import com.example.example.myapplication.remote.DynRH2LevClientWrapper;
import com.example.example.myapplication.utils.Const;
import com.example.example.myapplication.utils.IndexBuilder;
import com.example.example.myapplication.utils.IndexPair;
import com.example.example.myapplication.utils.TagGen;
import com.example.example.myapplication.utils.Utils;
import com.example.example.myapplication.view.GifImageView;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.commons.lang3.StringUtils;
import org.crypto.sse.CryptoPrimitives;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cz.msebera.android.httpclient.Header;

/**
 * Allows user to tag batch images
 */
public class BatchTaggingActivity extends AppCompatActivity implements View.OnClickListener {

    @BindView(R.id.batch_tag_grid) GridView _grid;
    @BindView(R.id.input_global_tags) EditText _globalTags;
    @BindView(R.id.batch_tag_image) ImageView _image;
    @BindView(R.id.input_batch_tag_image) EditText _imageTags;
    @BindView(R.id.btn_close_batch_image) Button _closeImage;
    @BindView(R.id.btn_upload_batch_image) Button _upload;
    @BindView(R.id.gif_loading) GifImageView _gifView;

    private ImageListAdapter adapter;
    private Activity mActivity = this;
    private int currentPosition = -1;
    private String[] imageFiles;
    private int loaded = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_tag);
        ButterKnife.bind(this);
        _closeImage.setOnClickListener(this);
        _upload.setOnClickListener(this);
        _gifView.setGifImageResource(R.drawable.loading);
        imageFiles = this.getIntent().getExtras().getStringArray(Const.IMAGE_FILE_BUNDLE_LABEL);
        if (imageFiles == null) {
            Toast.makeText(this, "Incorrect bundle passed in", Toast.LENGTH_SHORT);
            super.onBackPressed();
        }
        // if we got here, set things up
        List<ImageTileWithTagGen> tiles = new ArrayList<>();
        for (String filename : imageFiles) {
            try {
                Uri imageURI = Uri.fromFile(new File(filename));
                Bitmap image = Utils.cropBitmap(Utils.rotateBitmapIfRequired(TagGen.decodeSampledBitmapFromUri(this, imageURI, 224, 224), imageURI));
                long timestamp = System.currentTimeMillis();
                tiles.add(new ImageTileWithTagGen(filename, image, timestamp));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        this.adapter = new ImageListAdapter(this, tiles);
        _grid.setAdapter(adapter);
        _grid.setOnItemClickListener(new ImageClickListener(this));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_close_batch_image:
                closeImage();
                break;
            case R.id.btn_upload_batch_image:
                uploadAll();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        // if we have our image tagging screen up, close it
        if (currentPosition != -1) {
            closeImage();
        } else {
            // do the default
            super.onBackPressed();
        }
    }

    public void closeImage() {
        _image.setImageBitmap(null);
        _image.setVisibility(View.GONE);
        if (currentPosition != -1) {
            adapter.getItem(currentPosition).tags = new ArrayList<>(Arrays.asList(_imageTags.getText().toString().trim().toLowerCase().split(" ")));
        }
        currentPosition = -1;
        _imageTags.setText("");
        _imageTags.setVisibility(View.GONE);
        _closeImage.setVisibility(View.GONE);
        _globalTags.setVisibility(View.VISIBLE);
        _grid.setVisibility(View.VISIBLE);
        _upload.setVisibility(View.VISIBLE);
    }

    public void uploadAll() {
        _upload.setEnabled(false);
        new UploadTask().execute(new Void[]{});
    }

    public class ImageTileWithTagGen {
        public Bitmap bitmap;
        public long timestamp;
        public String file;
        public List<String> tags;

        public ImageTileWithTagGen(String file, Bitmap bitmap1, long timestamp1) {
            this.bitmap = bitmap1;
            this.timestamp = timestamp1;
            this.file = file;

            // generate the tags
            generateTagBackground();
        }

        private void generateTagBackground() {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        tags = TagGen.generateTagFromGalleryImage(mActivity, Uri.fromFile(new File(file)));
                        Log.i("TAGS", tags.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                        tags = new ArrayList<>();
                    }
                    loaded++;
                    if (loaded == imageFiles.length) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                _gifView.setVisibility(View.GONE);
                                _upload.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }
            });
        }

    }

    private class ImageListAdapter extends ArrayAdapter<ImageTileWithTagGen> {

        private Context context;
        private List<ImageTileWithTagGen> tiles;

        public ImageListAdapter(Context context, List<ImageTileWithTagGen> tiles) {
            super(context, R.layout.image_list_item, tiles);
            this.context = context;
            this.tiles = tiles;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageTileWithTagGen tile = tiles.get(position);
            ViewHolder holder;
            if (convertView == null) {
                Log.i("convert view null", "null");
                holder = new ViewHolder();
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.image_list_item, parent, false);
                holder.image = (ImageView) convertView.findViewById(R.id.img);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
                Log.i("convert view not null", "not null");
            }
            holder.image.setImageBitmap(tile.bitmap);
            return convertView;
        }

    }

    private static class ViewHolder {
        ImageView image;
    }

    private class ImageClickListener implements AdapterView.OnItemClickListener {

        private Activity activity;

        public ImageClickListener(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            try {
                ProgressDialog progressDialog = new ProgressDialog(mActivity);
                progressDialog.setIndeterminate(true);
                progressDialog.setMessage("Fetching Image...");
                progressDialog.show();
                // we get the tile that was clicked
                currentPosition = position;
                ImageTileWithTagGen tile = adapter.getItem(position);
                _image.setImageBitmap(Utils.rotateBitmapIfRequired(TagGen.decodeSampledBitmapFromUri(mActivity, Uri.fromFile(new File(tile.file)), Const.MEDIUM_SCALE_SIZE, Const.MEDIUM_SCALE_SIZE), Uri.fromFile(new File(tile.file))));
                _imageTags.setText(Utils.listToString(tile.tags));
                _image.setVisibility(View.VISIBLE);
                _imageTags.setVisibility(View.VISIBLE);
                _imageTags.bringToFront();
                _closeImage.setVisibility(View.VISIBLE);
                _globalTags.setVisibility(View.GONE);
                _grid.setVisibility(View.GONE);
                _upload.setVisibility(View.GONE);
                progressDialog.dismiss();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private class UploadTask extends AsyncTask<Void, Void, Void> {

        ProgressDialog progressDialog;
        List<String> globalTags;

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(mActivity);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Uploading...");
            progressDialog.show();
            globalTags = Arrays.asList(_globalTags.getText().toString().trim().toLowerCase().split(" "));
        }

        @Override
        protected Void doInBackground(Void... params) {
            byte[] sk = null;
            try {
                sk = Utils.getSk(mActivity);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

            for (ImageTileWithTagGen tile : adapter.tiles) {
                tile.tags.addAll(globalTags);
            }
            final IndexPair indexPair = IndexBuilder.buildIndexFromBatch(mActivity, adapter.tiles);

            // copy over the files locally with the correct orientation
            final List<String> imageNames = new ArrayList<>();
            for (String file : imageFiles) {
                try {
                    String imageName = indexPair.fileToRename.get(file);
                    Bitmap fullRotatedImage = Utils.rotateBitmapIfRequired(BitmapFactory.decodeFile(file), Uri.fromFile(new File(file)));
                    Bitmap thumbnailRotated = Utils.cropBitmap(Utils.rotateBitmapIfRequired(TagGen.decodeSampledBitmapFromUri(mActivity, Uri.fromFile(new File(file)), Const.THUMBNAIL_SCALE_SIZE, Const.THUMBNAIL_SCALE_SIZE), Uri.fromFile(new File(file))));
                    Bitmap mediumRotated = Utils.rotateBitmapIfRequired(TagGen.decodeSampledBitmapFromUri(mActivity, Uri.fromFile(new File(file)), Const.MEDIUM_SCALE_SIZE, Const.MEDIUM_SCALE_SIZE), Uri.fromFile(new File(file)));
                    File fullImageLoc = Utils.makeFileInUserImageDir(mActivity, imageName + ".jpg");
                    File thumbnailLoc = Utils.makeFileInUserThumbnailDir(mActivity, imageName + ".jpg");
                    File mediumLoc = Utils.makeFileInUserMediumDir(mActivity, imageName + ".jpg");
                    ByteArrayOutputStream fullImageByteStream = new ByteArrayOutputStream();
                    ByteArrayOutputStream thumbByteStream = new ByteArrayOutputStream();
                    ByteArrayOutputStream mediumByteStream = new ByteArrayOutputStream();
                    fullRotatedImage.compress(Bitmap.CompressFormat.JPEG, 100, fullImageByteStream);
                    thumbnailRotated.compress(Bitmap.CompressFormat.JPEG, 100, thumbByteStream);
                    mediumRotated.compress(Bitmap.CompressFormat.JPEG, 100, mediumByteStream);
                    fullRotatedImage = null;
                    thumbnailRotated = null;
                    mediumRotated = null;
                    System.gc(); // attempt to free memory intensive resources
                    byte[] imageBytes = fullImageByteStream.toByteArray();
                    byte[] encImage = CryptoPrimitives.encryptAES_CTR_Byte(sk, CryptoPrimitives.randomBytes(16), imageBytes);
                    Files.write(encImage, fullImageLoc);
                    imageBytes = null;
                    encImage = null;
                    byte[] thumbBytes = thumbByteStream.toByteArray();
                    byte[] encThumb = CryptoPrimitives.encryptAES_CTR_Byte(sk, CryptoPrimitives.randomBytes(16), thumbBytes);
                    Files.write(encThumb, thumbnailLoc);
                    thumbBytes = null;
                    encThumb = null;
                    byte[] mediumBytes = mediumByteStream.toByteArray();
                    byte[] encMedium = CryptoPrimitives.encryptAES_CTR_Byte(sk, CryptoPrimitives.randomBytes(16), mediumBytes);
                    Files.write(encMedium, mediumLoc);
                    mediumBytes = null;
                    encMedium = null;
                    System.gc(); // attempt to free memory intensive resources
                    imageNames.add(imageName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // update the local cache
            try {
                Multimap<String, String> multimap = Utils.getCurrentUserTagIndex(mActivity);
                for (String tag : indexPair.lp1.keySet()) {
                    for (String imageName : indexPair.lp1.get(tag)) {
                        if (!multimap.get(tag).contains(imageName)) {
                            multimap.put(tag, imageName);
                        }
                    }
                }
                Utils.saveCurrentUserTagIndex(mActivity, multimap);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // save image tags locally
            ImageDatabase idb = new ImageDatabase(mActivity);
            for (String imageName : indexPair.lp2.keySet()) {
                idb.addImageName(imageName);
                try {
                    String tagString = StringUtils.join(indexPair.lp2.get(imageName), " ");
                    String paddedTagString = Utils.rightPadding(tagString, Const.TAG_TOTAL_MAX_CHARS);
                    byte[] encTags = CryptoPrimitives.encryptAES_CTR_Byte(sk, CryptoPrimitives.randomBytes(16), paddedTagString.getBytes());
                    idb.addTags(imageName, encTags);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            idb.close();

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        DynRH2LevClientWrapper.updateMulti(mActivity, indexPair, imageNames, new AsyncHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                                Log.i("UPDATE", new String(responseBody));
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                                Log.i("UPDATE", new String(responseBody));
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            progressDialog.dismiss();
            _upload.setEnabled(true);
            onBackPressed();
        }
    }

}
