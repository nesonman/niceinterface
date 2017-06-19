package com.example.example.myapplication.remote;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.example.example.myapplication.db.ImageDatabase;
import com.example.example.myapplication.db.ImageTimeData;
import com.example.example.myapplication.db.RemoteImageDatabase;
import com.example.example.myapplication.utils.Const;
import com.example.example.myapplication.utils.IndexBuilder;
import com.example.example.myapplication.utils.IndexPair;
import com.example.example.myapplication.utils.KeyStoreHelper;
import com.example.example.myapplication.utils.TagGen;
import com.example.example.myapplication.utils.Utils;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

import org.apache.commons.lang3.StringUtils;
import org.crypto.sse.CryptoPrimitives;
import org.crypto.sse.remote.DynRH2LevStatelessClient;
import org.crypto.sse.remote.DynRH2LevStatelessServer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import cz.msebera.android.httpclient.Header;

public class DynRH2LevClientWrapper {

    public static AsyncHttpClient client = new AsyncHttpClient();
    public static SyncHttpClient syncClient = new SyncHttpClient();
    private static final String BASE_URL = "example.com";
    private static final String SSE_URL = "sse/";
    private static final String ACCOUNT_URL = "account/";

    /**
     * Tries to log the user in.
     */
    public static void login(Activity activity, String email, String password, AsyncHttpResponseHandler handler) {
        SharedPreferences pref = Utils.getUserSharedPreference(activity);
        byte[] clientSalt = Utils.JSONToByteArray(pref.getString(Const.LOCAL_PASSWORD_SALT_LABEL, ""));
        String passwordHash = Utils.bytesToHex(CryptoPrimitives.scrypt(password.getBytes(), clientSalt, Const.SCRYPT_CPU, Const.SCRYPT_MEM, Const.SCRYPT_PAR, Const.SCRYPT_LENGTH));
        RequestParams params = new RequestParams();
        params.put(Const.EMAIL_LABEL, email);
        params.put(Const.PASSWORD_LABEL, passwordHash);
        client.post(BASE_URL + ACCOUNT_URL + "login", params, handler);
    }

    public static void signup(Activity activity, String firstName, String lastName, String email, String password,
                              String securityQuestion, String securityAnswer, String securityQuestion2,
                              String securityAnswer2, String clientSalt, AsyncHttpResponseHandler handler) {
        String passwordHash = Utils.bytesToHex(CryptoPrimitives.scrypt(password.getBytes(), Utils.JSONToByteArray(clientSalt), Const.SCRYPT_CPU, Const.SCRYPT_MEM, Const.SCRYPT_PAR, Const.SCRYPT_LENGTH));
        byte[] keysalt = null;
        byte[] encPassword = null;
        try {
            keysalt = CryptoPrimitives.randomBytes(8);
            byte[] passwordKey = CryptoPrimitives.keyGenSetM(securityAnswer + securityAnswer2, keysalt, 100, 128);
            encPassword = CryptoPrimitives.encryptAES_CTR_Byte(passwordKey, CryptoPrimitives.randomBytes(16), password.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            handler.onFailure(500, null, null, null);
            return;
        }
        RequestParams params = new RequestParams();
        params.put(Const.FIRST_NAME_LABEL, firstName);
        params.put(Const.LAST_NAME_LABEL, lastName);
        params.put(Const.EMAIL_LABEL, email);
        params.put(Const.PASSWORD_LABEL, passwordHash);
        params.put(Const.SECURITY_QUESTION_LABEL, securityQuestion);
        params.put(Const.SECURITY_ANSWER_LABEL, securityAnswer);
        params.put(Const.SECURITY_QUESTION_LABEL + 2, securityQuestion2);
        params.put(Const.SECURITY_ANSWER_LABEL + 2, securityAnswer2);
        params.put(Const.LOCAL_PASSWORD_SALT_LABEL, clientSalt);
        params.put(Const.ENCRYPTED_PASSWORD_LABEL, Utils.byteArrayToJSON(encPassword));
        params.put(Const.ENCRYPTED_PASSWORD_SALT_LABEL, Utils.byteArrayToJSON(keysalt));
        client.post(BASE_URL + ACCOUNT_URL + "signup", params, handler);
    }

    /**
     * Attemps to generate the key
     * @param password
     * @return true if the key is generated and stored successfully, false otherwise.
     */
    public static boolean keyGen(Activity activity, String password) {
        try {
            SharedPreferences sharedPref = Utils.getUserSharedPreference(activity);
            String salt = sharedPref.getString(Const.SHARED_PREF_SALT, "[]");
            if (salt.equals("[]")) {
                return false;
            }
            Log.i("SHARED PREF SALT", salt);
            byte[] sk = DynRH2LevStatelessClient.keygen(password, Utils.JSONToByteArray(salt));
            // encrypt the key using the secure key in the android keystore
            byte[] encSk = KeyStoreHelper.encrypt(activity, sk);
            SharedPreferences.Editor editor = sharedPref.edit();
            // we store the encrypted secret key
            editor.putString("sk", Utils.byteArrayToJSON(encSk));
            editor.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Sends a single picture taken from the custom camera activity to the server.
     * @param activity
     * @param indexPair
     * @param handler
     */
    public static void updateSingleCustom(Activity activity, IndexPair indexPair, String imageFile, String thumbnailFile, String mediumFile,
                                          String imageName, AsyncHttpResponseHandler handler) throws Exception {
        SharedPreferences sharedPref = Utils.getUserSharedPreference(activity);
        String tempState = sharedPref.getString(Const.SHARED_PREF_STATE, null);
        if (tempState == null) {
            // TODO: throw some error to the user
            throw new Exception("state not found");
        }
        Map<String, Integer> state = Utils.JSONToState(tempState);
        byte[] sk = Utils.getSk(activity);
        // state is modified in the update
        Multimap<String, byte[]> tokenUp = DynRH2LevStatelessClient.update(sk, indexPair.lp1, state, IndexBuilder.FILE_NAME_LENGTH + 3);
        // save the new state
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Const.SHARED_PREF_STATE, Utils.stateToJSON(state));
        editor.commit();

        // if we are using the local mode for the app, do our "upload" locally
        if (Utils.isUsingLocalMode(activity)) {
            try {
                updateSingleCustomLocal(activity, tokenUp, thumbnailFile, imageFile, mediumFile, imageName);
                uploadImageTagsLocal(activity, indexPair, sk);
                // simulate a successful upload
                handler.onSuccess(200, new Header[]{}, "Update Local Success".getBytes());
            } catch (Exception e) {
                e.printStackTrace();
                // simulate an upload failure
                handler.onFailure(500, new Header[]{}, "Update Local Failure".getBytes(), null);
            }
            return;
        }

        // otherwise we interact with the cloud
        String updateJSON = Utils.tokenUpToJSON(tokenUp);
        RequestParams params = new RequestParams();;
        params.put(Const.UPDATE_TOKEN_LABEL, updateJSON);
        params.put(Const.PICTURE_LABEL + 1, new File(imageFile));
        params.put(Const.THUMBNAIL_LABEL + 1, new File(thumbnailFile));
        params.put(Const.MEDIUM_LABEL + 1, new File(mediumFile));
        params.put(Const.PICTURE_NAME_LABEL + 1, imageName);
        client.post(BASE_URL + SSE_URL + "updatemulti", params, handler);

        // backup the state
        backupState(activity, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.i("BACKUP STATE", new String(responseBody));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.i("BACKUP STATE", new String(responseBody));
            }
        });

        // upload the image tags
        uploadImageTags(activity, indexPair, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.i("UPLOAD TAGS", new String(responseBody));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.i("UPLOAD TAGS", new String(responseBody));
            }
        });
    }

    /**
     * Private method to update our EDS in local mode.
     * @param activity
     * @param tokenUp
     */
    private static void updateSingleCustomLocal(Activity activity, Multimap<String, byte[]> tokenUp, String thumb,
                                                String image, String medium, String imageName) throws Exception {
        // put the encrypted files into the file system
        File encThumbFile = Utils.makeFileInLocalThumbnailDir(activity, imageName);
        File encImageFile = Utils.makeFileInLocalImageDir(activity, imageName);
        //File encMediumFile = Utils.makeFileInLocalMediumDir(activity, imageName);;
        Files.write(Files.toByteArray(new File(thumb)), encThumbFile);
        Files.write(Files.toByteArray(new File(image)), encImageFile);
        //Files.write(Files.toByteArray(new File(medium)), encMediumFile);

        Log.i("updated", "updated");
        for (File f : Utils.getLocalUserEncryptedMediumDir(activity).listFiles()) {
            Log.i("UPDATE", f.getAbsolutePath());
        }

        // store the timestamp of the image we wrote
        RemoteImageDatabase tsdb = new RemoteImageDatabase(activity);
        tsdb.insertTimestamp(imageName);
        tsdb.close();

        // update our EDS
        File dictUp = Utils.getLocalUserDictionaryUpdates(activity);
        Map<String, byte[]> dictionaryUpdates = (HashMap<String, byte[]>) new ObjectInputStream(new FileInputStream(dictUp)).readObject();
        DynRH2LevStatelessServer.update(dictionaryUpdates, tokenUp);
        new ObjectOutputStream(new FileOutputStream(dictUp)).writeObject(dictionaryUpdates);
    }

    public static void updateMulti(Activity activity, IndexPair indexPair, List<String> imageNames, AsyncHttpResponseHandler handler) throws Exception {
        SharedPreferences sharedPref = Utils.getUserSharedPreference(activity);
        String tempState = sharedPref.getString(Const.SHARED_PREF_STATE, null);
        if (tempState == null) {
            // TODO: throw some error to the user
            throw new Exception("state not found");
        }
        Map<String, Integer> state = Utils.JSONToState(tempState);
        byte[] sk = Utils.getSk(activity);
        // state is modified in the update
        Multimap<String, byte[]> tokenUp = DynRH2LevStatelessClient.update(sk, indexPair.lp1, state, IndexBuilder.FILE_NAME_LENGTH + 3);
        // save the new state
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Const.SHARED_PREF_STATE, Utils.stateToJSON(state));
        editor.commit();

        // if we are using local mode, "upload" images locally
        if (Utils.isUsingLocalMode(activity)) {
            try {
                updateMultiLocal(activity, sk, tokenUp, imageNames, indexPair.fileToRename);
                uploadImageTagsLocal(activity, indexPair, sk);
                handler.onSuccess(200, new Header[]{}, "Success".getBytes());
            } catch (Exception e) {
                e.printStackTrace();
                handler.onFailure(500, new Header[]{}, "Failure".getBytes(), null);
            }
            return;
        }

        // otherwise we do our regular cloud update
        // get the json version of the update token
        String updateJSON = Utils.tokenUpToJSON(tokenUp);
        RequestParams updateParams = buildUpdateParams(activity, sk, updateJSON, imageNames);
        client.post(BASE_URL + SSE_URL + "updatemulti", updateParams, handler);

        // backup the state
        backupState(activity, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.i("BACKUP STATE", new String(responseBody));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.i("BACKUP STATE", new String(responseBody));
            }
        });

        // upload the image tags
        uploadImageTags(activity, indexPair, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.i("UPLOAD TAGS", new String(responseBody));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.i("UPLOAD TAGS", new String(responseBody));
            }
        });
    }

    private static void updateMultiLocal(Activity activity, byte[] sk, Multimap<String, byte[]> tokenUp,
                                         List<String> imageNames, Map<String, String> fileToRename) throws Exception {
        // store the images locally
        RemoteImageDatabase tsdb = new RemoteImageDatabase(activity);
        File thumbDir = Utils.getCurrentUserThumbnailDir(activity);
        File imageDir = Utils.getCurrentUserImageDir(activity);
        //File mediumDir = Utils.getCurrentUserMediumDir(activity);
        for (String imageName : imageNames) {
            File encThumbFile = Utils.makeFileInLocalThumbnailDir(activity, imageName);
            File encImageFile = Utils.makeFileInLocalImageDir(activity, imageName);
            File encMediumFile = Utils.makeFileInLocalMediumDir(activity, imageName);
            Files.write(Files.toByteArray(new File(thumbDir, imageName + ".jpg")), encThumbFile);
            Files.write(Files.toByteArray(new File(imageDir, imageName + ".jpg")), encImageFile);
            //Files.write(Files.toByteArray(new File(mediumDir, imageName + ".jpg")), encMediumFile);
            tsdb.insertTimestamp(imageName);
        }
        tsdb.close(); // free this resource
        // update our EDS
        File dictUp = Utils.getLocalUserDictionaryUpdates(activity);
        Map<String, byte[]> dictionaryUpdates = (HashMap<String, byte[]>) new ObjectInputStream(new FileInputStream(dictUp)).readObject();
        DynRH2LevStatelessServer.update(dictionaryUpdates, tokenUp);
        new ObjectOutputStream(new FileOutputStream(dictUp)).writeObject(dictionaryUpdates);
    }

    /**
     * A request to map each image to the encrypted tags for that image. Maybe merge this with the
     * upload request?
     * @param activity
     * @param indexPair
     */
    public static void uploadImageTags(Activity activity, IndexPair indexPair, AsyncHttpResponseHandler handler) throws Exception {
        byte[] sk = Utils.getSk(activity);

        if (Utils.isUsingLocalMode(activity)) {
            try {
                uploadImageTagsLocal(activity, indexPair, sk);
                handler.onSuccess(200, new Header[]{}, "tag success".getBytes());
            } catch (Exception e) {
                e.printStackTrace();
                handler.onFailure(500, new Header[]{}, "failure".getBytes(), null);
            }
            return;
        }

        Multimap<String, String> lp2 = indexPair.lp2;
        RequestParams params = new RequestParams();
        int count = 1;
        for (String imageName : lp2.keySet()) {
            params.put(Const.PICTURE_NAME_LABEL + count, imageName);
            String tagString = StringUtils.join(lp2.get(imageName), " ");
            String paddedTagString = Utils.rightPadding(tagString, Const.TAG_TOTAL_MAX_CHARS);
            byte[] encTags = CryptoPrimitives.encryptAES_CTR_Byte(sk, CryptoPrimitives.randomBytes(16), paddedTagString.getBytes());
            params.put(Const.TAG_LABEL + count, new ByteArrayInputStream(encTags));
            count++;
        }
        client.post(BASE_URL + SSE_URL + "uploadtags", params, handler);
    }

    public static void uploadImageTagsLocal(Activity activity, IndexPair indexPair, byte[] sk) throws Exception {
        Multimap<String, String> lp2 = indexPair.lp2;
        RemoteImageDatabase ridb = new RemoteImageDatabase(activity);
        for (String imageName : lp2.keySet()) {
            String tagString = StringUtils.join(lp2.get(imageName), " ");
            String paddedTagString = Utils.rightPadding(tagString, Const.TAG_TOTAL_MAX_CHARS);
            byte[] encTags = CryptoPrimitives.encryptAES_CTR_Byte(sk, CryptoPrimitives.randomBytes(16), paddedTagString.getBytes());
            ridb.insertTags(imageName, encTags);
        }
        ridb.close();
    }

    public static void query(Activity activity, String keyword, FileAsyncHttpResponseHandler handler) throws Exception {
        SharedPreferences sharedPref = Utils.getUserSharedPreference(activity);
        String tempState = sharedPref.getString(Const.SHARED_PREF_STATE, null);
        if (tempState == null) {
            // TODO: throw some error to the user
            throw new Exception("Can't get state");
        }
        Map<String, Integer> state = Utils.JSONToState(tempState);
        byte[] sk = Utils.getSk(activity);
        byte[][] queryToken = DynRH2LevStatelessClient.getQueryToken(sk, keyword, state);
        byte[] cmac = DynRH2LevStatelessClient.getCmacKey(sk);

        // if we are using local mode, then retrive it from our own file system
        if (Utils.isUsingLocalMode(activity)) {
            try {
                queryLocal(activity, queryToken, cmac, sk);
                handler.onSuccess(227, new Header[]{}, new File("null"));
            } catch (Exception e) {
                e.printStackTrace();
                handler.onFailure(500, new Header[]{}, null, new File("null"));
            }
            return;
        }

        Log.i("QUERY", "CLOUD");
        // otherwise retrieve things from the cloud
        String queryJSON = Utils.byteArrayToJSON2D(queryToken);
        String cmacJSON = Utils.byteArrayToJSON(cmac);
        Multimap<String, String> multimap = Utils.getCurrentUserTagIndex(activity);
        String cachedImages = StringUtils.join(multimap.get(keyword), ",");
        Log.i("CACHED", cachedImages);
        RequestParams params = new RequestParams();
        params.put(Const.CMAC_TOKEN_LABEL, cmacJSON);
        params.put(Const.QUERY_TOKEN_LABEL, queryJSON);
        params.put(Const.CACHED_IMAGE_LABEL, cachedImages);
        client.post(BASE_URL + SSE_URL + "query", params, handler);
    }

    private static void queryLocal(Activity activity, byte[][] queryToken, byte[] cmac, byte[] sk) throws Exception {
        Log.i("QUERY", "LOCAL");
        byte[][] arr = null;
        File dictUp = Utils.getLocalUserDictionaryUpdates(activity);
        File dict = Utils.getLocalUserDictionary(activity);
        Map<String, byte[]> dictionaryUpdates = (Map<String, byte[]>) new ObjectInputStream(new FileInputStream(dictUp)).readObject();
        Multimap<String, byte[]> dictionary = (Multimap<String, byte[]>) new ObjectInputStream(new FileInputStream(dict)).readObject();

        // now we make our query
        List<String> identifiers = DynRH2LevStatelessServer.query(cmac, queryToken, dictionary, arr, dictionaryUpdates);
        // we decrypt each file and put it in the thumbnail dir
        ImageDatabase idb = new ImageDatabase(activity);
        RemoteImageDatabase ridb = new RemoteImageDatabase(activity);
        Multimap<String, String> multimap = Utils.getCurrentUserTagIndex(activity);
        for (String ident : identifiers) {
            File f = new File(Utils.getLocalUserEncryptedThumbnailDir(activity) + "/" + ident);
            if (!f.exists()) {
                continue; // if the file doesn't exist, it may have been deleted, so skip to the next file
            }
            File output = Utils.makeFileInUserThumbnailDir(activity, ident + ".jpg");
            Files.write(Files.toByteArray(f), output);
            // we update the timestamp of the time we wrote the file
            idb.addImageName(ident);

            // now get the image tags
            byte[] encTags = ridb.getTags(ident);
            String tags = new String(CryptoPrimitives.decryptAES_CTR_String(encTags, sk));
            for (String tag : tags.split(" ")) {
                if (!multimap.get(tag).contains(ident)) {
                    multimap.put(tag, ident);
                }
            }
            idb.addTags(ident, encTags);
        }
        idb.close();
        ridb.close();
        Utils.saveCurrentUserTagIndex(activity, multimap);
    }

    public static void queryTimestamp(Activity activity, long numRequest, FileAsyncHttpResponseHandler handler) {
        List<String> imageNamesLocal = new ArrayList<>();
        for (File f : Utils.getCurrentUserThumbnailDir(activity).listFiles()) {
            String filename = f.getName();
            if (filename.endsWith(".jpg")) {
                imageNamesLocal.add(filename.replace(".jpg", ""));
            }
        }
        if (Utils.isUsingLocalMode(activity)) {
            try {
                queryTimestampLocal(activity, imageNamesLocal, numRequest);
                handler.onSuccess(200, new Header[]{}, new File("null"));
            } catch (Exception e) {
                e.printStackTrace();
                handler.onFailure(500, new Header[]{}, null, new File("null"));
            }
            return;
        }
        RequestParams params = new RequestParams();
        params.put(Const.NUM_REQUEST_LABEL, numRequest);
        params.put(Const.LOCAL_IMAGE_NAMES_LABEL, Utils.listToString(imageNamesLocal));
        client.post(BASE_URL + SSE_URL + "querytimestamp", params, handler);
    }

    public static void queryTimestampLocal(Activity activity, List<String> localFileNames, long numRequest) throws Exception {
        SharedPreferences pref = Utils.getLocalSharedPref(activity);
        byte[] sk = Utils.getSk(activity);
        SharedPreferences.Editor editor = pref.edit();

        RemoteImageDatabase ridb = new RemoteImageDatabase(activity);
        List<ImageTimeData> data = ridb.getImagesNotInSet(new HashSet<>(localFileNames), numRequest);
        ImageDatabase idb = new ImageDatabase(activity);
        Multimap<String, String> multimap = Utils.getCurrentUserTagIndex(activity);
        for (ImageTimeData d : data) {
            File f = new File(Utils.getLocalUserEncryptedThumbnailDir(activity) + "/" + d.imageName);
            File output = Utils.makeFileInUserThumbnailDir(activity, d.imageName + ".jpg");
            Files.write(Files.toByteArray(f), output);
            // we update the timestamp of the time we wrote the file
            idb.addImageName(d.imageName);
            // we get the tags
            byte[] encTags = ridb.getTags(d.imageName);
            String tags = new String(CryptoPrimitives.decryptAES_CTR_String(encTags, sk));
            idb.addTags(d.imageName, encTags);
            for (String tag : tags.split(" ")) {
                if (!multimap.get(tag).contains(d.imageName)) {
                    multimap.put(tag, d.imageName);
                }
            }
        }
        ridb.close();
        idb.close();
        Utils.saveCurrentUserTagIndex(activity, multimap);
        editor.commit();
    }

    /**
     * Sends a request to the server to delete the image
     * @param activity
     * @param imageName
     * @param handler
     */
    public static void delete(Activity activity, String imageName, AsyncHttpResponseHandler handler) {
        if (Utils.isUsingLocalMode(activity)) {
            deleteLocal(activity, imageName);
            handler.onSuccess(200, new Header[]{}, "Delete Local Success".getBytes());
            return;
        }

        RequestParams params = new RequestParams();
        params.put(Const.PICTURE_NAME_LABEL, imageName);
        client.post(BASE_URL + SSE_URL + "delete", params, handler);
    }

    public static void deleteLocal(Activity activity, String imageName) {
        File encImageFile = new File(Utils.getLocalUserEncryptedImageDir(activity), imageName);
        File encThumbFile = new File(Utils.getLocalUserEncryptedThumbnailDir(activity), imageName);

        // delete files
        encImageFile.delete();
        encThumbFile.delete();

        // delete from database
        RemoteImageDatabase ridb = new RemoteImageDatabase(activity);
        ridb.deleteImage(imageName);
        ridb.close();
    }

    public static void clearImageCookie(Activity activity, AsyncHttpResponseHandler handler) {
        if (Utils.isUsingLocalMode(activity)) {
            SharedPreferences pref = Utils.getLocalSharedPref(activity);
            SharedPreferences.Editor editor = pref.edit();
            editor.remove(Const.Local.LAST_IMAGE_QUERIED_ID);
            editor.commit();
            handler.onSuccess(200, new Header[]{}, "Success".getBytes());
            return;
        }
        client.post(BASE_URL + SSE_URL + "removeimagecookie", handler);
    }

    public static void getFullImage(Activity activity, String imageName, FileAsyncHttpResponseHandler handler) {
        if (Utils.isUsingLocalMode(activity)) {
            try {
                getFullImageLocal(activity, imageName);
                handler.onSuccess(200, new Header[]{}, new File("null"));
            } catch (Exception e) {
                e.printStackTrace();
                handler.onFailure(500, new Header[]{}, null, new File("null"));
            }
            return;
        }
        RequestParams params = new RequestParams();
        params.put(Const.PICTURE_NAME_LABEL, imageName);
        client.post(BASE_URL + SSE_URL + "queryfullimage", params, handler);
    }

    private static void getFullImageLocal(Activity activity, String imageName) throws Exception {
        File encImage = new File(Utils.getLocalUserEncryptedImageDir(activity), imageName);
        byte[] encData = Files.toByteArray(encImage);
        Files.write(encData, new File(Utils.getCurrentUserImageDir(activity), imageName + ".jpg"));
    }

    public static void getMediumImage(Activity activity, String imageName, AsyncHttpResponseHandler handler) {
        RequestParams params = new RequestParams();
        params.put(Const.PICTURE_NAME_LABEL, imageName);
        client.post(BASE_URL + SSE_URL + "querymediumimage", params, handler);
    }

    public static void requestRecovery(Activity activity, String email, AsyncHttpResponseHandler handler) {
        RequestParams params = new RequestParams();
        params.put(Const.EMAIL_LABEL, email);
        client.post(BASE_URL + ACCOUNT_URL + "requestrecover", params, handler);
    }

    public static void recoveryPassword(Activity activity, String code, AsyncHttpResponseHandler handler) {
        RequestParams params = new RequestParams();
        params.put(Const.CODE_LABEL, code);
        client.post(BASE_URL + ACCOUNT_URL + "recoverpassword", params, handler);
    }

    // sends the current state (encrypted) to the server so it can back it up
    public static void backupState(Activity activity, AsyncHttpResponseHandler handler) throws Exception {
        SharedPreferences sharedPref = Utils.getUserSharedPreference(activity);
        String tempState = sharedPref.getString(Const.SHARED_PREF_STATE, null);
        if (tempState == null) {
            // TODO: throw some error to the user
            throw new Exception("Can't get state");
        }
        byte[] sk = Utils.getSk(activity);
        // encrypt our state
        byte[] encState = CryptoPrimitives.encryptAES_CTR_Byte(sk, CryptoPrimitives.randomBytes(16), tempState.getBytes());

        RequestParams params = new RequestParams();
        params.put(Const.STATE_LABEL, new ByteArrayInputStream(encState));

        // send our state to the server
        client.post(BASE_URL + SSE_URL + "backupstate", params, handler);
    }

    public static void requestSetupNewDevice(Activity activity, String email, AsyncHttpResponseHandler handler) {
        RequestParams params = new RequestParams();
        params.put(Const.EMAIL_LABEL, email);
        client.post(BASE_URL + ACCOUNT_URL + "requestregister", params, handler);
    }

    public static void setupNewDevice(Activity activity, String code, AsyncHttpResponseHandler handler) {
        RequestParams params = new RequestParams();
        params.put(Const.CODE_LABEL, code);
        client.post(BASE_URL + ACCOUNT_URL + "registerdevice", params, handler);
    }

    /**
     * Builds the request param to upload to the server.
     * @param updateJSON
     */
    private static RequestParams buildUpdateParams(Activity activity, byte[] sk, String updateJSON, List<String> imageNames) throws Exception {
        RequestParams params = new RequestParams();
        params.put(Const.UPDATE_TOKEN_LABEL, updateJSON);
        int count = 1;
        // TODO: paralellize this operation
        File thumbDir = Utils.getCurrentUserThumbnailDir(activity);
        File imageDir = Utils.getCurrentUserImageDir(activity);
        File mediumDir = Utils.getCurrentUserMediumDir(activity);
        for (String imageName : imageNames) {
            params.put(Const.THUMBNAIL_LABEL + count, new File(thumbDir, imageName + ".jpg"));
            params.put(Const.PICTURE_LABEL + count, new File(imageDir, imageName + ".jpg"));
            params.put(Const.MEDIUM_LABEL + count, new File(mediumDir, imageName + ".jpg"));
            params.put(Const.PICTURE_NAME_LABEL + count, imageName);
            count++;
        }
        Log.i("PARAMS", params.toString());
        return params;
    }

}
