package com.example.example.myapplication.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Class to help create the SQLite database to store the timestamps of "upload" in the local usecase"
 */
public class RemoteImageSQLiteHelper extends SQLiteOpenHelper {

    public static final String TABLE_ENC_TIME = "enctime";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_IMAGE_NAME = "image_name";
    public static final String COLUMN_TIMESTAMP = "time";
    public static final String COLUMN_TAGS = "tags";

    private static final String DATABASE_NAME = "timestamp.db";
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_CREATE = "CREATE TABLE " + TABLE_ENC_TIME + " ( " + COLUMN_ID +
            " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_IMAGE_NAME + " TEXT NOT NULL, " + COLUMN_TIMESTAMP +
            " INTEGER NOT NULL, " + COLUMN_TAGS + " BLOB);";

    public RemoteImageSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
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
