package uk.ac.cam.cl.lm649.bonjourtesting.menu.settings;

import android.content.Context;
import android.content.SharedPreferences;

import uk.ac.cam.cl.lm649.bonjourtesting.R;
import uk.ac.cam.cl.lm649.bonjourtesting.util.SaveData;

public class SaveSettingsData extends SaveData {

    private static SaveSettingsData INSTANCE = null;

    private static final String SAVE_LOCATION_FOR_APP_OPERATIONAL_CORE_ENABLED = "app_operations_enabled";
    private static final String SAVE_LOCATION_FOR_CUSTOM_SERVICE_NAME = "custom_service_name";
    private static final String SAVE_LOCATION_FOR_RANDOM_SERVICE_NAME = "service_name_is_random";
    private static final String SAVE_LOCATION_FOR_OWN_PHONE_NUMBER = "phone_number";

    private SaveSettingsData(Context context) {
        super(context, context.getString(R.string.settings_save_location));
    }

    public static synchronized SaveSettingsData getInstance(Context context) {
        if (null == INSTANCE) {
            INSTANCE = new SaveSettingsData(context);
        }
        return INSTANCE;
    }

    public void saveCustomServiceName(String str){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SAVE_LOCATION_FOR_CUSTOM_SERVICE_NAME, str);
        editor.apply();
    }

    public String getCustomServiceName(){
        return sharedPreferences.getString(SAVE_LOCATION_FOR_CUSTOM_SERVICE_NAME, "myServiceName");
    }

    public void savePhoneNumber(String str) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SAVE_LOCATION_FOR_OWN_PHONE_NUMBER, str);
        editor.apply();
    }

    public String getPhoneNumber(){
        return sharedPreferences.getString(SAVE_LOCATION_FOR_OWN_PHONE_NUMBER, "+441234567890");
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