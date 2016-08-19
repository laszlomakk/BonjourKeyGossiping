package uk.ac.cam.cl.lm649.bonjourtesting.bonjour.polling;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public class NetworkPollingReceiver extends WakefulBroadcastReceiver {

    private static final String TAG = "NetworkPollingReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        FLogger.i(TAG, "onReceive(). received intent.  action: " + intent.getAction());
        Intent service = new Intent(context, NetworkPollingService.class);

        // start the service, keeping the device awake while it is launching
        startWakefulService(context, service);
    }

}
