package uk.ac.cam.cl.lm649.bonjourtesting.util;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetworkUtil {

    private static final String TAG = "NetworkUtil";

    private NetworkUtil() {}

    public enum NetworkConnectionType {
        NOT_CONNECTED, WIFI, MOBILE
    }

    /**
     * note: requires android.permission.ACCESS_NETWORK_STATE
     */
    public static NetworkConnectionType getConnectivityStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        if (null != activeNetwork) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                return NetworkConnectionType.WIFI;
            }
            if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                return NetworkConnectionType.MOBILE;
            }
        }
        return NetworkConnectionType.NOT_CONNECTED;
    }

    /**
     * @return IPv4 address of this Android device on the local wi-fi network
     */
    public static InetAddress getWifiIpAddress(Context context){
        InetAddress ret = null;
        try {
            // default to Android localhost
            ret = InetAddress.getByName("10.0.2.2");

            // try to figure out our wifi address, or fail
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiinfo = wifiManager.getConnectionInfo();
            int ip = wifiinfo.getIpAddress();
            byte[] byteaddr = new byte[] { (byte) (ip & 0xff), (byte) (ip >> 8 & 0xff), (byte) (ip >> 16 & 0xff), (byte) (ip >> 24 & 0xff) };
            ret = InetAddress.getByAddress(byteaddr);
        } catch (UnknownHostException e) {
            FLogger.e(TAG, String.format("getWifiIpAddress() Error: %s", e.getMessage()));
        }
        return ret;
    }

    public static String getRouterMacAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo.getBSSID();
    }

    public static NetworkInfo.State getWifiState(Intent intent) {
        NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (null == netInfo) return null;
        return netInfo.getState();
    }

    public static String getWifiStateString(Intent intent) {
        NetworkInfo.State state = getWifiState(intent);
        return null == state ? "null" : state.name();
    }

}
