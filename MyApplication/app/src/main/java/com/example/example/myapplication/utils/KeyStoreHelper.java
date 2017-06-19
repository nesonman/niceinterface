package com.example.example.myapplication.utils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import org.crypto.sse.CryptoPrimitives;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * This class contains helper functions to use with the android keystore
 */
public class KeyStoreHelper {

    // private static final byte[] FIXED_IV = {127, 40, 53, 78, 90, 73, 116, 40, 28, 60, 35, 28, 45, 69, 34, 123};

    public static SecretKey getKey(Activity activity) {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            // we want to generate a new key for each user of the app for security purposes
            if (keyStore.containsAlias(Const.KEYSTORE_ALIAS + Utils.getCurrentEmailHashInSharedPref(activity))) {
                Log.i("KEYSTORE", "key already exists");
                return (SecretKey) keyStore.getKey(Const.KEYSTORE_ALIAS + Utils.getCurrentEmailHashInSharedPref(activity), null);
            }

            KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            KeyGenParameterSpec keySpec = new KeyGenParameterSpec.Builder(Const.KEYSTORE_ALIAS + Utils.getCurrentEmailHashInSharedPref(activity),
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CTR)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(false)
                    .build();
            kg.init(keySpec);
            Log.i("KEYSTORE", "new key generated");
            // generate iv
            byte[] iv = CryptoPrimitives.randomBytes(16);
            SharedPreferences pref = Utils.getUserSharedPreference(activity);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString(Const.KEYSTORE_IV, Utils.byteArrayToJSON(iv));
            editor.commit();
            return kg.generateKey();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] encrypt(Activity activity, byte[] data) {
        try {
            SecretKey sk = getKey(activity);
            //Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
            // get the iv stored in the shared preferences
            cipher.init(Cipher.ENCRYPT_MODE, sk, new IvParameterSpec(getIV(activity)));
            //return cipher.doFinal(data);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);
            cipherOutputStream.write(data);
            cipherOutputStream.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] decrypt(Activity activity, byte[] encData) {
        try {
            SecretKey sk = getKey(activity);
            //Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
            //GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, cipher.getIV());
            cipher.init(Cipher.DECRYPT_MODE, sk, new IvParameterSpec(getIV(activity)));
            //return cipher.doFinal(encData);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(encData);
            CipherInputStream cipherInputStream = new CipherInputStream(inputStream, cipher);
            int nRead;
            byte[] data = new byte[10];
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            while ((nRead = cipherInputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            return buffer.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] getIV(Activity activity) {
        SharedPreferences pref = Utils.getUserSharedPreference(activity);
        return Utils.JSONToByteArray(pref.getString(Const.KEYSTORE_IV, ""));
    }

}
