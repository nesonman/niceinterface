package com.example.example.myapplication.db;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This database is for storing the timestamp of image uploads in the local usecase
 */
public class RemoteImageDatabase {

    private SQLiteDatabase database;
    private RemoteImageSQLiteHelper dbHelper;

    public RemoteImageDatabase(Activity activity) {
        this.dbHelper = new RemoteImageSQLiteHelper(activity);
        // open the database upon creation
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
     * Inserts a single image and timestamp into database
     * @param imageName
     */
    public void insertTimestamp(String imageName) {
        ContentValues values = new ContentValues();
        values.put(RemoteImageSQLiteHelper.COLUMN_IMAGE_NAME, imageName);
        values.put(RemoteImageSQLiteHelper.COLUMN_TIMESTAMP, System.currentTimeMillis());
        this.database.insert(RemoteImageSQLiteHelper.TABLE_ENC_TIME, null, values);
    }

    /**
     * Inserts encrypted tags into database
     * @param imageName
     * @param encTags
     */
    public void insertTags(String imageName, byte[] encTags) {
        ContentValues values = new ContentValues();
        values.put(RemoteImageSQLiteHelper.COLUMN_TAGS, encTags);
        String where = RemoteImageSQLiteHelper.COLUMN_IMAGE_NAME + " = ?";
        String[] whereArgs = {imageName};
        this.database.update(RemoteImageSQLiteHelper.TABLE_ENC_TIME, values, where, whereArgs);
    }

    public byte[] getTags(String imageName) {
        Cursor cursor = this.database.query(RemoteImageSQLiteHelper.TABLE_ENC_TIME, new String[]{RemoteImageSQLiteHelper.COLUMN_TAGS},
                RemoteImageSQLiteHelper.COLUMN_IMAGE_NAME + " = ?", new String[]{imageName}, null, null, null);
        if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        byte[] encTags = cursor.getBlob(cursor.getColumnIndexOrThrow(RemoteImageSQLiteHelper.COLUMN_TAGS));
        cursor.close();
        return encTags;
    }

    public int getImageID(String imageName) {
        Cursor cursor = this.database.query(RemoteImageSQLiteHelper.TABLE_ENC_TIME, new String[]{RemoteImageSQLiteHelper.COLUMN_ID},
                RemoteImageSQLiteHelper.COLUMN_IMAGE_NAME + " = ?", new String[]{imageName}, null, null, null);
        if (!cursor.moveToFirst()) {
            // this means our query did not return anything
            cursor.close();
            return -1;
        }
        int imageID = cursor.getInt(cursor.getColumnIndex(RemoteImageSQLiteHelper.COLUMN_ID));
        cursor.close();
        return imageID;
    }

    /*public List<ImageTimeData> getImagesLessThanID(int imageID, long limit) {
        Cursor cursor = this.database.query(RemoteImageSQLiteHelper.TABLE_ENC_TIME, new String[]{RemoteImageSQLiteHelper.COLUMN_ID, RemoteImageSQLiteHelper.COLUMN_IMAGE_NAME, RemoteImageSQLiteHelper.COLUMN_TIMESTAMP},
                RemoteImageSQLiteHelper.COLUMN_ID + " < " + imageID, null, null, null, RemoteImageSQLiteHelper.COLUMN_ID + " DESC", limit + "");
        List<ImageTimeData> data = new ArrayList<>();
        while (cursor.moveToNext()) {
            data.add(new ImageTimeData(cursor.getString(cursor.getColumnIndex(RemoteImageSQLiteHelper.COLUMN_IMAGE_NAME)),
                    cursor.getInt(cursor.getColumnIndex(RemoteImageSQLiteHelper.COLUMN_ID)), cursor.getLong(cursor.getColumnIndex(RemoteImageSQLiteHelper.COLUMN_TIMESTAMP))));
        }
        cursor.close();
        return data;
    }*/

    public List<ImageTimeData> getImagesNotInSet(Set<String> imageNames, long limit) {
        Cursor cursor = this.database.query(RemoteImageSQLiteHelper.TABLE_ENC_TIME, new String[]{RemoteImageSQLiteHelper.COLUMN_ID, RemoteImageSQLiteHelper.COLUMN_IMAGE_NAME, RemoteImageSQLiteHelper.COLUMN_TIMESTAMP},
                null, null, null, null, RemoteImageSQLiteHelper.COLUMN_ID + " DESC");
        List<ImageTimeData> data = new ArrayList<>();
        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndex(RemoteImageSQLiteHelper.COLUMN_IMAGE_NAME));
            if (!imageNames.contains(name)) {
                data.add(new ImageTimeData(cursor.getString(cursor.getColumnIndex(RemoteImageSQLiteHelper.COLUMN_IMAGE_NAME)),
                        cursor.getInt(cursor.getColumnIndex(RemoteImageSQLiteHelper.COLUMN_ID)), cursor.getLong(cursor.getColumnIndex(RemoteImageSQLiteHelper.COLUMN_TIMESTAMP))));
            }
            if (data.size() >= limit) {
                break;
            }
        }
        cursor.close();
        return data;
    }

    /**
     * Deletes all records of this image from the database
     * @param imageName
     */
    public void deleteImage(String imageName) {
        this.database.delete(RemoteImageSQLiteHelper.TABLE_ENC_TIME, RemoteImageSQLiteHelper.COLUMN_IMAGE_NAME + " = ?",
                new String[]{imageName});
    }

}
