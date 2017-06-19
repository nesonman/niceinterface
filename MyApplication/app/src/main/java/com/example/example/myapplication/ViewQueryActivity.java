package com.example.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.example.example.myapplication.adapters.RecyclerViewGridAdapter;
import com.example.example.myapplication.db.ImageDatabase;
import com.example.example.myapplication.utils.Const;
import com.example.example.myapplication.utils.ImageTile;
import com.example.example.myapplication.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Activity to specifically view the results of the most recent query. The browse activity is used to view
 */
public class ViewQueryActivity extends AppCompatActivity {

    @BindView(R.id.view_query_image_grid) RecyclerView _recyclerView;

    private RecyclerViewGridAdapter adapter;
    private String[] queries;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_query);
        ButterKnife.bind(this);
        populateGrid();
    }

    /*@Override
    protected void onRestart() {
        super.onRestart();
        populateGrid();
    }*/

    private void populateGrid() {
        Bundle b = this.getIntent().getExtras();
        this.queries = b.getString(Const.QUERY_RESULT_LABEL).split(" ");
        List<File> queryResult = new ArrayList<>();
        try {
            for (String query : queries) {
                for (String imageName : Utils.getCurrentUserTagIndex(this).get(query)) {
                    Log.i("QUERY RESULT", imageName);
                    queryResult.add(new File(Utils.getCurrentUserThumbnailDir(this), imageName + ".jpg"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<ImageTile> tiles = new ArrayList<>();
        ImageDatabase idb = new ImageDatabase(this);
        for (File imageFile : queryResult) {
            String imageName = imageFile.getName().replace(".jpg", "");
            System.out.println(idb.getTimestamp(imageName));
            tiles.add(new ImageTile(idb.getTimestamp(imageName), imageName, imageFile));
        }
        idb.close();

        _recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        // sort the data before putting it in the adapter
        Collections.sort(tiles, new Comparator<ImageTile>() {
            @Override
            public int compare(ImageTile o1, ImageTile o2) {
                return new Long(o1.timestamp - o2.timestamp).intValue();
            }
        });
        this.adapter = new RecyclerViewGridAdapter(this, tiles, true);
        this.adapter.setClickListener(new ImageClickListener(this));
        _recyclerView.setAdapter(this.adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<File> queryResult = new ArrayList<>();
        try {
            for (String query : queries) {
                for (String imageName : Utils.getCurrentUserTagIndex(this).get(query)) {
                    Log.i("QUERY RESULT", imageName);
                    queryResult.add(new File(Utils.getCurrentUserThumbnailDir(this), imageName + ".jpg"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<ImageTile> tiles = new ArrayList<>();
        ImageDatabase idb = new ImageDatabase(this);
        for (File imageFile : queryResult) {
            String imageName = imageFile.getName().replace(".jpg", "");
            System.out.println(idb.getTimestamp(imageName));
            tiles.add(new ImageTile(idb.getTimestamp(imageName), imageName, imageFile));
        }
        idb.close();

        Collections.sort(tiles, new Comparator<ImageTile>() {
            @Override
            public int compare(ImageTile o1, ImageTile o2) {
                return new Long(o1.timestamp - o2.timestamp).intValue();
            }
        });

        this.adapter.clear();
        this.adapter.addAll(tiles);
    }

    private class ImageClickListener implements RecyclerViewGridAdapter.ItemClickListener {

        private Activity activity;

        public ImageClickListener(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onItemClick(View view, int position) {
            // we get the tile that was clicked
            final ImageTile tile = adapter.getItem(position);
            // if we already have the full image, don't bother requesting it from the server again
            Intent intent = new Intent(getBaseContext(), FullImageActivity.class);
            intent.putExtra(Const.PICTURE_NAME_LABEL, tile.name);
            startActivity(intent);
        }
    }

}
