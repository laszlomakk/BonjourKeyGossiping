package uk.ac.cam.cl.lm649.bonjourtesting.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class BootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = "BootCompletedRec";

    @Override
    public void onReceive(Context context, Intent intent) {
        FLogger.i(TAG, "onReceive(). received intent.  action: " + intent.getAction());
        HelperMethods.debugIntent(TAG, intent);
        // note: we are not doing anything startup specific
        // the fact that this method gets called ensures that CustomApplication
        // gets instantiated which means the singletons get instantiated and BonjourService starts
    }
}
