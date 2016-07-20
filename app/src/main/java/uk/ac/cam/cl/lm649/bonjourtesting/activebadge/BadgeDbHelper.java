package uk.ac.cam.cl.lm649.bonjourtesting.activebadge;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static uk.ac.cam.cl.lm649.bonjourtesting.activebadge.DbContract.BadgeEntry;

public class BadgeDbHelper extends SQLiteOpenHelper {

    private static BadgeDbHelper INSTANCE = null;

    private BadgeDbHelper(Context context) {
        super(context, DbContract.DATABASE_NAME, null, DbContract.DATABASE_VERSION);
    }

    public static synchronized BadgeDbHelper getInstance(Context context) {
        if (null == INSTANCE) {
            INSTANCE = new BadgeDbHelper(context);
        }
        return INSTANCE;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_BADGES_TABLE = String.format(Locale.US,
                "CREATE TABLE %s(%s TEXT PRIMARY KEY, %s TEXT, %s TEXT, %s INTEGER)",
                BadgeEntry.TABLE_NAME,
                BadgeEntry.COLUMN_NAME_BADGE_ID,
                BadgeEntry.COLUMN_NAME_CUSTOM_NAME,
                BadgeEntry.COLUMN_NAME_ROUTER_MAC,
                BadgeEntry.COLUMN_NAME_TIMESTAMP);
        db.execSQL(CREATE_BADGES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + BadgeEntry.TABLE_NAME);
        onCreate(db);
    }

    public void addBadge(Badge badge) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = createContentValuesFromBadge(badge);

        db.insert(BadgeEntry.TABLE_NAME, null, values);
    }

    private ContentValues createContentValuesFromBadge(Badge badge) {
        ContentValues values = new ContentValues();
        values.put(BadgeEntry.COLUMN_NAME_BADGE_ID, badge.getBadgeId().toString());
        values.put(BadgeEntry.COLUMN_NAME_CUSTOM_NAME, badge.getCustomName());
        values.put(BadgeEntry.COLUMN_NAME_ROUTER_MAC, badge.getRouterMac());
        values.put(BadgeEntry.COLUMN_NAME_TIMESTAMP, badge.getTimestamp());
        return values;
    }

    public Badge getBadge(UUID badgeId) {
        SQLiteDatabase db = this.getReadableDatabase();

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

    private Badge createBadgeFromCursor(Cursor cursor) {
        Badge badge = new Badge(UUID.fromString(cursor.getString(0)));
        badge.setCustomName(cursor.getString(1));
        badge.setRouterMac(cursor.getString(2));
        badge.setTimestamp(cursor.getLong(3));
        return badge;
    }

    public List<Badge> getAllBadges() {
        SQLiteDatabase db = this.getWritableDatabase();

        String selectQuery = "SELECT  * FROM " + BadgeEntry.TABLE_NAME
                + " ORDER BY " + BadgeEntry.COLUMN_NAME_TIMESTAMP + " DESC";
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

    public int getBadgesCount() {
        SQLiteDatabase db = this.getReadableDatabase();

        String countQuery = "SELECT  * FROM " + BadgeEntry.TABLE_NAME;
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();

        cursor.close();
        return count;
    }

    public void updateBadge(Badge badge) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = createContentValuesFromBadge(badge);
        db.update(BadgeEntry.TABLE_NAME,
                values, BadgeEntry.COLUMN_NAME_BADGE_ID + " = ?",
                new String[] { badge.getBadgeId().toString() });
    }

    public void smartUpdateBadge(Badge newBadge) {
        Badge oldBadge = getBadge(newBadge.getBadgeId());
        if (null == oldBadge) {
            // badge not yet in db
            addBadge(newBadge);
        } else {
            // badge already in db, merge
            Badge mergedBadge = new Badge(newBadge.getBadgeId());
            if (null != newBadge.getCustomName() && !"".equals(newBadge.getCustomName())) {
                mergedBadge.setCustomName(newBadge.getCustomName());
            } else {
                mergedBadge.setCustomName(oldBadge.getCustomName());
            }
            mergedBadge.setRouterMac(newBadge.getRouterMac());
            mergedBadge.setTimestamp(newBadge.getTimestamp());

            updateBadge(mergedBadge);
        }
    }

    public void deleteBadge(Badge badge) {
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(BadgeEntry.TABLE_NAME,
                BadgeEntry.COLUMN_NAME_BADGE_ID + " = ?",
                new String[] { badge.getBadgeId().toString() });
    }

}
