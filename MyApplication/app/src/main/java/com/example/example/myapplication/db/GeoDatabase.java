package com.example.example.myapplication.db;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.util.ArrayList;
import java.util.List;

public class GeoDatabase extends SQLiteAssetHelper {

    private static final String DATABASE_NAME = "cities.db";
    private static final int DATABASE_VERSION = 1;
    private static final float DEGREE_DELTA = 0.1f;
    private static final double R = 6372.8;

    public GeoDatabase(Activity activity) {
        super(activity, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public String getClosestCityNoAlternateNames(float lat, float lon) {
        SQLiteDatabase db = getReadableDatabase();
        String[] projection = {"uid", "name", "lat", "lon"};
        float lowerlat = lat - DEGREE_DELTA;
        float upperlat = lat + DEGREE_DELTA;
        float lowerlon = lon - DEGREE_DELTA;
        float upperlon = lon + DEGREE_DELTA;

        String selection = "lon > ? and lon < ? and lat > ? and lat < ?";
        String[] selectArgs = {lowerlon + "", upperlon + "", lowerlat + "", upperlat + ""};

        Cursor cursor = db.query("cities", projection, selection, selectArgs, null, null, null);

        // we get all the "close" cities
        double bestDist = Double.MAX_VALUE;
        String bestCityName = null;
        int bestCityID = -1;
        while(cursor.moveToNext()) {
            float templat = cursor.getFloat(cursor.getColumnIndexOrThrow("lat"));
            float templon = cursor.getFloat(cursor.getColumnIndexOrThrow("lon"));
            double tempdist = haversine(lat, lon, templat, templon);
            if (tempdist < bestDist) {
                bestDist = tempdist;
                bestCityName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                bestCityID = cursor.getInt(cursor.getColumnIndexOrThrow("uid"));
            }
        }
        cursor.close();
        return bestCityName;
    }

    /**
     * Returns information about the closest city to these coordinates
     * @param lat
     * @param lon
     * @return
     */
    public List<String> getClosestCity(float lat, float lon) {
        SQLiteDatabase db = getReadableDatabase();
        String[] projection = {"uid", "name", "lat", "lon"};
        float lowerlat = lat - DEGREE_DELTA;
        float upperlat = lat + DEGREE_DELTA;
        float lowerlon = lon - DEGREE_DELTA;
        float upperlon = lon + DEGREE_DELTA;

        String selection = "lon > ? and lon < ? and lat > ? and lat < ?";
        String[] selectArgs = {lowerlon + "", upperlon + "", lowerlat + "", upperlat + ""};

        Cursor cursor = db.query("cities", projection, selection, selectArgs, null, null, null);

        // we get all the "close" cities
        double bestDist = Double.MAX_VALUE;
        String bestCityName = null;
        int bestCityID = -1;
        while(cursor.moveToNext()) {
            float templat = cursor.getFloat(cursor.getColumnIndexOrThrow("lat"));
            float templon = cursor.getFloat(cursor.getColumnIndexOrThrow("lon"));
            double tempdist = haversine(lat, lon, templat, templon);
            if (tempdist < bestDist) {
                bestDist = tempdist;
                bestCityName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                bestCityID = cursor.getInt(cursor.getColumnIndexOrThrow("uid"));
            }
        }
        cursor.close();

        // get the other names of this city
        String[] projection2 = {"name"};
        String selection2 = "uid = ?";
        String[] selectArgs2 = {bestCityID + ""};

        cursor = db.query("alternate_names", projection2, selection2, selectArgs2, null, null, null);
        List<String> cityNames = new ArrayList<>();
        while(cursor.moveToNext()) {
            cityNames.add(cursor.getString(cursor.getColumnIndexOrThrow("name")));
        }
        cityNames.add(bestCityName);

        return cityNames;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = Math.pow(Math.sin(dLat / 2),2) + Math.pow(Math.sin(dLon / 2),2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c;
    }

}
