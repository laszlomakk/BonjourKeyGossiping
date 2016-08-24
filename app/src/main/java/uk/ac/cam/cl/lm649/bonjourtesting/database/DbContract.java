package uk.ac.cam.cl.lm649.bonjourtesting.database;

public final class DbContract {

    private DbContract() {}

    protected static final int DATABASE_VERSION = 10;
    protected static final String DATABASE_NAME = "key_gossip_db";

    public static class TablePhoneNumbers {
        public static final String TABLE_NAME = "phone_numbers";
        public static final String COLUMN_NAME_PHONE_NUMBER = "phoneNumber";
        public static final String COLUMN_NAME_CUSTOM_NAME = "customName";
        public static final String COLUMN_NAME_GOSSIPING_STATUS = "gossiping_status";
    }

    public static class TablePublicKeys {
        public static final String TABLE_NAME = "public_keys";
        public static final String COLUMN_NAME_PUBLIC_KEY = "publicKey";
        public static final String COLUMN_NAME_PHONE_NUMBER = "phoneNumber";
        public static final String COLUMN_NAME_TIMESTAMP_FIRST_SEEN_PUBLIC_KEY = "timestamp_first_seen_pubKey";
        public static final String COLUMN_NAME_TIMESTAMP_LAST_SEEN_ALIVE_PUBLIC_KEY = "timestamp_alive_pubKey";
        public static final String COLUMN_NAME_SIGNED_HASH = "owner_signed_hash";
    }

}
