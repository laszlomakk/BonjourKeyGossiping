package uk.ac.cam.cl.lm649.bonjourtesting.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import uk.ac.cam.cl.lm649.bonjourtesting.crypto.Asymmetric;

import static uk.ac.cam.cl.lm649.bonjourtesting.database.DbContract.BadgeEntry;
import static uk.ac.cam.cl.lm649.bonjourtesting.database.DbContract.PhoneNumberEntry;
import static uk.ac.cam.cl.lm649.bonjourtesting.database.DbContract.PublicKeyEntry;

public final class DbTablePublicKeys {

    private static final String TAG = "DbTablePublicKeys";

    private DbTablePublicKeys() {}

    public static class Entry {
        protected String publicKey;
        protected String phoneNumber;
        protected Long timestampFirstSeenPublicKey = null;
        protected Long timestampLastSeenAlivePublicKey = null;

        public Entry() {}

        protected Entry(Entry entry) {
            this.publicKey = entry.publicKey;
            this.phoneNumber = entry.phoneNumber;
            this.timestampFirstSeenPublicKey = entry.timestampFirstSeenPublicKey;
            this.timestampLastSeenAlivePublicKey = entry.timestampLastSeenAlivePublicKey;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public Long getTimestampFirstSeenPublicKey() {
            return timestampFirstSeenPublicKey;
        }

        public void setTimestampFirstSeenPublicKey(Long timestampFirstSeenPublicKey) {
            this.timestampFirstSeenPublicKey = timestampFirstSeenPublicKey;
        }

        public Long getTimestampLastSeenAlivePublicKey() {
            return timestampLastSeenAlivePublicKey;
        }

        public void setTimestampLastSeenAlivePublicKey(Long timestampLastSeenAlivePublicKey) {
            this.timestampLastSeenAlivePublicKey = timestampLastSeenAlivePublicKey;
        }

        @Override
        public String toString() {
            return String.format(Locale.US,
                    "phoneNum: %s\npubKey: %s\nFS_time: %s\nLSA_time: %s",
                    phoneNumber,
                    Asymmetric.getFingerprint(publicKey),
                    new Date(timestampFirstSeenPublicKey),
                    new Date(timestampLastSeenAlivePublicKey));
        }
    }

    public static class EntryWithBadgeData extends Entry {
        protected UUID badgeId = null;
        protected String badgeCustomName = null;

        public UUID getBadgeId() {
            return badgeId;
        }

        public String getBadgeCustomName() {
            return badgeCustomName;
        }

        @Override
        public String toString() {
            return String.format(Locale.US,
                    "badgeID: %s\nnick: %s\n%s",
                    badgeId,
                    badgeCustomName,
                    super.toString());
        }
    }

    protected static String constructQueryToCreateTable() {
        return String.format(Locale.US,
                "CREATE TABLE %s(%s TEXT PRIMARY KEY, %s TEXT, %s INTEGER, %s INTEGER)",
                PublicKeyEntry.TABLE_NAME,
                PublicKeyEntry.COLUMN_NAME_PUBLIC_KEY,
                PublicKeyEntry.COLUMN_NAME_PHONE_NUMBER,
                PublicKeyEntry.COLUMN_NAME_TIMESTAMP_FIRST_SEEN_PUBLIC_KEY,
                PublicKeyEntry.COLUMN_NAME_TIMESTAMP_LAST_SEEN_ALIVE_PUBLIC_KEY);
    }

    public static void addEntry(Entry entry) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        ContentValues values = createContentValues(entry);

        db.insert(PublicKeyEntry.TABLE_NAME, null, values);
    }

    private static ContentValues createContentValues(Entry entry) {
        ContentValues values = new ContentValues();
        values.put(PublicKeyEntry.COLUMN_NAME_PUBLIC_KEY, entry.publicKey);
        values.put(PublicKeyEntry.COLUMN_NAME_PHONE_NUMBER, entry.phoneNumber);
        values.put(PublicKeyEntry.COLUMN_NAME_TIMESTAMP_FIRST_SEEN_PUBLIC_KEY, entry.timestampFirstSeenPublicKey);
        values.put(PublicKeyEntry.COLUMN_NAME_TIMESTAMP_LAST_SEEN_ALIVE_PUBLIC_KEY, entry.timestampLastSeenAlivePublicKey);
        return values;
    }

    public static Entry getEntry(String publicKey) {
        SQLiteDatabase db = DbHelper.getInstance().getReadableDatabase();

        Cursor cursor = db.query(PublicKeyEntry.TABLE_NAME,
                new String[] {
                        PublicKeyEntry.COLUMN_NAME_PUBLIC_KEY,
                        PublicKeyEntry.COLUMN_NAME_PHONE_NUMBER,
                        PublicKeyEntry.COLUMN_NAME_TIMESTAMP_FIRST_SEEN_PUBLIC_KEY,
                        PublicKeyEntry.COLUMN_NAME_TIMESTAMP_LAST_SEEN_ALIVE_PUBLIC_KEY,
                }, PublicKeyEntry.COLUMN_NAME_PUBLIC_KEY + "=?",
                new String[] { publicKey }, null, null, null, null);
        if (null == cursor) return null;
        if (cursor.getCount() == 0) {
            cursor.close();
            return null;
        }

        cursor.moveToFirst();
        Entry entry = new Entry();
        entry.publicKey = cursor.getString(0);
        entry.phoneNumber = cursor.getString(1);
        entry.timestampFirstSeenPublicKey = cursor.getLong(2);
        entry.timestampLastSeenAlivePublicKey = cursor.getLong(3);

        cursor.close();
        return entry;
    }

    public static List<EntryWithBadgeData> getAllEntries() {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        String selectQuery = String.format(Locale.US,
                "SELECT %s, %s, %s, %s, %s, %s FROM %s LEFT JOIN %s ON %s=%s LEFT JOIN %s ON %s=%s ORDER BY %s DESC",
                PhoneNumberEntry.TABLE_NAME + "." + PhoneNumberEntry.COLUMN_NAME_BADGE_ID,
                BadgeEntry.TABLE_NAME       + "." + BadgeEntry.COLUMN_NAME_CUSTOM_NAME,
                PublicKeyEntry.TABLE_NAME   + "." + PublicKeyEntry.COLUMN_NAME_PHONE_NUMBER,
                PublicKeyEntry.TABLE_NAME   + "." + PublicKeyEntry.COLUMN_NAME_PUBLIC_KEY,
                PublicKeyEntry.TABLE_NAME   + "." + PublicKeyEntry.COLUMN_NAME_TIMESTAMP_FIRST_SEEN_PUBLIC_KEY,
                PublicKeyEntry.TABLE_NAME   + "." + PublicKeyEntry.COLUMN_NAME_TIMESTAMP_LAST_SEEN_ALIVE_PUBLIC_KEY,
                PublicKeyEntry.TABLE_NAME,
                PhoneNumberEntry.TABLE_NAME,
                PublicKeyEntry.TABLE_NAME   + "." + PublicKeyEntry.COLUMN_NAME_PHONE_NUMBER,
                PhoneNumberEntry.TABLE_NAME + "." + PhoneNumberEntry.COLUMN_NAME_PHONE_NUMBER,
                BadgeEntry.TABLE_NAME,
                PhoneNumberEntry.TABLE_NAME + "." + PhoneNumberEntry.COLUMN_NAME_BADGE_ID,
                BadgeEntry.TABLE_NAME       + "." + BadgeEntry.COLUMN_NAME_BADGE_ID,
                PublicKeyEntry.TABLE_NAME   + "." + PublicKeyEntry.COLUMN_NAME_TIMESTAMP_LAST_SEEN_ALIVE_PUBLIC_KEY);
        Cursor cursor = db.rawQuery(selectQuery, null);

        List<EntryWithBadgeData> entries = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                EntryWithBadgeData entry = new EntryWithBadgeData();
                entry.badgeId = null == cursor.getString(0) ? null : UUID.fromString(cursor.getString(0));
                entry.badgeCustomName = cursor.getString(1);
                entry.phoneNumber = cursor.getString(2);
                entry.publicKey = cursor.getString(3);
                entry.timestampFirstSeenPublicKey = cursor.getLong(4);
                entry.timestampLastSeenAlivePublicKey = cursor.getLong(5);
                entries.add(entry);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return entries;
    }

    public static void updateEntry(Entry entry) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        ContentValues values = createContentValues(entry);
        db.update(PublicKeyEntry.TABLE_NAME,
                values, PublicKeyEntry.COLUMN_NAME_PUBLIC_KEY + " = ?",
                new String[] { entry.publicKey });
    }

    public static void smartUpdateEntry(Entry newEntry) {
        Entry oldEntry = getEntry(newEntry.publicKey);
        if (null == oldEntry) {
            // entry not yet in db
            Entry mergedEntry = new Entry(newEntry);
            mergedEntry.timestampFirstSeenPublicKey = System.currentTimeMillis();
            addEntry(mergedEntry);
        } else {
            Entry mergedEntry = new Entry(newEntry);
            mergedEntry.timestampFirstSeenPublicKey = oldEntry.timestampFirstSeenPublicKey;
            updateEntry(mergedEntry);
        }
    }

    public static void deleteEntry(String publicKey) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        db.delete(PublicKeyEntry.TABLE_NAME,
                PublicKeyEntry.COLUMN_NAME_PUBLIC_KEY + " = ?",
                new String[] { publicKey });
    }
    
}
