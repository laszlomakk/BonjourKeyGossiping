package uk.ac.cam.cl.lm649.bonjourtesting.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

import uk.ac.cam.cl.lm649.bonjourtesting.BuildConfig;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class DeviceIdleBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "DevIdleBroadcastRec";

    @Override
    public void onReceive(Context context, Intent intent) {
        FLogger.i(TAG, "onReceive(). received intent.  action: " + intent.getAction());
        HelperMethods.debugIntent(TAG, intent);

        if (Build.VERSION.SDK_INT >= 23) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            FLogger.i(TAG, "device idle mode: " + pm.isDeviceIdleMode());
            FLogger.i(TAG, "ignoring battery optimisations: "
                    + pm.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID));
        }
    }

}
