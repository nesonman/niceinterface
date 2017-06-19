package com.example.example.myapplication.db;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.example.myapplication.utils.Const;
import com.example.example.myapplication.utils.Utils;

import org.crypto.sse.CryptoPrimitives;

/**
 * Database for storing information about the images we downloaded from the server
 */
public class ImageDatabase {

    private static final String DATABASE_NAME = "image.db";

    private SQLiteDatabase database;
    private ImageSQLiteHelper dbHelper;
    private Activity activity;

    public ImageDatabase(Activity activity) {
        String emailHash = Utils.getCurrentEmailHashInSharedPref(activity);
        this.dbHelper = new ImageSQLiteHelper(activity, emailHash + DATABASE_NAME);
        this.activity = activity;
        open();
    }

    public void open() throws SQLException {
        this.database = dbHelper.getWritableDatabase();
    }

    public void close() {
        this.dbHelper.close();
        this.database.close();
    }

    /**
     * Adds the image name to the database with the current time stamp
     * @param imageName
     */
    public void addImageName(String imageName) {
        // if the image has not been inserted, insert new row
        if (getTimestamp(imageName) == 0) {
            ContentValues values = new ContentValues();
            values.put(ImageSQLiteHelper.COLUMN_IMAGE_NAME, imageName);
            values.put(ImageSQLiteHelper.COLUMN_TIMESTAMP, System.currentTimeMillis());
            this.database.insert(ImageSQLiteHelper.TABLE_IMAGE, null, values);
        } else {
            // otherwise update the time of the existing row
            ContentValues values = new ContentValues();
            values.put(ImageSQLiteHelper.COLUMN_TIMESTAMP, System.currentTimeMillis());
            String[] whereArgs = {imageName};
            this.database.update(ImageSQLiteHelper.TABLE_IMAGE, values, ImageSQLiteHelper.COLUMN_IMAGE_NAME + " = ?", whereArgs);
        }
    }

    /**
     * This function takes in the encrypted tags to store the the database. The data is securely
     * encrypted at rest to make it more difficult to recover the data
     * @param imageName
     * @param encTags the encrypted tags
     */
    public void addTags(String imageName, byte[] encTags) {
        ContentValues values = new ContentValues();
        values.put(ImageSQLiteHelper.COLUMN_TAGS, encTags);
        String[] whereArgs = {imageName};
        this.database.update(ImageSQLiteHelper.TABLE_IMAGE, values, ImageSQLiteHelper.COLUMN_IMAGE_NAME + " = ?", whereArgs);
    }

    public long getTimestamp(String imageName) {
        String[] projection = {ImageSQLiteHelper.COLUMN_TIMESTAMP};
        String selection = ImageSQLiteHelper.COLUMN_IMAGE_NAME + " = ?";
        String[] selectionArgs = {imageName};

        Cursor cursor = this.database.query(ImageSQLiteHelper.TABLE_IMAGE, projection, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            long time = cursor.getLong(cursor.getColumnIndexOrThrow(ImageSQLiteHelper.COLUMN_TIMESTAMP));
            cursor.close();
            return time;
        }
        cursor.close();
        // if we could not find the image, return 0;
        return 0;
    }

    public String getTags(String imageName) {
        byte[] sk = null;
        try {
            sk = Utils.getSk(activity);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        String[] projection = {ImageSQLiteHelper.COLUMN_TAGS};
        String selection = ImageSQLiteHelper.COLUMN_IMAGE_NAME + " = ?";
        String[] selectionArgs = {imageName};

        Cursor cursor = this.database.query(ImageSQLiteHelper.TABLE_IMAGE, projection, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            byte[] encTags = cursor.getBlob(cursor.getColumnIndexOrThrow(ImageSQLiteHelper.COLUMN_TAGS));
            cursor.close();
            try {
                // remove padding by trimming the tags
                return new String(CryptoPrimitives.decryptAES_CTR_String(encTags, sk)).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }
        cursor.close();
        Log.i("IMAGE DATABASE", "COULD NOT FIND TAG");
        // if we could not get the tags, return an empty string
        return "";
    }

    public void deleteImageData(String imageName) {
        this.database.delete(ImageSQLiteHelper.TABLE_IMAGE, ImageSQLiteHelper.COLUMN_IMAGE_NAME + " = ?", new String[]{imageName});
    }

    public void clearAll() {
        this.database.delete(ImageSQLiteHelper.TABLE_IMAGE, null, null);
    }

}
