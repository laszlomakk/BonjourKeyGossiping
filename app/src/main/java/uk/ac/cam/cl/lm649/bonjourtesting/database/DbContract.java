package uk.ac.cam.cl.lm649.bonjourtesting.database;

public final class DbContract {

    private DbContract() {}

    protected static final int DATABASE_VERSION = 9;
    protected static final String DATABASE_NAME = "key_gossip_db";

    protected static class PhoneNumberEntry {
        protected static final String TABLE_NAME = "phone_numbers";
        protected static final String COLUMN_NAME_PHONE_NUMBER = "phoneNumber";
        protected static final String COLUMN_NAME_CUSTOM_NAME = "customName";
        protected static final String COLUMN_NAME_GOSSIPING_STATUS = "gossiping_status";
    }

    protected static class PublicKeyEntry {
        protected static final String TABLE_NAME = "public_keys";
        protected static final String COLUMN_NAME_PUBLIC_KEY = "publicKey";
        protected static final String COLUMN_NAME_PHONE_NUMBER = "phoneNumber";
        protected static final String COLUMN_NAME_TIMESTAMP_FIRST_SEEN_PUBLIC_KEY = "timestamp_first_seen_pubKey";
        protected static final String COLUMN_NAME_TIMESTAMP_LAST_SEEN_ALIVE_PUBLIC_KEY = "timestamp_alive_pubKey";
    }

}
