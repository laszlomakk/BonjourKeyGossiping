package uk.ac.cam.cl.lm649.bonjourtesting.database.tables.phonenumbers;

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

public final class DbTablePhoneNumbers {

    private static final String TAG = "DbTablePhoneNumbers";

    private DbTablePhoneNumbers() {}

    public static String constructQueryToCreateTable() {
        return String.format(Locale.US,
                "CREATE TABLE %s(%s TEXT PRIMARY KEY, %s TEXT, %s INTEGER)",
                TablePhoneNumbers.TABLE_NAME,
                TablePhoneNumbers.COLUMN_NAME_PHONE_NUMBER,
                TablePhoneNumbers.COLUMN_NAME_CUSTOM_NAME,
                TablePhoneNumbers.COLUMN_NAME_GOSSIPING_STATUS);
    }

    public static void addEntry(Contact contact) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        ContentValues values = createContentValues(contact);

        db.insert(TablePhoneNumbers.TABLE_NAME, null, values);
    }

    private static ContentValues createContentValues(Contact contact) {
        ContentValues values = new ContentValues();
        values.put(TablePhoneNumbers.COLUMN_NAME_PHONE_NUMBER, contact.getPhoneNumber());
        values.put(TablePhoneNumbers.COLUMN_NAME_CUSTOM_NAME, contact.getCustomName());
        values.put(TablePhoneNumbers.COLUMN_NAME_GOSSIPING_STATUS, contact.getGossipingStatus().getValue());
        return values;
    }

    @Nullable
    public static Contact getEntry(String phoneNumber) {
        SQLiteDatabase db = DbHelper.getInstance().getReadableDatabase();

        Cursor cursor = null;
        try {
            cursor = db.query(TablePhoneNumbers.TABLE_NAME,
                    new String[] {
                            TablePhoneNumbers.COLUMN_NAME_PHONE_NUMBER,
                            TablePhoneNumbers.COLUMN_NAME_CUSTOM_NAME,
                            TablePhoneNumbers.COLUMN_NAME_GOSSIPING_STATUS,
                    }, TablePhoneNumbers.COLUMN_NAME_PHONE_NUMBER + "=?",
                    new String[] { phoneNumber }, null, null, null, null);

            if (cursor.moveToFirst()) {
                Contact contact = new Contact();
                contact.setPhoneNumber(cursor.getString(0));
                contact.setCustomName(cursor.getString(1));
                contact.setGossipingStatus(Contact.GossipingStatus.fromIntVal(cursor.getInt(2)));
                return contact;
            } else {
                return null;
            }
        } finally {
            if (null != cursor) cursor.close();
        }
    }

    @NonNull
    public static List<Contact> getAllEntries() {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        String selectQuery = String.format(Locale.US,
                "SELECT %s, %s, %s FROM %s ORDER BY %s ",
                TablePhoneNumbers.COLUMN_NAME_PHONE_NUMBER,
                TablePhoneNumbers.COLUMN_NAME_CUSTOM_NAME,
                TablePhoneNumbers.COLUMN_NAME_GOSSIPING_STATUS,
                TablePhoneNumbers.TABLE_NAME,
                TablePhoneNumbers.COLUMN_NAME_CUSTOM_NAME);

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(selectQuery, null);

            List<Contact> entries = new ArrayList<>();
            if (cursor.moveToFirst()) {
                do {
                    Contact contact = new Contact();
                    contact.setPhoneNumber(cursor.getString(0));
                    contact.setCustomName(cursor.getString(1));
                    contact.setGossipingStatus(Contact.GossipingStatus.fromIntVal(cursor.getInt(2)));
                    entries.add(contact);
                } while (cursor.moveToNext());
            }
            return reorderEntriesToHaveOnesWithUntouchedGossipingFirst(entries);
        } finally {
            if (null != cursor) cursor.close();
        }
    }

    private static List<Contact> reorderEntriesToHaveOnesWithUntouchedGossipingFirst(@NonNull List<Contact> contacts) {
        List<Contact> reorderedEntries = new ArrayList<>(contacts.size());
        for (Contact contact : contacts) {
            if (contact.getGossipingStatus() == Contact.GossipingStatus.UNTOUCHED) {
                reorderedEntries.add(contact);
            }
        }
        for (Contact contact : contacts) {
            if (contact.getGossipingStatus() != Contact.GossipingStatus.UNTOUCHED) {
                reorderedEntries.add(contact);
            }
        }
        return reorderedEntries;
    }

    public static void updateEntry(Contact contact) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        ContentValues values = createContentValues(contact);
        db.update(TablePhoneNumbers.TABLE_NAME,
                values, TablePhoneNumbers.COLUMN_NAME_PHONE_NUMBER + " = ?",
                new String[] { contact.getPhoneNumber() });
    }

    public static void smartUpdateEntry(Contact newEntry) {
        Contact oldEntry = getEntry(newEntry.getPhoneNumber());
        if (null == oldEntry) {
            // entry not yet in db
            addEntry(newEntry);
        } else {
            Contact mergedEntry = new Contact(newEntry);
            if (null == mergedEntry.gossipingStatus) {
                mergedEntry.gossipingStatus = oldEntry.gossipingStatus;
            }
            updateEntry(mergedEntry);
        }
    }

    public static void deleteEntry(String phoneNumber) {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();

        db.delete(TablePhoneNumbers.TABLE_NAME,
                TablePhoneNumbers.COLUMN_NAME_PHONE_NUMBER + " = ?",
                new String[] { phoneNumber });
    }

    public static void deleteAllEntries() {
        SQLiteDatabase db = DbHelper.getInstance().getWritableDatabase();
        db.delete(TablePhoneNumbers.TABLE_NAME, null, null);
    }
    
}
