package uk.ac.cam.cl.lm649.bonjourtesting;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public class ConnectivityChangeReceiver extends BroadcastReceiver {

    private static final String TAG = "ConnectivityChangeRec";

    @Override
    public void onReceive(Context context, Intent intent) {
        FLogger.i(TAG, "onReceive(). received intent");
        //debugIntent(intent);
        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            // there was a change of connectivity
            if (isThisANewWifiConnectionThatWeJustEstablished(intent)) {
                FLogger.i(TAG, "onReceive(). decided to act on intent");
                debugIntent(intent);
                Context appContext = context.getApplicationContext();
                if (!(appContext instanceof CustomApplication)) {
                    FLogger.e(TAG, "onReceive(). wtf. can't access Application.");
                    return;
                }
                CustomApplication app = (CustomApplication) appContext;
                if (!app.isBonjourServiceBound()) {
                    FLogger.w(TAG, "onReceive(). bonjourService not bound. this is unexpected.");
                    return;
                }
                FLogger.i(TAG, "onReceive(). calling bonjourService.restartWork()");
                app.getBonjourService().restartWork(false);
            }
        }
    }

    private boolean isThisANewWifiConnectionThatWeJustEstablished(Intent intent) {
        NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (null == netInfo || !netInfo.isConnected()) {
            return false;
        }
        Bundle extras = intent.getExtras();
        if (null == extras || null == extras.get("bssid")) {
            return false;
        }
        return true;
    }

    private void debugIntent(Intent intent) {
        FLogger.d(TAG, "debugIntent(). action: " + intent.getAction());
        FLogger.d(TAG, "debugIntent(). component: " + intent.getComponent());
        Bundle extras = intent.getExtras();
        if (null != extras) {
            for (String key: extras.keySet()) {
                FLogger.d(TAG, "debugIntent(). key [" + key + "]: " + extras.get(key));
            }
        }
        else {
            FLogger.d(TAG, "debugIntent(). no extras");
        }
    }

}
