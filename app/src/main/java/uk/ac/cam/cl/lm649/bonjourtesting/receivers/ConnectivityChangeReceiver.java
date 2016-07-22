package uk.ac.cam.cl.lm649.bonjourtesting.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

import java.net.InetAddress;
import java.util.Locale;

import uk.ac.cam.cl.lm649.bonjourtesting.CustomApplication;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;
import uk.ac.cam.cl.lm649.bonjourtesting.util.NetworkUtil;

public class ConnectivityChangeReceiver extends BroadcastReceiver {

    private static final String TAG = "ConnChangeReceiver";

    private String prevIpAddress = null;
    private String prevRouterMac = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        FLogger.i(TAG, "onReceive(). received intent.  action: " + intent.getAction());
        // HelperMethods.debugIntent(TAG, intent);
        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            // there was a change of connectivity
            String ipAddress = NetworkUtil.getWifiIpAddress(context).getHostAddress();
            String routerMac = NetworkUtil.getRouterMacAddress(context);
            FLogger.i(TAG, String.format(Locale.US,
                    "onReceive(). our current IP address is: %s, the router MAC is: %s",
                    ipAddress, routerMac));
            boolean addressChanged = isOurIpOrRouterMacDifferentThanLastTime(ipAddress, routerMac);
            prevIpAddress = ipAddress;
            prevRouterMac = routerMac;
            if (isThisANewWifiConnectionThatWeJustEstablished(intent) && addressChanged) {
                FLogger.i(TAG, "onReceive(). decided to act on intent");
                HelperMethods.debugIntent(TAG, intent);
                Context appContext = context.getApplicationContext();
                if (!(appContext instanceof CustomApplication)) {
                    FLogger.e(TAG, "onReceive(). wtf. can't access Application.");
                    return;
                }
                CustomApplication app = (CustomApplication) appContext;
                if (!app.isBonjourServiceBound()) {
                    FLogger.w(TAG, "onReceive(). bonjourService not bound. this is unexpected.");
                    // note: after this receiver is registered on application startup,
                    // if there is an active wifi connection, a "network_state_changed" intent
                    // will be received, which then would normally result in calling restartWork()
                    // in BonjourService -- in this particular scenario,
                    // ending up in this branch IS what we expect
                    return;
                }
                FLogger.i(TAG, "onReceive(). calling bonjourService.restartWork()");
                app.getBonjourService().restartWork(false);
            } else {
                FLogger.i(TAG, "onReceive(). decided NOT to act on intent");
            }
        }
    }

    private boolean isThisANewWifiConnectionThatWeJustEstablished(Intent intent) {
        NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (null == netInfo || !netInfo.isConnected()) {
            FLogger.d(TAG, "intent not of isConnected type, no need to act");
            return false;
        }
        Bundle extras = intent.getExtras();
        if (null == extras || null == extras.get("bssid")) {
            FLogger.d(TAG, "intent is of 'noisy' isConnected type, no need to act");
            return false;
        }
        FLogger.d(TAG, "intent seems to suggest a genuinely new connection");
        return true;
    }

    private boolean isOurIpOrRouterMacDifferentThanLastTime(String ipAddress, String routerMac) {
        boolean ret;
        if (null == prevIpAddress || !prevIpAddress.equals(ipAddress)) {
            FLogger.d(TAG, "we have a different IP address than last time");
            ret = true;
        } else if (null == prevRouterMac || !prevRouterMac.equals(routerMac)) {
            FLogger.d(TAG, "the router MAC is different than last time");
            ret = true;
        } else {
            FLogger.d(TAG, "we have the same IP and the router MAC is the same as last time");
            ret = false;
        }
        return ret;
    }

}
