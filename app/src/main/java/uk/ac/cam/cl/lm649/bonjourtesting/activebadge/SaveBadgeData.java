package uk.ac.cam.cl.lm649.bonjourtesting.activebadge;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

import uk.ac.cam.cl.lm649.bonjourtesting.R;
import uk.ac.cam.cl.lm649.bonjourtesting.util.SaveData;

public class SaveBadgeData extends SaveData {

    private static SaveBadgeData INSTANCE = null;

    private static final String SAVE_LOCATION_FOR_OWN_BADGE_ID = "my_badge_id";

    private SaveBadgeData(Context context) {
        super(context, context.getString(R.string.badge_save_location));
    }

    public static synchronized SaveBadgeData getInstance(Context context) {
        if (null == INSTANCE) {
            INSTANCE = new SaveBadgeData(context);
        }
        return INSTANCE;
    }

    private void saveMyBadgeId(UUID badgeId){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String strId = badgeId.toString();
        editor.putString(SAVE_LOCATION_FOR_OWN_BADGE_ID, strId);
        editor.apply();
    }

    public UUID getMyBadgeId(){
        String strId = sharedPreferences.getString(SAVE_LOCATION_FOR_OWN_BADGE_ID, "");
        UUID ret;
        if ("".equals(strId)) {
            ret = UUID.randomUUID();
            saveMyBadgeId(ret);
        } else {
            ret = UUID.fromString(strId);
        }
        return ret;
    }

}
