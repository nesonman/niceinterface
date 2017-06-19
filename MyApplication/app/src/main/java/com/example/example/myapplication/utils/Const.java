package com.example.example.myapplication.utils;

/**
 * This class contains constants used in the application.
 */
public class Const {

    /**
     * Label for the password in the POST requests
     */
    public static final String PASSWORD_LABEL = "password";

    public static final String FIRST_NAME_LABEL = "firstName";

    public static final String LAST_NAME_LABEL = "lastName";

    public static final String EMAIL_LABEL = "email";

    public static final String USER_ID_SESSION_LABEL = "userID";

    public static final String PICTURE_LABEL = "picture";

    public static final String MEDIUM_LABEL = "medium";

    public static final String UPDATE_TOKEN_LABEL = "tokenUp";

    public static final String DICT_UPDATE_FILE = "dictUpdate";

    public static final String STATE_FILE = "state";

    public static final String STATE_LABEL = "state";

    public static final String DICT_FILE = "dict";

    public static final String QUERY_TOKEN_LABEL = "token";

    public static final String CMAC_TOKEN_LABEL = "cmac";

    public static final String PICTURE_NAME_LABEL = "pictureName";

    public static final String SHARED_PREFERENCE_NAME = "pixek.io.shared_preferences";

    public static final String SECURITY_QUESTION_LABEL = "securityQuestion";

    public static final String SECURITY_ANSWER_LABEL = "securityAnswer";

    public static final String SHARED_PREF_STATE = "state";

    public static final String SHARED_PREF_SALT = "salt";

    public static final String SHARED_PREF_SK = "sk";

    public static final String SHARED_PREF_EMAIL = "email";

    public static final String THUMBNAIL_LABEL = "thumbnail";

    public static final String NUM_REQUEST_LABEL = "numRequest";

    public static final String LOCAL_IMAGE_NAMES_LABEL = "localImages";

    public static final String THUMBNAIL_DIR = "/thumbnails/";

    public static final String FULL_IMAGE_DIR = "/images/";

    public static final String VIDEO_DIR = "/videos/";

    // for images of "medium" size to use as a placeholder while fetching large images
    public static final String MEDIUM_IMAGE_DIR = "/medium/";

    public static final String TEMP_ID_LABEL = "tempID";

    public static final String CODE_LABEL = "code";

    public static final String IMAGE_FILE_BUNDLE_LABEL = "imageFiles";

    public static final String TAG_LABEL = "tags";

    public static final String QUERY_RESULT_LABEL = "queryResult";

    public static final String TAG_INDEX = "tagIndex";

    public static final String CACHED_IMAGE_LABEL = "cachedImages";

    public static final String ENC_STATE_SHARED_PREF_LABEL = "encState";

    public static final String KEYSTORE_ALIAS = "pixek";

    public static final String KEYSTORE_IV = "keystoreIV";

    public static final String LOCAL_PASSWORD_SALT_LABEL = "localPasswordSalt";

    public static final String ENCRYPTED_PASSWORD_LABEL = "encPassword";

    public static final String ENCRYPTED_PASSWORD_SALT_LABEL = "encPasswordSalt";

    public static final int TAG_TOTAL_MAX_CHARS = 160;

    public static final int GLOBAL_TAG_MAX_CHARS = 80;

    public static final int BATCH_TAG_MAX_CHARS = 80;

    // send at most 3 files at a time to the server
    public static final int UPDATE_FILE_NUM = 3;

    public static final int THUMBNAIL_SCALE_SIZE = 100;

    public static final int MEDIUM_SCALE_SIZE = 300;

    public static final int SCRYPT_CPU = 10;

    public static final int SCRYPT_MEM = 10;

    public static final int SCRYPT_PAR = 10;

    public static final int SCRYPT_LENGTH = 32;

    /**
     * Class that stores the constants for the local usecase
     */
    public class Local {

        public static final int SALT_SIZE = 8;

        public static final String LOCAL_DIRECTORY = "local";

        public static final String SSE_DIR = "/.sse/";

        public static final String ENC_IMAGE_DIR = "/encImages/";

        public static final String ENC_THUMBNAIL_DIR = "/encThumbs/";

        public static final String ENC_MEDIUM_DIR = "/encMediums/";

        public static final String DICT_UPDATE_FILE = "dictUpdate";

        public static final String DICT_FILE = "dict";

        public static final String PASSWORD_HASH_LABEL = "passwordHash";

        public static final String PASSWORD_SALT_LABEL = "passwordSalt";

        public static final String LAST_IMAGE_QUERIED_ID = "lastImageQueriedID";

        public static final String PASSWORD_RECOVERY_MODE_LABEL = "passwordRecoveryMode";

        public static final String PASSWORD_RECOVERY_LOCAL = "passwordRecoveryLocal";

        public static final String PASSWORD_RECOVERY_CLOUD = "passwordRecoveryCloud";

        public static final String PASSWORD_RECOVERY_SALT_LABEL = "passwordRecoverySalt";

        public static final String PASSWORD_ENC_LABEL = "passwordRecoveryEnc";

        public static final String SECURITY_QUESTION_1_LABEL = "securityQuestion1";

        public static final String SECURITY_QUESTION_2_LABEL = "securityQuestion2";

    }

}