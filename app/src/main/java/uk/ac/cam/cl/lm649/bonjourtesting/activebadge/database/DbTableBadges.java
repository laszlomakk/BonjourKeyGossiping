package uk.ac.cam.cl.lm649.bonjourtesting.activebadge.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.Badge;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

import static uk.ac.cam.cl.lm649.bonjourtesting.activebadge.database.DbContract.BadgeEntry;

public final class DbTableBadges {

    private static final String TAG = "DbTableBadges";
    
    private DbTableBadges() {}

    public static void addBadge(Badge badge) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        ContentValues values = createContentValuesFromBadge(badge);

        db.insert(BadgeEntry.TABLE_NAME, null, values);
    }

    private static ContentValues createContentValuesFromBadge(Badge badge) {
        ContentValues values = new ContentValues();
        values.put(BadgeEntry.COLUMN_NAME_BADGE_ID, badge.getBadgeId().toString());
        values.put(BadgeEntry.COLUMN_NAME_CUSTOM_NAME, badge.getCustomName());
        values.put(BadgeEntry.COLUMN_NAME_ROUTER_MAC, badge.getRouterMac());
        values.put(BadgeEntry.COLUMN_NAME_TIMESTAMP, badge.getTimestamp());
        return values;
    }

    public static Badge getBadge(UUID badgeId) {
        SQLiteDatabase db = DbHelper.getInstance().getReadableDatabase();

        Cursor cursor = db.query(BadgeEntry.TABLE_NAME,
                new String[] {
                        BadgeEntry.COLUMN_NAME_BADGE_ID,
                        BadgeEntry.COLUMN_NAME_CUSTOM_NAME,
                        BadgeEntry.COLUMN_NAME_ROUTER_MAC,
                        BadgeEntry.COLUMN_NAME_TIMESTAMP
                }, BadgeEntry.COLUMN_NAME_BADGE_ID + "=?",
                new String[] { badgeId.toString() }, null, null, null, null);
        if (null == cursor || cursor.getCount() == 0) {
            return null;
        }

        cursor.moveToFirst();
        Badge badge = createBadgeFromCursor(cursor);

        cursor.close();
        return badge;
    }

    private static Badge createBadgeFromCursor(Cursor cursor) {
        Badge badge = new Badge(UUID.fromString(cursor.getString(0)));
        badge.setCustomName(cursor.getString(1));
        badge.setRouterMac(cursor.getString(2));
        badge.setTimestamp(cursor.getLong(3));
        return badge;
    }

    public static List<Badge> getAllBadges(Badge.SortOrder sortOrder) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        String orderBy = "";
        switch (sortOrder) {
            case MOST_RECENT_FIRST:
                orderBy = " ORDER BY " + BadgeEntry.COLUMN_NAME_TIMESTAMP + " DESC";
                break;
            case ALPHABETICAL:
                orderBy = " ORDER BY " + BadgeEntry.COLUMN_NAME_CUSTOM_NAME + " ASC";
                break;
            default:
                FLogger.e(TAG, "unknown badge sort order: " + sortOrder.name());
                break;
        }

        String selectQuery = "SELECT  * FROM " + BadgeEntry.TABLE_NAME + orderBy;
        Cursor cursor = db.rawQuery(selectQuery, null);

        List<Badge> badgeList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                Badge badge = createBadgeFromCursor(cursor);
                badgeList.add(badge);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return badgeList;
    }

    public static int getBadgesCount() {
        SQLiteDatabase db = DbHelper.getInstance().getReadableDatabase();

        String countQuery = "SELECT  * FROM " + BadgeEntry.TABLE_NAME;
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();

        cursor.close();
        return count;
    }

    public static void updateBadge(Badge badge) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        ContentValues values = createContentValuesFromBadge(badge);
        db.update(BadgeEntry.TABLE_NAME,
                values, BadgeEntry.COLUMN_NAME_BADGE_ID + " = ?",
                new String[] { badge.getBadgeId().toString() });
    }

    public static void smartUpdateBadge(Badge newBadge) {
        Badge oldBadge = getBadge(newBadge.getBadgeId());
        if (null == oldBadge) {
            // badge not yet in db
            addBadge(newBadge);
        } else {
            if (newBadge.getTimestamp() > oldBadge.getTimestamp()) {
                updateBadge(newBadge);
            }
        }
    }

    public static void deleteBadge(Badge badge) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        db.delete(BadgeEntry.TABLE_NAME,
                BadgeEntry.COLUMN_NAME_BADGE_ID + " = ?",
                new String[] { badge.getBadgeId().toString() });
    }
    
}
