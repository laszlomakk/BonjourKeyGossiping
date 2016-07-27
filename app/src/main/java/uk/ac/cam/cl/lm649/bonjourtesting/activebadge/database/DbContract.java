package uk.ac.cam.cl.lm649.bonjourtesting.activebadge.database;

public final class DbContract {

    private DbContract() {}

    protected static final int DATABASE_VERSION = 2;
    protected static final String DATABASE_NAME = "badgeDB";

    protected static class BadgeEntry {
        protected static final String TABLE_NAME = "badges";
        protected static final String COLUMN_NAME_BADGE_ID = "badgeId";
        protected static final String COLUMN_NAME_CUSTOM_NAME = "customName";
        protected static final String COLUMN_NAME_ROUTER_MAC = "routerMac";
        protected static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    }

    protected static class HistoryTransferEntry {
        protected static final String TABLE_NAME = "history_transfer";
        protected static final String COLUMN_NAME_BADGE_ID = "badgeId";
        protected static final String COLUMN_NAME_HISTORY_TRANSFER_TIMESTAMP = "ht_timestamp";
    }

}
