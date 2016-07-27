package uk.ac.cam.cl.lm649.bonjourtesting.activebadge.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.BadgeCore;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.BadgeStatus;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

import static uk.ac.cam.cl.lm649.bonjourtesting.activebadge.database.DbContract.BadgeEntry;

public final class DbTableBadges {

    private static final String TAG = "DbTableBadges";
    
    private DbTableBadges() {}

    public static class Entry {
        BadgeStatus badgeStatus;
        long timestampLastUpdated;
    }

    protected static String constructQueryToCreateTable() {
        return String.format(Locale.US,
                "CREATE TABLE %s(%s TEXT PRIMARY KEY, %s TEXT, %s TEXT, %s INTEGER, %s INTEGER)",
                BadgeEntry.TABLE_NAME,
                BadgeEntry.COLUMN_NAME_BADGE_ID,
                BadgeEntry.COLUMN_NAME_CUSTOM_NAME,
                BadgeEntry.COLUMN_NAME_ROUTER_MAC,
                BadgeEntry.COLUMN_NAME_TIMESTAMP_LAST_SEEN_ALIVE,
                BadgeEntry.COLUMN_NAME_TIMESTAMP_LAST_UPDATED_IN_DB);
    }

    public static void addBadgeStatus(BadgeStatus badgeStatus) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        ContentValues values = createContentValuesFromBadge(badgeStatus);

        db.insert(BadgeEntry.TABLE_NAME, null, values);
    }

    private static ContentValues createContentValuesFromBadge(BadgeStatus badgeStatus) {
        BadgeCore badgeCore = badgeStatus.getBadgeCore();
        ContentValues values = new ContentValues();
        values.put(BadgeEntry.COLUMN_NAME_BADGE_ID, badgeCore.getBadgeId().toString());
        values.put(BadgeEntry.COLUMN_NAME_CUSTOM_NAME, badgeCore.getCustomName());
        values.put(BadgeEntry.COLUMN_NAME_ROUTER_MAC, badgeStatus.getRouterMac());
        values.put(BadgeEntry.COLUMN_NAME_TIMESTAMP_LAST_SEEN_ALIVE, badgeStatus.getTimestampLastSeenAlive());
        values.put(BadgeEntry.COLUMN_NAME_TIMESTAMP_LAST_UPDATED_IN_DB, System.currentTimeMillis());
        return values;
    }

    public static Entry getEntry(UUID badgeId) {
        SQLiteDatabase db = DbHelper.getInstance().getReadableDatabase();

        Cursor cursor = db.query(BadgeEntry.TABLE_NAME,
                new String[] {
                        BadgeEntry.COLUMN_NAME_BADGE_ID,
                        BadgeEntry.COLUMN_NAME_CUSTOM_NAME,
                        BadgeEntry.COLUMN_NAME_ROUTER_MAC,
                        BadgeEntry.COLUMN_NAME_TIMESTAMP_LAST_SEEN_ALIVE,
                        BadgeEntry.COLUMN_NAME_TIMESTAMP_LAST_UPDATED_IN_DB
                }, BadgeEntry.COLUMN_NAME_BADGE_ID + "=?",
                new String[] { badgeId.toString() }, null, null, null, null);
        if (null == cursor || cursor.getCount() == 0) {
            return null;
        }

        cursor.moveToFirst();
        Entry entry = createEntryFromCursor(cursor);

        cursor.close();
        return entry;
    }

    private static Entry createEntryFromCursor(Cursor cursor) {
        BadgeCore badgeCore = new BadgeCore(UUID.fromString(cursor.getString(0)));
        badgeCore.setCustomName(cursor.getString(1));

        BadgeStatus badgeStatus = new BadgeStatus(badgeCore);
        badgeStatus.setRouterMac(cursor.getString(2));
        badgeStatus.setTimestampLastSeenAlive(cursor.getLong(3));

        Entry entry = new Entry();
        entry.badgeStatus = badgeStatus;
        entry.timestampLastUpdated = cursor.getLong(4);
        return entry;
    }

    public static List<BadgeStatus> getAllBadges(BadgeStatus.SortOrder sortOrder) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        String orderBy = "";
        switch (sortOrder) {
            case MOST_RECENT_ALIVE_FIRST:
                orderBy = " ORDER BY " + BadgeEntry.COLUMN_NAME_TIMESTAMP_LAST_SEEN_ALIVE + " DESC";
                break;
            case ALPHABETICAL:
                orderBy = " ORDER BY " + BadgeEntry.COLUMN_NAME_CUSTOM_NAME + " ASC";
                break;
            default:
                FLogger.e(TAG, "unknown badge sort order: " + sortOrder.name());
                break;
        }

        String selectQuery = "SELECT * FROM " + BadgeEntry.TABLE_NAME + orderBy;
        Cursor cursor = db.rawQuery(selectQuery, null);

        List<BadgeStatus> badgeList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                BadgeStatus badge = createEntryFromCursor(cursor).badgeStatus;
                badgeList.add(badge);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return badgeList;
    }

    public static List<BadgeStatus> getBadgesUpdatedSince(Long updateTime) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        String whereClause = "";
        if (null != updateTime) {
            whereClause = " WHERE " + BadgeEntry.COLUMN_NAME_TIMESTAMP_LAST_UPDATED_IN_DB
                    + " > " + updateTime;
        }

        String selectQuery = "SELECT * FROM " + BadgeEntry.TABLE_NAME
                + whereClause;
        Cursor cursor = db.rawQuery(selectQuery, null);

        List<BadgeStatus> badgeList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                BadgeStatus badge = createEntryFromCursor(cursor).badgeStatus;
                badgeList.add(badge);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return badgeList;
    }

    public static int getEntriesCount() {
        SQLiteDatabase db = DbHelper.getInstance().getReadableDatabase();

        String countQuery = "SELECT * FROM " + BadgeEntry.TABLE_NAME;
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();

        cursor.close();
        return count;
    }

    public static void updateBadgeStatus(BadgeStatus badgeStatus) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        ContentValues values = createContentValuesFromBadge(badgeStatus);
        db.update(BadgeEntry.TABLE_NAME,
                values, BadgeEntry.COLUMN_NAME_BADGE_ID + " = ?",
                new String[] { badgeStatus.getBadgeCore().getBadgeId().toString() });
    }

    public static void smartUpdateBadge(BadgeStatus newBadgeStatus) {
        Entry oldEntry = getEntry(newBadgeStatus.getBadgeCore().getBadgeId());
        if (null == oldEntry) {
            // badge not yet in db
            addBadgeStatus(newBadgeStatus);
        } else {
            if (newBadgeStatus.getTimestampLastSeenAlive() > oldEntry.badgeStatus.getTimestampLastSeenAlive()) {
                updateBadgeStatus(newBadgeStatus);
            }
        }
    }

    public static void deleteBadge(UUID badgeId) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        db.delete(BadgeEntry.TABLE_NAME,
                BadgeEntry.COLUMN_NAME_BADGE_ID + " = ?",
                new String[] { badgeId.toString() });
    }
    
}
