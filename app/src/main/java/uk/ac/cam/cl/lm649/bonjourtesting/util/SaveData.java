package uk.ac.cam.cl.lm649.bonjourtesting.util;

import android.content.Context;
import android.content.SharedPreferences;

public abstract class SaveData {

    protected final Context context;
    protected final SharedPreferences sharedPreferences;

    protected SaveData(Context context, String sharedPrefName) {
        this.context = context;
        sharedPreferences = context.getApplicationContext().getSharedPreferences(
                sharedPrefName, Context.MODE_PRIVATE);
    }

}
