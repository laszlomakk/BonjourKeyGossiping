package uk.ac.cam.cl.lm649.bonjourtesting.settings;

import android.content.Context;
import android.content.SharedPreferences;

import uk.ac.cam.cl.lm649.bonjourtesting.Constants;
import uk.ac.cam.cl.lm649.bonjourtesting.R;
import uk.ac.cam.cl.lm649.bonjourtesting.util.SaveData;

public class SaveSettingsData extends SaveData {

    private static SaveSettingsData INSTANCE = null;

    private static final String SAVE_LOCATION_FOR_CUSTOM_SERVICE_NAME = "custom_service_name";
    private static final String SAVE_LOCATION_FOR_RANDOM_SERVICE_NAME = "service_name_is_random";
    private static final String SAVE_LOCATION_FOR_SERVICE_TYPE = "service_type";

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

    public void saveServiceType(String str){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SAVE_LOCATION_FOR_SERVICE_TYPE, str);
        editor.apply();
    }

    public String getServiceType(){
        return sharedPreferences.getString(SAVE_LOCATION_FOR_SERVICE_TYPE, Constants.DEFAULT_SERVICE_TYPE);
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