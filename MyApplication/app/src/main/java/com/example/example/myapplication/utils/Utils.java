package com.example.example.myapplication.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.example.example.myapplication.db.ImageDatabase;
import com.example.example.myapplication.remote.DynRH2LevClientWrapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.commons.lang3.StringEscapeUtils;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.crypto.sse.CryptoPrimitives;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import cz.msebera.android.httpclient.Header;

public class Utils {
    private static final Gson GSON = new Gson();

    /**
     * Takes in a byte array and encode it as a json compatible string.
     *
     * @param bytes
     * @return a string that represents the byte array.
     */
    public static String byteArrayToJSON(byte[] bytes) {
        return GSON.toJson(bytes);
    }

    /**
     * Takes in a string that represents a byte array and converts it to a real byte array
     *
     * @param bytes
     * @return a byte array
     */
    public static byte[] JSONToByteArray(String bytes) {
        return GSON.fromJson(bytes, byte[].class);
    }

    /**
     * Turns an update token (a multimap) into a json string)
     * @param tokenUp
     * @return
     */
    public static String tokenUpToJSON(Multimap<String, byte[]> tokenUp) {
        return GSON.toJson(tokenUp.asMap());
    }

    public static String byteArrayToJSON2D(byte[][] bytes) {
        return GSON.toJson(bytes);
    }

    public static byte[][] JSONToByteArray2D(String bytes) {
        return GSON.fromJson(bytes, byte[][].class);
    }

    public static Multimap<String, byte[]> JSONToTokenUp(String tokenUp) {
        String[] pairs = tokenUp.replaceAll("^\\{|\\}$", "").split(",(?![^(\\[]*[\\])])");
        // we use the lexigraphically ordered multimap so the map is history independent.
        Multimap<String, byte[]> multi = TreeMultimap.create(Ordering.natural(), Ordering.usingToString());
        for (int i = 0; i < pairs.length; i++) {
            String[] temp = pairs[i].split(":");
            // make sure to keep the string json format
            String key = StringEscapeUtils.unescapeJson(temp[0].trim().replaceAll("^\"|\"$", ""));
            String values = temp[1].trim();
            // if we split badly, join things back up
            while (!values.endsWith("]]")) {
                values += "," + pairs[++i].trim();
            }
            List<byte[]> realValues = new LinkedList<>();
            String[] bas = values.replaceAll("^\\[|\\]$", "").split(",(?![^(\\[]*[\\])])");
            for (String ba : bas) {
                realValues.add(JSONToByteArray(ba));
            }
            multi.putAll(key, realValues);
        }
        return multi;
    }

    public static String stateToJSON(Map<String, Integer> state) {
        return GSON.toJson(state);
    }

    public static Map<String, Integer> JSONToState(String state) {
        Type type = new TypeToken<Map<String, Integer>>(){}.getType();
        return GSON.fromJson(state, type);
    }

    public static String[] concat(String[] a, String[] b) {
        int aLen = a.length;
        int bLen = b.length;
        String[] c= new String[aLen + bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }

    public static void unpackThumbnailsZip(Activity activity, File zip) throws Exception {
        InputStream is;
        ZipInputStream zis;
        String filename;
        is = new FileInputStream(zip);
        zis = new ZipInputStream(new BufferedInputStream(is));
        ZipEntry ze;
        byte[] buffer = new byte[1024];
        byte[] sk = Utils.getSk(activity);
        ImageDatabase idb = new ImageDatabase(activity);
        Multimap<String, String> multimap = getCurrentUserTagIndex(activity);
        while ((ze = zis.getNextEntry()) != null) {
            // put the timestamp in the database
            idb.addImageName(ze.getName().replace(".tag", ""));
            // if this is a tag file
            if (ze.getName().endsWith(".tag")) {
                String imageName = ze.getName().replace(".tag", "");
                int count = 0;
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                while ((count = zis.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, count);
                }
                byte[] encTags = outputStream.toByteArray();
                String tags = new String(CryptoPrimitives.decryptAES_CTR_String(encTags, sk));
                for (String tag : tags.split(" ")) {
                    if (!multimap.get(tag).contains(imageName)) {
                        multimap.put(tag, imageName);
                    }
                }
                idb.addTags(imageName, encTags);
            } else {
                filename = ze.getName() + ".jpg";
                int count = 0;

                File file = makeFileInUserThumbnailDir(activity, filename);
                Log.i("UNZIPPING", file.getAbsolutePath());
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                while ((count = zis.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, count);
                }
                FileOutputStream fout = new FileOutputStream(file);
                byte[] encBytes = outputStream.toByteArray();
                fout.write(encBytes);
                outputStream.close();
                fout.close();
                zis.closeEntry();
            }
        }
        zis.close();
        idb.close();
        saveCurrentUserTagIndex(activity, multimap);
    }

    /**
     * The difference between this and the unzip thumbnail function is that this doesn't log the time
     * of the image being received.
     * @param activity
     * @param zip
     * @return
     * @throws IOException
     */
    public static boolean unpackImageZip(Activity activity, File zip) throws Exception {
        InputStream is;
        ZipInputStream zis;
        String filename;
        is = new FileInputStream(zip);
        zis = new ZipInputStream(new BufferedInputStream(is));
        ZipEntry ze;
        byte[] buffer = new byte[1024];
        while ((ze = zis.getNextEntry()) != null) {
            filename = ze.getName() + ".jpg";
            int count = 0;
            File file = makeFileInUserImageDir(activity, filename);
            Log.i("UNZIPPING FULL", file.getAbsolutePath());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            while ((count = zis.read(buffer)) != -1) {
                outputStream.write(buffer, 0, count);
            }
            byte[] encBytes = outputStream.toByteArray();
            FileOutputStream fout = new FileOutputStream(file);
            fout.write(encBytes);
            outputStream.close();
            fout.close();
            zis.closeEntry();
        }
        zis.close();
        return true;
    }

    public static boolean unpackMediumZip(Activity activity, File zip) throws Exception {
        InputStream is;
        ZipInputStream zis;
        String filename;
        is = new FileInputStream(zip);
        zis = new ZipInputStream(new BufferedInputStream(is));
        ZipEntry ze;
        byte[] buffer = new byte[1024];
        while ((ze = zis.getNextEntry()) != null) {
            filename = ze.getName() + ".jpg";
            int count = 0;
            File file = makeFileInUserMediumDir(activity, filename);
            Log.i("UNZIPPING MEDIUM", file.getAbsolutePath());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            while ((count = zis.read(buffer)) != -1) {
                outputStream.write(buffer, 0, count);
            }
            byte[] encBytes = outputStream.toByteArray();FileOutputStream fout = new FileOutputStream(file);
            fout.write(encBytes);
            outputStream.close();
            fout.close();
            zis.closeEntry();
        }
        zis.close();
        return true;
    }

    /**
     * Logs the current user who is interacting with the app in the shared preferences
     * @param activity
     * @param email
     */
    public static void putCurrentEmailInSharedPref(Activity activity, String email) {
        // when users sign up, put their email in the shared preferences so we know who just signed up
        SharedPreferences sharedPref = activity.getSharedPreferences(Const.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Const.SHARED_PREF_EMAIL, email);
        editor.commit();
    }

    public static String getCurrentEmailHashInSharedPref(Activity activity) {
        SharedPreferences sharedPref = activity.getSharedPreferences(Const.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        return md5(sharedPref.getString(Const.SHARED_PREF_EMAIL, ""));
    }

    public static SharedPreferences getLocalSharedPref(Activity activity) {
        return activity.getSharedPreferences(Const.SHARED_PREFERENCE_NAME + "." + Const.Local.LOCAL_DIRECTORY, Context.MODE_PRIVATE);
    }

    /**
     * Get the shared preference of the current user of the app. This is the normal shared preference folder
     * with the user's email concatenated at the end.
     * @param activity
     * @return the shared preference of this particular user
     */
    public static SharedPreferences getUserSharedPreference(Activity activity) {
        SharedPreferences sharedPref = activity.getSharedPreferences(Const.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        String userEmail = sharedPref.getString(Const.SHARED_PREF_EMAIL, null);
        if (isUsingLocalMode(activity)) {
            // get local shared pref instead
            return getLocalSharedPref(activity);
        }
        String emailHash = md5(userEmail);
        String userPref = Const.SHARED_PREFERENCE_NAME + "." + emailHash;
        return activity.getSharedPreferences(userPref, Context.MODE_PRIVATE);
    }

    public static String md5(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(input.getBytes());
            byte[] data = digest.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < data.length; i++) {
                String hex = Integer.toHexString(0xff & data[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Makes the local directory for the user. Makes a thumbnail directory as well as a full image directory;
     * also make a medium directory
     * @param activity
     */
    public static void makeUserDirectory(Activity activity) {
        File dir = getCurrentUserDir(activity);
        if (!dir.exists()) {
            dir.mkdir();
        }
        File thumbnailDir = new File(dir + Const.THUMBNAIL_DIR);
        if (!thumbnailDir.exists()) {
            thumbnailDir.mkdir();
        }
        File fullImageDir = new File(dir + Const.FULL_IMAGE_DIR);
        if (!fullImageDir.exists()) {
            fullImageDir.mkdir();
        }
        File videoDir = new File(dir + Const.VIDEO_DIR);
        if (!videoDir.exists()) {
            videoDir.mkdir();
        }
        File mediumImageDir = new File(dir + Const.MEDIUM_IMAGE_DIR);
        if (!mediumImageDir.exists()) {
            mediumImageDir.mkdir();
        }
    }

    public static void makeLocalUsecaseDirectory(Activity activity) throws IOException {
        File dir = getLocalUsecaseDirectory(activity);
        if (!dir.exists()) {
            dir.mkdir();
        }
        // make the "client" folders
        File thumbnailDir = new File(dir + Const.THUMBNAIL_DIR);
        if (!thumbnailDir.exists()) {
            thumbnailDir.mkdir();
        }
        File fullImageDir = new File(dir + Const.FULL_IMAGE_DIR);
        if (!fullImageDir.exists()) {
            fullImageDir.mkdir();
        }
        File videoDir = new File(dir + Const.VIDEO_DIR);
        if (!videoDir.exists()) {
            videoDir.mkdir();
        }
        File mediumImageDir = new File(dir + Const.MEDIUM_IMAGE_DIR);
        if (!mediumImageDir.exists()) {
            mediumImageDir.mkdir();
        }

        // make the "server" folders
        File sseInfo = new File(dir + Const.Local.SSE_DIR);
        if (!sseInfo.exists()) {
            sseInfo.mkdir();
        }
        File encImageDir = new File(dir + Const.Local.ENC_IMAGE_DIR);
        if (!encImageDir.exists()) {
            encImageDir.mkdir();
        }
        File encThumbDir = new File(dir + Const.Local.ENC_THUMBNAIL_DIR);
        if (!encThumbDir.exists()) {
            encThumbDir.mkdir();
        }
        File encMediumDir = new File(dir + Const.Local.ENC_MEDIUM_DIR);
        if (!encMediumDir.exists()) {
            encMediumDir.mkdir();
        }

        // populate the server folders with empty data structures
        File dictionaryUpdates = new File(dir + Const.Local.SSE_DIR + Const.Local.DICT_UPDATE_FILE);
        File dictionary = new File(dir + Const.Local.SSE_DIR + Const.Local.DICT_FILE);
        new ObjectOutputStream(new FileOutputStream(dictionaryUpdates)).writeObject(new HashMap<String, byte[]>());
        new ObjectOutputStream(new FileOutputStream(dictionary)).writeObject(ArrayListMultimap.<String, byte[]>create());
    }

    public static File getLocalUserDictionaryUpdates(Activity activity) {
        return new File(getLocalUsecaseDirectory(activity) + Const.Local.SSE_DIR + Const.Local.DICT_UPDATE_FILE);
    }

    public static File getLocalUserDictionary(Activity activity) {
        return new File(getLocalUsecaseDirectory(activity) + Const.Local.SSE_DIR + Const.Local.DICT_FILE);
    }

    public static File getLocalUserEncryptedThumbnailDir(Activity activity) {
        return new File(getLocalUsecaseDirectory(activity) + Const.Local.ENC_THUMBNAIL_DIR);
    }

    public static File getLocalUserEncryptedImageDir(Activity activity) {
        return new File(getLocalUsecaseDirectory(activity) + Const.Local.ENC_IMAGE_DIR);
    }

    public static File getLocalUserEncryptedMediumDir(Activity activity) {
        return new File(getLocalUsecaseDirectory(activity) + Const.Local.ENC_MEDIUM_DIR);
    }

    public static File makeFileInLocalThumbnailDir(Activity activity, String filename) {
        return new File(getLocalUserEncryptedThumbnailDir(activity) + "/" + filename);
    }

    public static File makeFileInLocalImageDir(Activity activity, String filename) {
        return new File(getLocalUserEncryptedImageDir(activity) + "/" + filename);
    }

    public static File makeFileInLocalMediumDir(Activity activity, String filename) {
        return new File(getLocalUserEncryptedMediumDir(activity) + "/" + filename);
    }


    /**
     * Creates a file in the current user's own internal file directory.
     * @param activity
     * @param filename
     * @return
     */
    public static File makeFileInUserThumbnailDir(Activity activity, String filename) {
        return new File(getCurrentUserDir(activity) + Const.THUMBNAIL_DIR + filename);
    }

    public static File makeFileInUserImageDir(Activity activity, String filename) {
        return new File(getCurrentUserDir(activity) + Const.FULL_IMAGE_DIR + filename);
    }

    public static File makeFileInUserMediumDir(Activity activity, String filename) {
        return new File(getCurrentUserDir(activity) + Const.MEDIUM_IMAGE_DIR + filename);
    }

    public static File getCurrentUserThumbnailDir(Activity activity) {
        File dir = getCurrentUserDir(activity);
        return new File(dir, Const.THUMBNAIL_DIR);
    }

    public static File getCurrentUserImageDir(Activity activity) {
        File dir = getCurrentUserDir(activity);
        return new File(dir, Const.FULL_IMAGE_DIR);
    }

    public static File getCurrentUserMediumDir(Activity activity) {
        File dir = getCurrentUserDir(activity);
        return new File(dir, Const.MEDIUM_IMAGE_DIR);
    }

    public static File getCurrentUserVideoDir(Activity activity) {
        File dir = getCurrentUserDir(activity);
        return new File(dir, Const.VIDEO_DIR);
    }

    public static File getCurrentUserDir(Activity activity) {
        SharedPreferences sharedPref = activity.getSharedPreferences(Const.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        String userEmail = sharedPref.getString(Const.SHARED_PREF_EMAIL, null);
        // if we are using local mode
        if (isUsingLocalMode(activity)) {
            return getLocalUsecaseDirectory(activity);
        }
        String emailHash = md5(userEmail);
        return new File(activity.getFilesDir(), emailHash);
    }

    public static Multimap<String, String> getCurrentUserTagIndex(Activity activity) throws Exception {
        File dir = getCurrentUserDir(activity);
        File tagIndexFile = new File(dir, Const.TAG_INDEX);
        Multimap<String, String> multimap;
        byte[] sk = Utils.getSk(activity);
        if (!tagIndexFile.exists()) {
            // if this file doesn't exist, write one
            multimap = ArrayListMultimap.create();
            byte[] mapBytes = serialize(multimap);
            byte[] encMapBytes = CryptoPrimitives.encryptAES_CTR_Byte(sk, CryptoPrimitives.randomBytes(16), mapBytes);
            Files.write(encMapBytes, tagIndexFile);
        } else {
            byte[] encMapBytes = Files.toByteArray(tagIndexFile);
            byte[] mapBytes = CryptoPrimitives.decryptAES_CTR_String(encMapBytes, sk);
            multimap = (Multimap<String, String>) deserialize(mapBytes);
        }
        return multimap;
    }

    public static void saveCurrentUserTagIndex(Activity activity, Multimap<String, String> multimap) throws Exception {
        File dir = getCurrentUserDir(activity);
        File tagIndexFile = new File(dir, Const.TAG_INDEX);
        byte[] sk = Utils.getSk(activity);
        byte[] mapBytes = serialize(multimap);
        byte[] encMapBytes = CryptoPrimitives.encryptAES_CTR_Byte(sk, CryptoPrimitives.randomBytes(16), mapBytes);
        Files.write(encMapBytes, tagIndexFile);
    }

    public static File getLocalUsecaseDirectory(Activity activity) {
        return new File(activity.getFilesDir(), Const.Local.LOCAL_DIRECTORY);
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static Bitmap rotateBitmapIfRequired(Bitmap img, Uri selectedImage) {
        try {
            ExifInterface ei = new ExifInterface(selectedImage.getPath());
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateBitmap(img, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateBitmap(img, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateBitmap(img, 270);
                default:
                    return img;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return img;
        }
    }

    public static Bitmap scaleBitmap(Bitmap source, int wantedWidth, int wantedHeight) {
        Bitmap output = Bitmap.createBitmap(wantedWidth, wantedHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Matrix m = new Matrix();
        m.setScale((float) wantedWidth / source.getWidth(), (float) wantedHeight / source.getHeight());
        canvas.drawBitmap(source, m, new Paint());
        return output;
    }

    public static boolean isUsingLocalMode(Activity activity) {
        SharedPreferences sharedPref = activity.getSharedPreferences(Const.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        String userEmail = sharedPref.getString(Const.SHARED_PREF_EMAIL, null);

        if (userEmail != null && userEmail.equals("local")) {
            return true;
        }
        return false;
    }

    public static Bitmap cropBitmap(Bitmap source) {
        if (source.getWidth() >= source.getHeight()){

            return Bitmap.createBitmap(
                    source,
                    source.getWidth()/2 - source.getHeight()/2,
                    0,
                    source.getHeight(),
                    source.getHeight()
            );

        }else{

            return Bitmap.createBitmap(
                    source,
                    0,
                    source.getHeight()/2 - source.getWidth()/2,
                    source.getWidth(),
                    source.getWidth()
            );
        }
    }

    public static String listToString(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String el : list) {
            sb.append(el + " ");
        }
        return sb.toString().trim();
    }

    /**
     * Attempts to get the current location of the phone.
     * @param activity
     * @throws SecurityException is thrown if the user has not granted permission
     */
    public static Location getLocation(Activity activity) throws SecurityException {
        LocationManager lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = lm.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location l = lm.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                bestLocation = l;
            }
        }
        if (bestLocation == null) {
            return null;
        }
        return bestLocation;
    }

    /**
     * Hashes the password for the local usecase
     * @param password
     * @param salt
     * @return
     */
    public static String hashPassword(String password, String salt) {
        if (password == null) return null;
        return Hashing.sha256().hashString(password + salt, StandardCharsets.UTF_8).toString();
    }

    public static String rightPadding(String str, int num) {
        return String.format("%1$-" + num + "s", str);
    }

    public static byte[] serialize(Object obj) throws IOException {
        try(ByteArrayOutputStream b = new ByteArrayOutputStream()){
            try(ObjectOutputStream o = new ObjectOutputStream(b)){
                o.writeObject(obj);
            }
            return b.toByteArray();
        }
    }

    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try(ByteArrayInputStream b = new ByteArrayInputStream(bytes)){
            try(ObjectInputStream o = new ObjectInputStream(b)){
                return o.readObject();
            }
        }
    }

    public static byte[] getSk(Activity activity) throws Exception {
        SharedPreferences sharedPref = Utils.getUserSharedPreference(activity);
        String tempSK = sharedPref.getString(Const.SHARED_PREF_SK, null);
        if (tempSK == null) {
            // TODO: throw some error to the user
            throw new Exception("secret key not found");
        }
        byte[] encSk = Utils.JSONToByteArray(tempSK);
        return KeyStoreHelper.decrypt(activity, encSk);
    }

    public static String bytesToHex(byte[] input) {
        final StringBuilder builder = new StringBuilder();
        for(byte b : input) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
