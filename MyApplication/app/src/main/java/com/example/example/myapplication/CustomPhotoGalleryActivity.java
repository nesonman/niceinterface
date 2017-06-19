package com.example.example.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.CheckableImageButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.example.myapplication.adapters.RecyclerViewGridAdapter;
import com.example.example.myapplication.utils.ImageTile;
import com.example.example.myapplication.utils.TagGen;
import com.example.example.myapplication.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CustomPhotoGalleryActivity extends AppCompatActivity implements View.OnClickListener {

    @BindView(R.id.gallery_grid) RecyclerView _recyclerGrid;
    @BindView(R.id.gallery_select) Button _selectButton;

    private Activity mActivity = this;
    private RecyclerViewGridAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_gallery);
        ButterKnife.bind(this);

        final String[] columns = { MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID };
        final String orderBy = MediaStore.Images.Media._ID;
        Cursor imagecursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, orderBy);
        int image_column_index = imagecursor.getColumnIndex(MediaStore.Images.Media._ID);
        int count = imagecursor.getCount();
        List<ImageTile> tiles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            imagecursor.moveToPosition(i);
            int dataColumnIndex = imagecursor.getColumnIndex(MediaStore.Images.Media.DATA);
            tiles.add(new ImageTile(0, null, new File(imagecursor.getString(dataColumnIndex))));
        }
        imagecursor.close();
        this._recyclerGrid.setLayoutManager(new GridLayoutManager(this, 3));
        this.adapter = new RecyclerViewGridAdapter(this, tiles, false);
        this.adapter.setClickListener(new ImageClickListener(this));
        _selectButton.setOnClickListener(this);
        this._recyclerGrid.setAdapter(this.adapter);
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.gallery_select:
                selectImage();
                break;
        }
    }

    private void selectImage() {
        _selectButton.setEnabled(false);
        List<String> selectImageFiles = new ArrayList<>();
        for (ImageTile tile : this.adapter.getTiles()) {
            if (tile.selected) {
                Log.i("SELECTED", tile.file.getAbsolutePath());
                selectImageFiles.add(tile.file.getAbsolutePath());
            }
        }
        if (selectImageFiles.size() == 0) {
            Toast.makeText(getApplicationContext(), "Please select at least one image", Toast.LENGTH_LONG).show();
            _selectButton.setEnabled(true);
        } else {
            Intent i = new Intent();
            i.putExtra("data", selectImageFiles.toArray(new String[1]));
            setResult(Activity.RESULT_OK, i);
            finish();
        }
    }

    private class ImageClickListener implements RecyclerViewGridAdapter.ItemClickListener {

        private Activity activity;

        public ImageClickListener(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onItemClick(View view, int position) {

            ImageView imageView = (ImageView) view;

            ImageTile tile = adapter.getItem(position);
            if (tile.selected) {
                ShapeDrawable shapedrawable = new ShapeDrawable();
                shapedrawable.setShape(new RectShape());
                shapedrawable.getPaint().setColor(Color.TRANSPARENT);
                shapedrawable.getPaint().setStrokeWidth(10f);
                shapedrawable.getPaint().setStyle(Paint.Style.STROKE);
                imageView.setBackground(shapedrawable);
                tile.selected = false;
                Log.i("SELECTED", position + " deselected");
            } else {
                ShapeDrawable shapedrawable = new ShapeDrawable();
                shapedrawable.setShape(new RectShape());
                shapedrawable.getPaint().setColor(0Xffff2929);
                shapedrawable.getPaint().setStrokeWidth(10f);
                shapedrawable.getPaint().setStyle(Paint.Style.STROKE);
                imageView.setBackground(shapedrawable);
                tile.selected = true;
                Log.i("SELECTED", position + " selected");
            }
        }
    }
}