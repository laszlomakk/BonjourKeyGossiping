package uk.ac.cam.cl.lm649.bonjourtesting.activebadge.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Locale;
import java.util.UUID;

import static uk.ac.cam.cl.lm649.bonjourtesting.activebadge.database.DbContract.HistoryTransferEntry;

public final class DbTableHistoryTransfer {

    private static final String TAG = "DbTableHistoryTransfer";

    private DbTableHistoryTransfer() {}

    protected static String constructQueryToCreateTable() {
        return String.format(Locale.US,
                "CREATE TABLE %s(%s TEXT PRIMARY KEY, %s INTEGER)",
                HistoryTransferEntry.TABLE_NAME,
                HistoryTransferEntry.COLUMN_NAME_BADGE_ID,
                HistoryTransferEntry.COLUMN_NAME_TIMESTAMP_LAST_HISTORY_TRANSFER);
    }

    public static void addEntry(UUID badgeId, long timestamp) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        ContentValues values = createContentValues(badgeId, timestamp);

        db.insert(HistoryTransferEntry.TABLE_NAME, null, values);
    }

    private static ContentValues createContentValues(UUID badgeId, long timestamp) {
        ContentValues values = new ContentValues();
        values.put(HistoryTransferEntry.COLUMN_NAME_BADGE_ID, badgeId.toString());
        values.put(HistoryTransferEntry.COLUMN_NAME_TIMESTAMP_LAST_HISTORY_TRANSFER, timestamp);
        return values;
    }

    public static Long getTimestamp(UUID badgeId) {
        SQLiteDatabase db = DbHelper.getInstance().getReadableDatabase();

        Cursor cursor = db.query(HistoryTransferEntry.TABLE_NAME,
                new String[] {
                        HistoryTransferEntry.COLUMN_NAME_TIMESTAMP_LAST_HISTORY_TRANSFER
                }, HistoryTransferEntry.COLUMN_NAME_BADGE_ID + "=?",
                new String[] { badgeId.toString() }, null, null, null, null);
        if (null == cursor) return null;
        if (cursor.getCount() == 0) {
            cursor.close();
            return null;
        }

        cursor.moveToFirst();
        long ret = cursor.getLong(0);

        cursor.close();
        return ret;
    }

    public static void updateEntry(UUID badgeId, long timestamp) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        ContentValues values = createContentValues(badgeId, timestamp);
        db.update(HistoryTransferEntry.TABLE_NAME,
                values, HistoryTransferEntry.COLUMN_NAME_BADGE_ID + " = ?",
                new String[] { badgeId.toString() });
    }

    public static void smartUpdateEntry(UUID badgeId, long newTimestamp) {
        Long oldTimestamp = getTimestamp(badgeId);
        if (null == oldTimestamp) {
            // entry not yet in db
            addEntry(badgeId, newTimestamp);
        } else {
            if (newTimestamp > oldTimestamp) {
                updateEntry(badgeId, newTimestamp);
            }
        }
    }

    public static void deleteEntry(UUID badgeId) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        db.delete(HistoryTransferEntry.TABLE_NAME,
                HistoryTransferEntry.COLUMN_NAME_BADGE_ID + " = ?",
                new String[] { badgeId.toString() });
    }
    
}
