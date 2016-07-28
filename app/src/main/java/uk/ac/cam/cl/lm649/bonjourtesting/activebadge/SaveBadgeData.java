package uk.ac.cam.cl.lm649.bonjourtesting.activebadge;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

import uk.ac.cam.cl.lm649.bonjourtesting.R;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.SaveData;

public class SaveBadgeData extends SaveData {

    private static final String TAG = "SaveBadgeData";

    private static SaveBadgeData INSTANCE = null;

    private static final String SAVE_LOCATION_FOR_OWN_BADGE_ID = "my_badge_id";
    private static final String SAVE_LOCATION_FOR_OWN_BADGE_CUSTOM_NAME = "my_badge_custom_name";

    private SaveBadgeData(Context context) {
        super(context, context.getString(R.string.badge_save_location));
    }

    public static synchronized SaveBadgeData getInstance(Context context) {
        if (null == INSTANCE) {
            INSTANCE = new SaveBadgeData(context);
        }
        return INSTANCE;
    }

    public void deleteMyBadge() {
        FLogger.i(TAG, "deleteMyBadge() called.");
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    private void saveMyBadgeId(UUID badgeId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String strId = badgeId.toString();
        editor.putString(SAVE_LOCATION_FOR_OWN_BADGE_ID, strId);
        editor.apply();
    }

    public UUID getMyBadgeId() {
        String strId = sharedPreferences.getString(SAVE_LOCATION_FOR_OWN_BADGE_ID, "");
        UUID ret;
        if ("".equals(strId)) {
            ret = UUID.randomUUID();
            FLogger.i(TAG, "Generated a new badgeId for us: " + ret.toString());
            saveMyBadgeId(ret);
        } else {
            ret = UUID.fromString(strId);
        }
        return ret;
    }

    public void saveMyBadgeCustomName(String customName) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SAVE_LOCATION_FOR_OWN_BADGE_CUSTOM_NAME, customName);
        editor.apply();
    }

    public String getMyBadgeCustomName() {
        return sharedPreferences.getString(SAVE_LOCATION_FOR_OWN_BADGE_CUSTOM_NAME, "");
    }

}
