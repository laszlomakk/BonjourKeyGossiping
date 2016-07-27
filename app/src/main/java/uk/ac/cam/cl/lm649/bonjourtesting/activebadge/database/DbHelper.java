package uk.ac.cam.cl.lm649.bonjourtesting.activebadge.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Locale;

import uk.ac.cam.cl.lm649.bonjourtesting.CustomApplication;

import static uk.ac.cam.cl.lm649.bonjourtesting.activebadge.database.DbContract.BadgeEntry;
import static uk.ac.cam.cl.lm649.bonjourtesting.activebadge.database.DbContract.HistoryTransferEntry;

public class DbHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static DbHelper INSTANCE = null;

    private DbHelper(Context context) {
        super(context, DbContract.DATABASE_NAME, null, DbContract.DATABASE_VERSION);
    }

    protected static synchronized DbHelper getInstance(Context context) {
        if (null == INSTANCE) {
            INSTANCE = new DbHelper(context);
        }
        return INSTANCE;
    }

    protected static DbHelper getInstance() {
        return getInstance(CustomApplication.getInstance());
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

        String CREATE_HISTORY_TRANSFER_TABLE = String.format(Locale.US,
                "CREATE TABLE %s(%s TEXT PRIMARY KEY, %s INTEGER)",
                HistoryTransferEntry.TABLE_NAME,
                HistoryTransferEntry.COLUMN_NAME_BADGE_ID,
                HistoryTransferEntry.COLUMN_NAME_HISTORY_TRANSFER_TIMESTAMP);
        db.execSQL(CREATE_HISTORY_TRANSFER_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + BadgeEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + HistoryTransferEntry.TABLE_NAME);
        onCreate(db);
    }



}
