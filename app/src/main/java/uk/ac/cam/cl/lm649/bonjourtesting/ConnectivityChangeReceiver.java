package uk.ac.cam.cl.lm649.bonjourtesting;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

public class ConnectivityChangeReceiver extends BroadcastReceiver {

    private static final String TAG = "ConnectivityChangeRec";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "onReceive(). received intent"); // TODO this should be level INFO
        debugIntent(intent);
        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            // there was a change of connectivity
            if (isThisANewWifiConnectionThatWeJustEstablished(intent)) {
                Log.e(TAG, "onReceive(). decided to act on intent"); // TODO this should be level INFO
                Context appContext = context.getApplicationContext();
                if (!(appContext instanceof CustomApplication)) {
                    Log.e(TAG, "onReceive(). wtf. can't access Application.");
                    return;
                }
                CustomApplication app = (CustomApplication) appContext;
                if (!app.isBonjourServiceBound()) {
                    Log.w(TAG, "onReceive(). bonjourService not bound. this is unexpected.");
                    return;
                }
                app.getBonjourService().restartWork();
            }
        }
    }

    private boolean isThisANewWifiConnectionThatWeJustEstablished(Intent intent) {
        NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (null == netInfo || !netInfo.isConnected()) {
            return false;
        }
        Bundle extras = intent.getExtras();
        if (null == extras) {
            return false;
        }
        if (null == extras.get("bssid")){
            return false;
        }
        return true;
    }

    private void debugIntent(Intent intent) {
        Log.d(TAG, "action: " + intent.getAction());
        Log.d(TAG, "component: " + intent.getComponent());
        Bundle extras = intent.getExtras();
        if (null != extras) {
            for (String key: extras.keySet()) {
                Log.d(TAG, "key [" + key + "]: " + extras.get(key));
            }
        }
        else {
            Log.d(TAG, "no extras");
        }
    }

}
