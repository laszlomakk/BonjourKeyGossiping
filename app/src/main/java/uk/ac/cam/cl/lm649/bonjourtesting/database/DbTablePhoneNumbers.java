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
        private String phoneNumber;
        private String customName;

        public String getCustomName() {
            return customName;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        @Override
        public String toString() {
            return String.format(Locale.US,
                    "nick: %s\nphoneNum: %s",
                    customName, phoneNumber);
        }
    }

    protected static String constructQueryToCreateTable() {
        return String.format(Locale.US,
                "CREATE TABLE %s(%s TEXT PRIMARY KEY, %s TEXT)",
                PhoneNumberEntry.TABLE_NAME,
                PhoneNumberEntry.COLUMN_NAME_PHONE_NUMBER,
                PhoneNumberEntry.COLUMN_NAME_CUSTOM_NAME);
    }

    public static void addEntry(String phoneNumber, String customName) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        ContentValues values = createContentValues(phoneNumber, customName);

        db.insert(PhoneNumberEntry.TABLE_NAME, null, values);
    }

    private static ContentValues createContentValues(String phoneNumber, String customName) {
        ContentValues values = new ContentValues();
        values.put(PhoneNumberEntry.COLUMN_NAME_PHONE_NUMBER, phoneNumber);
        values.put(PhoneNumberEntry.COLUMN_NAME_CUSTOM_NAME, customName);
        return values;
    }

    public static String getCustomName(String phoneNumber) {
        SQLiteDatabase db = DbHelper.getInstance().getReadableDatabase();

        Cursor cursor = db.query(PhoneNumberEntry.TABLE_NAME,
                new String[] {
                        PhoneNumberEntry.COLUMN_NAME_CUSTOM_NAME
                }, PhoneNumberEntry.COLUMN_NAME_PHONE_NUMBER + "=?",
                new String[] { phoneNumber }, null, null, null, null);
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

        String selectQuery = String.format(Locale.US,
                "SELECT %s, %s FROM %s ORDER BY %s ",
                PhoneNumberEntry.COLUMN_NAME_PHONE_NUMBER,
                PhoneNumberEntry.COLUMN_NAME_CUSTOM_NAME,
                PhoneNumberEntry.TABLE_NAME,
                PhoneNumberEntry.COLUMN_NAME_CUSTOM_NAME);
        Cursor cursor = db.rawQuery(selectQuery, null);

        List<Entry> entries = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                Entry entry = new Entry();
                entry.phoneNumber = cursor.getString(0);
                entry.customName = cursor.getString(1);
                entries.add(entry);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return entries;
    }

    public static void updateEntry(String phoneNumber, String customName) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        ContentValues values = createContentValues(phoneNumber, customName);
        db.update(PhoneNumberEntry.TABLE_NAME,
                values, PhoneNumberEntry.COLUMN_NAME_PHONE_NUMBER + " = ?",
                new String[] { phoneNumber });
    }

    public static void smartUpdateEntry(String phoneNumber, String customName) {
        String oldCustomName = getCustomName(phoneNumber);
        if (null == oldCustomName) {
            // entry not yet in db
            addEntry(phoneNumber, customName);
        } else {
            updateEntry(phoneNumber, customName);
        }
    }

    public static void deleteEntry(String phoneNumber) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        db.delete(PhoneNumberEntry.TABLE_NAME,
                PhoneNumberEntry.COLUMN_NAME_PHONE_NUMBER + " = ?",
                new String[] { phoneNumber });
    }

    public static void deleteAllEntries() {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();
        db.delete(PhoneNumberEntry.TABLE_NAME, null, null);
    }
    
}
