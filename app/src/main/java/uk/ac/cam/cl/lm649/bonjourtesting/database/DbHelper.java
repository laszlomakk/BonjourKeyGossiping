package uk.ac.cam.cl.lm649.bonjourtesting.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import uk.ac.cam.cl.lm649.bonjourtesting.CustomApplication;

import static uk.ac.cam.cl.lm649.bonjourtesting.database.DbContract.PhoneNumberEntry;
import static uk.ac.cam.cl.lm649.bonjourtesting.database.DbContract.PublicKeyEntry;

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
        db.execSQL(DbTablePhoneNumbers.constructQueryToCreateTable());
        db.execSQL(DbTablePublicKeys.constructQueryToCreateTable());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + PhoneNumberEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + PublicKeyEntry.TABLE_NAME);
        onCreate(db);
    }



}
