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
        db.execSQL(DbTableBadges.constructQueryToCreateTable());
        db.execSQL(DbTableHistoryTransfer.constructQueryToCreateTable());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + BadgeEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + HistoryTransferEntry.TABLE_NAME);
        onCreate(db);
    }



}
