package uk.ac.cam.cl.lm649.bonjourtesting.menu.settings;

import android.content.Context;
import android.content.SharedPreferences;

import uk.ac.cam.cl.lm649.bonjourtesting.R;
import uk.ac.cam.cl.lm649.bonjourtesting.savedata.SaveData;

public class SaveSettingsData extends SaveData {

    private static SaveSettingsData INSTANCE = null;

    private static final String SAVE_LOCATION_FOR_APP_OPERATIONAL_CORE_ENABLED = "app_operations_enabled";
    private static final String SAVE_LOCATION_FOR_RANDOM_SERVICE_NAME = "service_name_is_random";

    private SaveSettingsData(Context context) {
        super(context, context.getString(R.string.settings_save_location));
    }

    public static synchronized SaveSettingsData getInstance(Context context) {
        if (null == INSTANCE) {
            INSTANCE = new SaveSettingsData(context);
        }
        return INSTANCE;
    }

    public void saveAppOperationalCoreEnabled(boolean bool) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(SAVE_LOCATION_FOR_APP_OPERATIONAL_CORE_ENABLED, bool);
        editor.apply();
    }

    public boolean isAppOperationalCoreEnabled() {
        return sharedPreferences.getBoolean(SAVE_LOCATION_FOR_APP_OPERATIONAL_CORE_ENABLED, true);
    }

    public void saveUsingRandomServiceName(boolean bool){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(SAVE_LOCATION_FOR_RANDOM_SERVICE_NAME, bool);
        editor.apply();
    }

    public boolean isUsingRandomServiceName(){
        return sharedPreferences.getBoolean(SAVE_LOCATION_FOR_RANDOM_SERVICE_NAME, true);
    }

}