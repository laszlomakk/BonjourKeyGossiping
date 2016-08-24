package uk.ac.cam.cl.lm649.bonjourtesting.database.tables.publickeys;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import uk.ac.cam.cl.lm649.bonjourtesting.database.DbHelper;

import static uk.ac.cam.cl.lm649.bonjourtesting.database.DbContract.TablePhoneNumbers;
import static uk.ac.cam.cl.lm649.bonjourtesting.database.DbContract.TablePublicKeys;

public final class DbTablePublicKeys {

    private static final String TAG = "DbTablePublicKeys";

    private DbTablePublicKeys() {}

    public static String constructQueryToCreateTable() {
        return String.format(Locale.US,
                "CREATE TABLE %s(%s TEXT PRIMARY KEY, %s TEXT, %s INTEGER, %s INTEGER, %s BLOB)",
                TablePublicKeys.TABLE_NAME,
                TablePublicKeys.COLUMN_NAME_PUBLIC_KEY,
                TablePublicKeys.COLUMN_NAME_PHONE_NUMBER,
                TablePublicKeys.COLUMN_NAME_TIMESTAMP_FIRST_SEEN_PUBLIC_KEY,
                TablePublicKeys.COLUMN_NAME_TIMESTAMP_LAST_SEEN_ALIVE_PUBLIC_KEY,
                TablePublicKeys.COLUMN_NAME_SIGNED_HASH);
    }

    public static void addEntry(PublicKeyEntry entry) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        ContentValues values = createContentValues(entry);

        db.insert(TablePublicKeys.TABLE_NAME, null, values);
    }

    private static ContentValues createContentValues(PublicKeyEntry entry) {
        ContentValues values = new ContentValues();
        values.put(TablePublicKeys.COLUMN_NAME_PUBLIC_KEY, entry.publicKey);
        values.put(TablePublicKeys.COLUMN_NAME_PHONE_NUMBER, entry.phoneNumber);
        values.put(TablePublicKeys.COLUMN_NAME_TIMESTAMP_FIRST_SEEN_PUBLIC_KEY, entry.timestampFirstSeenPublicKey);
        values.put(TablePublicKeys.COLUMN_NAME_TIMESTAMP_LAST_SEEN_ALIVE_PUBLIC_KEY, entry.timestampLastSeenAlivePublicKey);
        values.put(TablePublicKeys.COLUMN_NAME_SIGNED_HASH, entry.signedHash);
        return values;
    }

    @Nullable
    public static PublicKeyEntry getEntry(String publicKey) {
        SQLiteDatabase db = DbHelper.getInstance().getReadableDatabase();

        Cursor cursor = null;
        try {
            cursor = db.query(TablePublicKeys.TABLE_NAME,
                    new String[] {
                            TablePublicKeys.COLUMN_NAME_PUBLIC_KEY,
                            TablePublicKeys.COLUMN_NAME_PHONE_NUMBER,
                            TablePublicKeys.COLUMN_NAME_TIMESTAMP_FIRST_SEEN_PUBLIC_KEY,
                            TablePublicKeys.COLUMN_NAME_TIMESTAMP_LAST_SEEN_ALIVE_PUBLIC_KEY,
                            TablePublicKeys.COLUMN_NAME_SIGNED_HASH,
                    }, TablePublicKeys.COLUMN_NAME_PUBLIC_KEY + "=?",
                    new String[] { publicKey }, null, null, null, null);

            if (cursor.moveToFirst()) {
                PublicKeyEntry entry = new PublicKeyEntry();
                entry.publicKey = cursor.getString(0);
                entry.phoneNumber = cursor.getString(1);
                entry.timestampFirstSeenPublicKey = cursor.getLong(2);
                entry.timestampLastSeenAlivePublicKey = cursor.getLong(3);
                entry.signedHash = cursor.getBlob(4);
                return entry;
            } else {
                return null;
            }
        } finally {
            if (null != cursor) cursor.close();
        }
    }

    @NonNull
    public static List<PublicKeyEntry> getEntriesForPhoneNumber(String phoneNumber) {
        SQLiteDatabase db = DbHelper.getInstance().getReadableDatabase();

        Cursor cursor = null;
        try {
            cursor = db.query(TablePublicKeys.TABLE_NAME,
                    new String[] {
                            TablePublicKeys.COLUMN_NAME_PUBLIC_KEY,
                            TablePublicKeys.COLUMN_NAME_PHONE_NUMBER,
                            TablePublicKeys.COLUMN_NAME_TIMESTAMP_FIRST_SEEN_PUBLIC_KEY,
                            TablePublicKeys.COLUMN_NAME_TIMESTAMP_LAST_SEEN_ALIVE_PUBLIC_KEY,
                            TablePublicKeys.COLUMN_NAME_SIGNED_HASH,
                    }, TablePublicKeys.COLUMN_NAME_PHONE_NUMBER + "=?",
                    new String[] { phoneNumber }, null, null, null, null);

            List<PublicKeyEntry> entries = new ArrayList<>();
            if (cursor.moveToFirst()) {
                do {
                    PublicKeyEntry entry = new PublicKeyEntry();
                    entry.publicKey = cursor.getString(0);
                    entry.phoneNumber = cursor.getString(1);
                    entry.timestampFirstSeenPublicKey = cursor.getLong(2);
                    entry.timestampLastSeenAlivePublicKey = cursor.getLong(3);
                    entry.signedHash = cursor.getBlob(4);
                    entries.add(entry);
                } while (cursor.moveToNext());
            }
            return entries;

        } finally {
            if (null != cursor) cursor.close();
        }
    }

    @NonNull
    public static List<PublicKeyEntryWithName> getAllEntries() {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        String selectQuery = String.format(Locale.US,
                "SELECT %s, %s, %s, %s, %s, %s FROM %s LEFT JOIN %s ON %s=%s ORDER BY %s DESC",
                TablePhoneNumbers.TABLE_NAME + "." + TablePhoneNumbers.COLUMN_NAME_CUSTOM_NAME,
                TablePublicKeys.TABLE_NAME   + "." + TablePublicKeys.COLUMN_NAME_PHONE_NUMBER,
                TablePublicKeys.TABLE_NAME   + "." + TablePublicKeys.COLUMN_NAME_PUBLIC_KEY,
                TablePublicKeys.TABLE_NAME   + "." + TablePublicKeys.COLUMN_NAME_TIMESTAMP_FIRST_SEEN_PUBLIC_KEY,
                TablePublicKeys.TABLE_NAME   + "." + TablePublicKeys.COLUMN_NAME_TIMESTAMP_LAST_SEEN_ALIVE_PUBLIC_KEY,
                TablePublicKeys.TABLE_NAME   + "." + TablePublicKeys.COLUMN_NAME_SIGNED_HASH,
                TablePublicKeys.TABLE_NAME,
                TablePhoneNumbers.TABLE_NAME,
                TablePublicKeys.TABLE_NAME   + "." + TablePublicKeys.COLUMN_NAME_PHONE_NUMBER,
                TablePhoneNumbers.TABLE_NAME + "." + TablePhoneNumbers.COLUMN_NAME_PHONE_NUMBER,
                TablePublicKeys.TABLE_NAME   + "." + TablePublicKeys.COLUMN_NAME_TIMESTAMP_LAST_SEEN_ALIVE_PUBLIC_KEY);

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(selectQuery, null);

            List<PublicKeyEntryWithName> entries = new ArrayList<>();
            if (cursor.moveToFirst()) {
                do {
                    PublicKeyEntryWithName entry = new PublicKeyEntryWithName();
                    entry.customName = cursor.getString(0);
                    entry.phoneNumber = cursor.getString(1);
                    entry.publicKey = cursor.getString(2);
                    entry.timestampFirstSeenPublicKey = cursor.getLong(3);
                    entry.timestampLastSeenAlivePublicKey = cursor.getLong(4);
                    entry.signedHash = cursor.getBlob(5);
                    entries.add(entry);
                } while (cursor.moveToNext());
            }
            return entries;
        } finally {
            if (null != cursor) cursor.close();
        }
    }

    public static void updateEntry(PublicKeyEntry entry) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        ContentValues values = createContentValues(entry);
        db.update(TablePublicKeys.TABLE_NAME,
                values, TablePublicKeys.COLUMN_NAME_PUBLIC_KEY + " = ?",
                new String[] { entry.publicKey });
    }

    public static void smartUpdateEntry(PublicKeyEntry newEntry) {
        PublicKeyEntry oldEntry = getEntry(newEntry.publicKey);
        if (null == oldEntry) {
            // entry not yet in db
            PublicKeyEntry mergedEntry = new PublicKeyEntry(newEntry);
            mergedEntry.timestampFirstSeenPublicKey = System.currentTimeMillis();
            addEntry(mergedEntry);
        } else {
            PublicKeyEntry mergedEntry = new PublicKeyEntry(newEntry);
            mergedEntry.timestampFirstSeenPublicKey = oldEntry.timestampFirstSeenPublicKey;
            updateEntry(mergedEntry);
        }
    }

    public static void deleteEntry(String publicKey) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        db.delete(TablePublicKeys.TABLE_NAME,
                TablePublicKeys.COLUMN_NAME_PUBLIC_KEY + " = ?",
                new String[] { publicKey });
    }
    
}
