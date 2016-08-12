package uk.ac.cam.cl.lm649.bonjourtesting.savedata;

import android.content.Context;
import android.content.SharedPreferences;

public abstract class SaveData {

    protected final SharedPreferences sharedPreferences;

    protected SaveData(Context context, String sharedPrefName) {
        sharedPreferences = context.getApplicationContext().getSharedPreferences(
                sharedPrefName, Context.MODE_PRIVATE);
    }

}
