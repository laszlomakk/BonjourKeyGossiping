package uk.ac.cam.cl.lm649.bonjourtesting.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static uk.ac.cam.cl.lm649.bonjourtesting.database.DbContract.PhoneNumberEntry;

public final class DbTablePhoneNumbers {

    private static final String TAG = "DbTablePhoneNumbers";

    private DbTablePhoneNumbers() {}

    public static class Entry {
        private UUID badgeId;
        private String phoneNumber;

        public UUID getBadgeId() {
            return badgeId;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        @Override
        public String toString() {
            return String.format(Locale.US,
                    "badgeID: %s,\nphoneNum: %s",
                    badgeId.toString(), phoneNumber);
        }
    }

    protected static String constructQueryToCreateTable() {
        return String.format(Locale.US,
                "CREATE TABLE %s(%s TEXT PRIMARY KEY, %s TEXT)",
                PhoneNumberEntry.TABLE_NAME,
                PhoneNumberEntry.COLUMN_NAME_BADGE_ID,
                PhoneNumberEntry.COLUMN_NAME_PHONE_NUMBER);
    }

    public static void addEntry(UUID badgeId, String phoneNumber) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        ContentValues values = createContentValues(badgeId, phoneNumber);

        db.insert(PhoneNumberEntry.TABLE_NAME, null, values);
    }

    private static ContentValues createContentValues(UUID badgeId, String phoneNumber) {
        ContentValues values = new ContentValues();
        values.put(PhoneNumberEntry.COLUMN_NAME_BADGE_ID, badgeId.toString());
        values.put(PhoneNumberEntry.COLUMN_NAME_PHONE_NUMBER, phoneNumber);
        return values;
    }

    private static Entry createEntryFromCursor(Cursor cursor) {
        Entry entry = new Entry();
        entry.badgeId = UUID.fromString(cursor.getString(0));
        entry.phoneNumber = cursor.getString(1);
        return entry;
    }

    public static String getPhoneNumber(UUID badgeId) {
        SQLiteDatabase db = DbHelper.getInstance().getReadableDatabase();

        Cursor cursor = db.query(PhoneNumberEntry.TABLE_NAME,
                new String[] {
                        PhoneNumberEntry.COLUMN_NAME_PHONE_NUMBER
                }, PhoneNumberEntry.COLUMN_NAME_BADGE_ID + "=?",
                new String[] { badgeId.toString() }, null, null, null, null);
        if (null == cursor) return null;
        if (cursor.getCount() == 0) {
            cursor.close();
            return null;
        }

        cursor.moveToFirst();
        String ret = cursor.getString(0);

        cursor.close();
        return ret;
    }

    public static List<Entry> getAllEntries() {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        String selectQuery = "SELECT * FROM " + PhoneNumberEntry.TABLE_NAME;
        Cursor cursor = db.rawQuery(selectQuery, null);

        List<Entry> entries = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                Entry entry = createEntryFromCursor(cursor);
                entries.add(entry);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return entries;
    }

    public static void updateEntry(UUID badgeId, String phoneNumber) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        ContentValues values = createContentValues(badgeId, phoneNumber);
        db.update(PhoneNumberEntry.TABLE_NAME,
                values, PhoneNumberEntry.COLUMN_NAME_BADGE_ID + " = ?",
                new String[] { badgeId.toString() });
    }

    public static void smartUpdateEntry(UUID badgeId, String phoneNumber) {
        String oldPhoneNumber = getPhoneNumber(badgeId);
        if (null == oldPhoneNumber) {
            // entry not yet in db
            addEntry(badgeId, phoneNumber);
        } else {
            updateEntry(badgeId, phoneNumber);
        }
    }

    public static void deleteEntry(UUID badgeId) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        db.delete(PhoneNumberEntry.TABLE_NAME,
                PhoneNumberEntry.COLUMN_NAME_BADGE_ID + " = ?",
                new String[] { badgeId.toString() });
    }
    
}
