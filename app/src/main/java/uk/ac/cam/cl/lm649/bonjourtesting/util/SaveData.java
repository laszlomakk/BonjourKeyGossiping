package uk.ac.cam.cl.lm649.bonjourtesting.util;

import android.content.Context;
import android.content.SharedPreferences;

public abstract class SaveData {

    protected final SharedPreferences sharedPreferences;

    protected SaveData(Context context, String sharedPrefName) {
        sharedPreferences = context.getSharedPreferences(
                sharedPrefName, Context.MODE_PRIVATE);
    }

}
