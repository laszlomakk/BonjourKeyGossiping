package uk.ac.cam.cl.lm649.bonjourtesting.database;

public final class DbContract {

    private DbContract() {}

    protected static final int DATABASE_VERSION = 6;
    protected static final String DATABASE_NAME = "badgeDB";

    protected static class BadgeEntry {
        protected static final String TABLE_NAME = "badges";
        protected static final String COLUMN_NAME_BADGE_ID = "badgeId";
        protected static final String COLUMN_NAME_CUSTOM_NAME = "customName";
        protected static final String COLUMN_NAME_ROUTER_MAC = "routerMac";
        protected static final String COLUMN_NAME_TIMESTAMP_LAST_SEEN_ALIVE = "timestamp_alive";
        protected static final String COLUMN_NAME_TIMESTAMP_LAST_UPDATED_IN_DB = "timestamp_db_update";
    }

    protected static class HistoryTransferEntry {
        protected static final String TABLE_NAME = "history_transfer";
        protected static final String COLUMN_NAME_BADGE_ID = "badgeId";
        protected static final String COLUMN_NAME_TIMESTAMP_LAST_HISTORY_TRANSFER = "timestamp_history_transfer";
    }

    protected static class PhoneNumberEntry {
        protected static final String TABLE_NAME = "phone_numbers";
        protected static final String COLUMN_NAME_BADGE_ID = "badgeId";
        protected static final String COLUMN_NAME_PHONE_NUMBER = "phoneNumber";
    }

    protected static class PublicKeyEntry {
        protected static final String TABLE_NAME = "public_keys";
        protected static final String COLUMN_NAME_PUBLIC_KEY = "publicKey";
        protected static final String COLUMN_NAME_PHONE_NUMBER = "phoneNumber";
        protected static final String COLUMN_NAME_TIMESTAMP_FIRST_SEEN_PUBLIC_KEY = "timestamp_first_seen_pubKey";
        protected static final String COLUMN_NAME_TIMESTAMP_LAST_SEEN_ALIVE_PUBLIC_KEY = "timestamp_alive_pubKey";
    }

}
