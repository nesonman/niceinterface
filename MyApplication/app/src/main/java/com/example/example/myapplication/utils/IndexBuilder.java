package com.example.example.myapplication.utils;

import android.app.Activity;
import android.net.Uri;

import com.example.example.myapplication.BatchTaggingActivity;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.jar.Pack200;

/**
 * Parallelized method to build multimap.
 */
public class IndexBuilder {

    private static final char[] VALID_CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456879".toCharArray();
    public static final int FILE_NAME_LENGTH = 30;

    /**
     * This is paralleized so that we can more efficiently tag multiple images at the same time;
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    /*public static IndexPair buildIndexFromGallery(final Activity activity, String[] fileNames) throws InterruptedException, ExecutionException {
        // make as many threads as we can
        int threads = 0;
        if (Runtime.getRuntime().availableProcessors() > fileNames.length) {
            threads = fileNames.length;
        } else {
            threads = Runtime.getRuntime().availableProcessors();
        }

        ExecutorService service = Executors.newFixedThreadPool(threads);

        ArrayList<String[]> inputs = new ArrayList<>(threads);

        for (int i = 0; i < threads; i++) {
            String[] tmp;
            if (i == threads - 1) {
                tmp = new String[fileNames.length / threads + fileNames.length % threads];
                for (int j = 0; j < fileNames.length / threads + fileNames.length % threads; j++) {
                    tmp[j] = fileNames[(fileNames.length / threads) * i + j];
                }
            } else {
                tmp = new String[fileNames.length / threads];
                for (int j = 0; j < fileNames.length / threads; j++) {

                    tmp[j] = fileNames[(fileNames.length / threads) * i + j];
                }
            }
            inputs.add(i, tmp);
        }

        List<Future<IndexPair>> futures = new ArrayList<>();
        for (final String[] input : inputs) {
            Callable<IndexPair> callable = new Callable<IndexPair>() {
                @Override
                public IndexPair call() throws Exception {
                    return extractDocs(activity, input);
                }
            };
            futures.add(service.submit(callable));
        }
        service.shutdown();

        Multimap<String, String> lp1 = ArrayListMultimap.create();
        Multimap<String, String> lp2 = ArrayListMultimap.create();
        Map<String, String> fileToRename = new HashMap<>();
        for (Future<IndexPair> future : futures) {
            Set<String> keywords1 = future.get().lp1.keySet();
            Set<String> keywords2 = future.get().lp2.keySet();
            fileToRename.putAll(future.get().fileToRename);
            for (String key : keywords1) {
                lp1.putAll(key, future.get().lp1.get(key));
            }
            for (String key : keywords2) {
                lp2.putAll(key, future.get().lp2.get(key));
            }
        }
        return new IndexPair(lp1, lp2, fileToRename);
    }*/

    /*public static IndexPair buildIndexFromTaken(Activity activity, String mCurrentPhotoPath, String mRealPhotoPath) throws IOException {
        Multimap<String, String> lookup1 = ArrayListMultimap.create();
        Multimap<String, String> lookup2 = ArrayListMultimap.create();
        Map<String, String> fileToRename = new HashMap<>();
        List<String> tokens = TagGen.generateTagFromTakenImage(activity, mCurrentPhotoPath, mRealPhotoPath);
        String fakeName = csRandomAlphaNumericString(FILE_NAME_LENGTH);
        fileToRename.put(mRealPhotoPath, fakeName);
        for (String token : tokens) {
            if (!lookup1.get(token).contains(fakeName)) {
                lookup1.put(token, fakeName);
            }

            // lookup2 is file to tokne
            if (!lookup2.get(fakeName).contains(token)) {
                lookup2.put(fakeName, token);
            }
        }
        return new IndexPair(lookup1, lookup2, fileToRename);
    }*/

    public static IndexPair buildIndexFromBatch(Activity activity, List<BatchTaggingActivity.ImageTileWithTagGen> tiles) {
        Multimap<String, String> lp1 = ArrayListMultimap.create();
        Multimap<String, String> lp2 = ArrayListMultimap.create();
        Map<String, String> fileToRename = new HashMap<>();

        for (BatchTaggingActivity.ImageTileWithTagGen tile : tiles) {
            String fakeName = csRandomAlphaNumericString(FILE_NAME_LENGTH);
            fileToRename.put(tile.file, fakeName);
            for (String tag : tile.tags) {
                // if the multimap does not contain the keyword file association, add it
                if (!lp1.get(tag).contains(fakeName)) {
                    lp1.put(tag, fakeName);
                }

                if (!lp2.get(fakeName).contains(tag)) {
                    lp2.put(fakeName, tag);
                }
            }
        }
        return new IndexPair(lp1, lp2, fileToRename);
    }

    public static IndexPair buildIndexFromCustomTaken(Activity activity, String[] tags, String imageName) {
        Multimap<String, String> lookup1 = ArrayListMultimap.create();
        Multimap<String, String> lookup2 = ArrayListMultimap.create();
        for (String token : tags) {
            if (!lookup1.get(token).contains(imageName)) {
                lookup1.put(token, imageName);
            }

            // lookup2 is file to tokne
            if (!lookup2.get(imageName).contains(token)) {
                lookup2.put(imageName, token);
            }
        }
        IndexPair indexPair = new IndexPair(lookup1, lookup2, null);
        return indexPair;
    }

    /*private static IndexPair extractDocs(Activity activity, String[] listOfFile) throws IOException {
        Multimap<String, String> lookup1 = ArrayListMultimap.create();
        Multimap<String, String> lookup2 = ArrayListMultimap.create();
        Map<String, String> fileToRename = new HashMap<>();
        for (String file : listOfFile) {
            String fakeName = csRandomAlphaNumericString(FILE_NAME_LENGTH);
            fileToRename.put(file, fakeName);
            List<String> tokens = TagGen.generateTagFromGalleryImage(activity, Uri.fromFile(new File(file)));
            for (String token : tokens) {
                // lookup1 is tokens to file
                if (!lookup1.get(token).contains(fakeName)) {
                    lookup1.put(token, fakeName);
                }

                // lookup2 is file to tokne
                if (!lookup2.get(fakeName).contains(token)) {
                    lookup2.put(fakeName, token);
                }
            }
        }
        return new IndexPair(lookup1, lookup2, fileToRename);
    }*/

    public static String csRandomAlphaNumericString(int numChars) {
        SecureRandom srand = new SecureRandom();
        Random rand = new Random();
        char[] buff = new char[numChars];

        for (int i = 0; i < numChars; ++i) {
            // reseed rand once you've used up all available entropy bits
            if ((i % 10) == 0) {
                rand.setSeed(srand.nextLong()); // 64 bits of random!
            }
            buff[i] = VALID_CHARACTERS[rand.nextInt(VALID_CHARACTERS.length)];
        }
        return new String(buff);
    }

}
