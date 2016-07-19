package uk.ac.cam.cl.lm649.bonjourtesting.activebadge;

public class DbContract {

    private DbContract() {}

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "badgeDB";

    public static class BadgeEntry {
        public static final String TABLE_NAME = "badges";
        protected static final String COLUMN_NAME_BADGE_ID = "badgeId";
        protected static final String COLUMN_NAME_CUSTOM_NAME = "customName";
        protected static final String COLUMN_NAME_ROUTER_MAC = "routerMac";
        protected static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    }

}
