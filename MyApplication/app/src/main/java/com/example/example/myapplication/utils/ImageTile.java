package com.example.example.myapplication.utils;

import android.graphics.Bitmap;

import java.io.File;

/**
 * This class is a thumbnail tile for the grid views
 */
public class ImageTile {

    public long timestamp;
    public String name;
    public File file;
    // this boolean is for use in the custom gallery activity
    public boolean selected = false;

    public ImageTile(long timestamp1, String name, File file) {
        this.timestamp = timestamp1;
        this.name = name;
        this.file = file;
    }

}