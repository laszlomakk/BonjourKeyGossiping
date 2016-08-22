package uk.ac.cam.cl.lm649.bonjourtesting.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

import static uk.ac.cam.cl.lm649.bonjourtesting.database.DbContract.PhoneNumberEntry;

public final class DbTablePhoneNumbers {

    private static final String TAG = "DbTablePhoneNumbers";

    private DbTablePhoneNumbers() {}

    public static class Entry {
        private String phoneNumber;
        private String customName;
        private Integer gossipingStatus = null;

        public static final int GOSSIPING_STATUS_USER_DISABLED = 0;
        public static final int GOSSIPING_STATUS_UNTOUCHED     = 1;
        public static final int GOSSIPING_STATUS_USER_ENABLED  = 2;

        public static final int META_GOSSIPING_STATUS_NUM_STATES = 3;

        public Entry() {}

        public Entry(String phoneNumber, String customName) {
            this.phoneNumber = phoneNumber;
            this.customName = customName;
        }

        public Entry(Entry entry) {
            this.phoneNumber = entry.phoneNumber;
            this.customName = entry.customName;
            this.gossipingStatus = entry.gossipingStatus;
        }

        public String getCustomName() {
            return customName;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public int getGossipingStatus() {
            return null == gossipingStatus ? GOSSIPING_STATUS_UNTOUCHED : gossipingStatus;
        }

        public String getGossipingStatusText() {
            switch (getGossipingStatus()) {
                case GOSSIPING_STATUS_USER_DISABLED:
                    return "disabled";
                case GOSSIPING_STATUS_UNTOUCHED:
                    return "default";
                case GOSSIPING_STATUS_USER_ENABLED:
                    return "enabled";
                default:
                    return "unknown";
            }
        }

        public void setGossipingStatus(int gossipingStatus) {
            switch (gossipingStatus) {
                case GOSSIPING_STATUS_USER_DISABLED:
                case GOSSIPING_STATUS_UNTOUCHED:
                case GOSSIPING_STATUS_USER_ENABLED:
                    break;
                default:
                    FLogger.e(TAG, "setGossipingStatus(). invalid gossipingStatus: " + gossipingStatus);
                    return;
            }
            this.gossipingStatus = gossipingStatus;
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
                "CREATE TABLE %s(%s TEXT PRIMARY KEY, %s TEXT, %s INTEGER)",
                PhoneNumberEntry.TABLE_NAME,
                PhoneNumberEntry.COLUMN_NAME_PHONE_NUMBER,
                PhoneNumberEntry.COLUMN_NAME_CUSTOM_NAME,
                PhoneNumberEntry.COLUMN_NAME_GOSSIPING_STATUS);
    }

    public static void addEntry(Entry entry) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        if (null == entry.gossipingStatus) {
            entry.gossipingStatus = Entry.GOSSIPING_STATUS_UNTOUCHED;
        }
        ContentValues values = createContentValues(entry);

        db.insert(PhoneNumberEntry.TABLE_NAME, null, values);
    }

    private static ContentValues createContentValues(Entry entry) {
        ContentValues values = new ContentValues();
        values.put(PhoneNumberEntry.COLUMN_NAME_PHONE_NUMBER, entry.phoneNumber);
        values.put(PhoneNumberEntry.COLUMN_NAME_CUSTOM_NAME, entry.customName);
        values.put(PhoneNumberEntry.COLUMN_NAME_GOSSIPING_STATUS, entry.gossipingStatus);
        return values;
    }

    @Nullable
    public static Entry getEntry(String phoneNumber) {
        SQLiteDatabase db = DbHelper.getInstance().getReadableDatabase();

        Cursor cursor = null;
        try {
            cursor = db.query(PhoneNumberEntry.TABLE_NAME,
                    new String[] {
                            PhoneNumberEntry.COLUMN_NAME_PHONE_NUMBER,
                            PhoneNumberEntry.COLUMN_NAME_CUSTOM_NAME,
                            PhoneNumberEntry.COLUMN_NAME_GOSSIPING_STATUS,
                    }, PhoneNumberEntry.COLUMN_NAME_PHONE_NUMBER + "=?",
                    new String[] { phoneNumber }, null, null, null, null);

            if (cursor.moveToFirst()) {
                Entry entry = new Entry();
                entry.phoneNumber = cursor.getString(0);
                entry.customName = cursor.getString(1);
                entry.gossipingStatus = cursor.getInt(2);
                return entry;
            } else {
                return null;
            }
        } finally {
            if (null != cursor) cursor.close();
        }
    }

    @NonNull
    public static List<Entry> getAllEntries() {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        String selectQuery = String.format(Locale.US,
                "SELECT %s, %s, %s FROM %s ORDER BY %s ",
                PhoneNumberEntry.COLUMN_NAME_PHONE_NUMBER,
                PhoneNumberEntry.COLUMN_NAME_CUSTOM_NAME,
                PhoneNumberEntry.COLUMN_NAME_GOSSIPING_STATUS,
                PhoneNumberEntry.TABLE_NAME,
                PhoneNumberEntry.COLUMN_NAME_CUSTOM_NAME);

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(selectQuery, null);

            List<Entry> entries = new ArrayList<>();
            if (cursor.moveToFirst()) {
                do {
                    Entry entry = new Entry();
                    entry.phoneNumber = cursor.getString(0);
                    entry.customName = cursor.getString(1);
                    entry.gossipingStatus = cursor.getInt(2);
                    entries.add(entry);
                } while (cursor.moveToNext());
            }
            return reorderEntriesToHaveOnesWithUntouchedGossipingFirst(entries);
        } finally {
            if (null != cursor) cursor.close();
        }
    }

    private static List<Entry> reorderEntriesToHaveOnesWithUntouchedGossipingFirst(@NonNull List<Entry> entries) {
        List<Entry> reorderedEntries = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            if (entry.getGossipingStatus() == Entry.GOSSIPING_STATUS_UNTOUCHED) {
                reorderedEntries.add(entry);
            }
        }
        for (Entry entry : entries) {
            if (entry.getGossipingStatus() != Entry.GOSSIPING_STATUS_UNTOUCHED) {
                reorderedEntries.add(entry);
            }
        }
        return reorderedEntries;
    }

    public static void updateEntry(Entry entry) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        ContentValues values = createContentValues(entry);
        db.update(PhoneNumberEntry.TABLE_NAME,
                values, PhoneNumberEntry.COLUMN_NAME_PHONE_NUMBER + " = ?",
                new String[] { entry.phoneNumber });
    }

    public static void smartUpdateEntry(Entry newEntry) {
        Entry oldEntry = getEntry(newEntry.phoneNumber);
        if (null == oldEntry) {
            // entry not yet in db
            addEntry(newEntry);
        } else {
            Entry mergedEntry = new Entry(newEntry);
            if (null == mergedEntry.gossipingStatus) {
                mergedEntry.gossipingStatus = oldEntry.gossipingStatus;
            }
            updateEntry(mergedEntry);
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
