package com.example.example.myapplication.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.example.example.myapplication.db.GeoDatabase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class generates the tags from the image.
 */
public class TagGen {

    private static final int INPUT_SIZE = 224;

    private static TensorFlowImageClassifier classifier;
    private static GeoDatabase geoDatabase;

    public static void initClassifier(Activity activity, TensorFlowImageClassifier tfc) {
        classifier = tfc;
        geoDatabase = new GeoDatabase(activity);
    }

    public static List<String> generateTagFromGalleryImage(Activity activity, Uri uri) throws IOException {
        List<String> keywords = new ArrayList<>();
        //Bitmap bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), uri);
        Bitmap bitmap = decodeSampledBitmapFromUri(activity, uri, INPUT_SIZE, INPUT_SIZE);
        Log.i("bit map size", bitmap.getHeight() + " " + bitmap.getWidth());
        Bitmap rotatedBitmap = Utils.rotateBitmapIfRequired(bitmap, uri);
        Bitmap croppedBitmap = Utils.cropBitmap(rotatedBitmap);
        Bitmap scaledBitmap = Utils.scaleBitmap(croppedBitmap, INPUT_SIZE, INPUT_SIZE);
        String path = GetFilePathFromDevice.getPath(activity, uri);
        Map<String, String> exif = getEXIFData(path);
        Log.i("exif data", "data : " + exif.toString());
        if (exif.containsKey("lat")) {
            String cityName = geoDatabase.getClosestCityNoAlternateNames(Float.parseFloat(exif.get("lat")), Float.parseFloat(exif.get("lon")));
            keywords.add(cityName);
        }
        if (exif.containsKey("date") && exif.get("date") != null) {
            keywords.addAll(dateToWords(exif.get("date")));
        }
        List<Classifier.Recognition> recs = classifier.recognizeImage(scaledBitmap);
        for (Classifier.Recognition rec : recs) {
            if (rec.getConfidence() > 0.1) {
                keywords.add(rec.getTitle());
            }
        }
        List<String> lowerWords = new ArrayList<>();
        for (String keyword : keywords) {
            lowerWords.add(keyword.toLowerCase());
        }
        return lowerWords;
    }

    public static List<String> generateTagFromCustomTakenImage(Activity activity, Bitmap image) {
        List<String> keywords = new ArrayList<>();
        Bitmap croppedBitmap = Utils.cropBitmap(image);
        Bitmap scaledBitmap = Utils.scaleBitmap(croppedBitmap, INPUT_SIZE, INPUT_SIZE);
        List<Classifier.Recognition> recs = classifier.recognizeImage(scaledBitmap);
        for (Classifier.Recognition rec : recs) {
            if (rec.getConfidence() > 0.1) {
                keywords.add(rec.getTitle());
            }
        }
        Log.i("IMAGE REC", keywords.toString());
        // get location if we have permission
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location loc = Utils.getLocation(activity);
            // if the location is not null, add our geo tag
            if (loc != null) {
                //List<String> cityNames = geoDatabase.getClosestCity((float) loc.getLatitude(), (float) loc.getLongitude());
                // only get the best city without alternate names
                String cityName = geoDatabase.getClosestCityNoAlternateNames((float) loc.getLatitude(), (float) loc.getLongitude());
                if (cityName != null) {
                    Log.i("CITIES", cityName);
                    keywords.add(cityName);
                }
            }
        }

        // get the date
        DateFormat dateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        Date date = new Date();
        List<String> dateWords = dateToWords(dateFormat.format(date));
        Log.i("DATE", dateWords.toString());
        keywords.addAll(dateWords);
        return keywords;
    }

    private static Map<String, String> getEXIFData(String path) throws IOException {
        ExifInterface exif = new ExifInterface(path);
        Map<String, String> exifData = new HashMap<>();
        exifData.put("date", exif.getAttribute(ExifInterface.TAG_DATETIME));
        float [] latlon = new float[2];
        if (exif.getLatLong(latlon)) {
            exifData.put("lat", latlon[0] + "");
            exifData.put("lon", latlon[1] + "");
        }
        /*exifData.put(ExifInterface.TAG_GPS_LATITUDE, exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
        exifData.put(ExifInterface.TAG_GPS_LONGITUDE, exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));*/
        return exifData;
    }

    private static List<String> dateToWords(String date) {
        List<String> words = new ArrayList<>();
        String[] time = date.split(" ")[0].split(":");
        words.add(time[0]); // adding year
        words.add(time[1]);
        words.add(getMonth(time[1]));
        words.add(time[2]);
        return words;
    }

    private static String getMonth(String m) {
        switch (m) {
            case "01":
                return "January";
            case "02":
                return "February";
            case "03":
                return "March";
            case "04":
                return "April";
            case "05":
                return "May";
            case "06":
                return "June";
            case "07":
                return "July";
            case "08":
                return "August";
            case "09":
                return "September";
            case "10":
                return "October";
            case "11":
                return "November";
            case "12":
                return "December";
        }
        return "";
    }

    public static Bitmap decodeSampledBitmapFromUri(Activity activity, Uri uri,int reqWidth, int reqHeight) throws FileNotFoundException {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(activity.getContentResolver().openInputStream(uri), null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeStream(activity.getContentResolver().openInputStream(uri), null, options);
    }

    public static Bitmap decodeSampledBitmapFromBytes(byte[] input, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(input, 0, input.length, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(input, 0, input.length, options);
    }

    public static int calculateInSampleSize (
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        Log.i("height", height + "");
        Log.i("req height", reqHeight + "");
        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        Log.i("in sample size", inSampleSize + "");
        return inSampleSize;
    }

}
