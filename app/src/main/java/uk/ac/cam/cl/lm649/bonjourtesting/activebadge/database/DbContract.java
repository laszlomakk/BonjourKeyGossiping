package uk.ac.cam.cl.lm649.bonjourtesting.activebadge.database;

public final class DbContract {

    private DbContract() {}

    protected static final int DATABASE_VERSION = 3;
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

}
