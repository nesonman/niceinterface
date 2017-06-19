package com.example.example.myapplication.db;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.example.myapplication.utils.Utils;

/**
 * Helper for creating an image database to hold information about the images we are caching locally
 * TODO: look into encrypting this information
 */
public class ImageSQLiteHelper extends SQLiteOpenHelper {

    public static final String TABLE_IMAGE = "image";
    public static final String COLUMN_ID = "id"; // some incrementing id of the images, not too important
    public static final String COLUMN_IMAGE_NAME = "image_name"; // the name of the image
    public static final String COLUMN_TIMESTAMP = "time"; // the time the image was downloaded
    public static final String COLUMN_TAGS = "tags"; // the tags of the image

    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_CREATE = "CREATE TABLE " + TABLE_IMAGE + " ( " + COLUMN_ID +
            " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_IMAGE_NAME + " TEXT NOT NULL, " + COLUMN_TIMESTAMP +
            " INTEGER NOT NULL, " + COLUMN_TAGS + " BLOB NOT NULL DEFAULT '');";

    public ImageSQLiteHelper(Context context, String databaseName) {
        super(context, databaseName, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // don't do anything
    }

}
