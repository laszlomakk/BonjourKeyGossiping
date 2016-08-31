package uk.ac.cam.cl.lm649.bonjourtesting.util;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class NetworkUtil {

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
            FLogger.e(TAG, "getWifiIpAddress(). UnknownHostException: " + e);
        }
        return ret;
    }

    public static String getRouterMacAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String routerMac = wifiInfo.getBSSID();
        return null == routerMac ? "00:00:00:00:00:00" : routerMac;
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

    public static boolean isPortValid(int port) {
        return 0 <= port && port <= 65535;
    }

    /**
     * Try to extract a hardware MAC address from a given IP address using the
     * ARP cache (/proc/net/arp).<br>
     * <br>
     * We assume that the file has this structure:<br>
     * <br>
     * IP address       HW type     Flags       HW address            Mask     Device
     * 192.168.18.11    0x1         0x2         00:04:20:06:55:1a     *        eth0
     * 192.168.18.36    0x1         0x2         00:22:43:ab:2a:5b     *        eth0
     *
     * source: http://www.flattermann.net/2011/02/android-howto-find-the-hardware-mac-address-of-a-remote-host/
     *
     * @param ip
     * @return the MAC from the ARP cache
     */
    @Nullable
    public static String getMacFromArpCache(@Nullable String ip) {
        if (ip == null) return null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] splitted = line.split(" +");
                if (splitted != null && splitted.length >= 4 && ip.equals(splitted[0])) {
                    // Basic sanity check
                    String mac = splitted[3];
                    if (mac.matches("..:..:..:..:..:..")) {
                        return mac;
                    } else {
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            FLogger.e(TAG, "getMacFromArpCache(). " + e);
            FLogger.d(TAG, e);
        } finally {
            try {
                if (null != br) br.close();
            } catch (IOException e) {
                FLogger.e(TAG, "getMacFromArpCache(). " + e);
                FLogger.d(TAG, e);
            }
        }
        return null;
    }

}
