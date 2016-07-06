/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
 */

package uk.ac.cam.cl.lm649.bonjourtesting.util;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Random;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;

public class HelperMethods {

    private static final String TAG = "HelperMethods";

    public static Enumeration<InetAddress> getWifiInetAddresses(final Context context) {
        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        final String macAddress = wifiInfo.getMacAddress();
        final String[] macParts = macAddress.split(":");
        final byte[] macBytes = new byte[macParts.length];
        for (int i = 0; i< macParts.length; i++) {
            macBytes[i] = (byte)Integer.parseInt(macParts[i], 16);
        }
        try {
            final Enumeration<NetworkInterface> e =  NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                final NetworkInterface networkInterface = e.nextElement();
                if (Arrays.equals(networkInterface.getHardwareAddress(), macBytes)) {
                    return networkInterface.getInetAddresses();
                }
            }
        } catch (SocketException e) {
            Log.wtf("WIFIIP", "Unable to NetworkInterface.getNetworkInterfaces()");
        }
        return null;
    }

    /**
     * Example usage:
     * final Inet4Address inet4Address = getWifiInetAddress(context, Inet4Address.class);
     */
    @SuppressWarnings("unchecked")
    public static<T extends InetAddress> T getWifiInetAddress(final Context context, final Class<T> inetClass) {
        final Enumeration<InetAddress> e = getWifiInetAddresses(context);
        if (null == e) return null;
        while (e.hasMoreElements()) {
            final InetAddress inetAddress = e.nextElement();
            if (inetAddress.getClass() == inetClass) {
                return (T)inetAddress;
            }
        }
        return null;
    }

    /**
     * @return IPv4 address of this Android device on the local wi-fi network
     */
    public static InetAddress getWifiIpAddress(Context context){
        InetAddress ret = null;
        try {
            // default to Android localhost
            ret = InetAddress.getByName("10.0.0.2");

            // try to figure out our wifi address, or fail
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiinfo = wifiManager.getConnectionInfo();
            int ip = wifiinfo.getIpAddress();
            byte[] byteaddr = new byte[] { (byte) (ip & 0xff), (byte) (ip >> 8 & 0xff), (byte) (ip >> 16 & 0xff), (byte) (ip >> 24 & 0xff) };
            ret = InetAddress.getByAddress(byteaddr);
        } catch (UnknownHostException ex) {
            Log.e(TAG, String.format("getWifiIpAddress() Error: %s", ex.getMessage()));
        }
        return ret;
    }

    public static String getDetailedString(ServiceEvent event){
        return getNameAndTypeString(event)+getAddressesAndPortString(event);
    }

    public static String getDetailedString(ServiceInfo info){
        return getNameAndTypeString(info)+getAddressesAndPortString(info);
    }

    public static String getAddressesAndPortString(ServiceEvent event){
        ServiceInfo info = event.getInfo();
        return getAddressesAndPortString(info);
    }

    public static String getAddressesAndPortString(ServiceInfo info){
        StringBuffer sb = new StringBuffer();
        if (info != null){
            String[] addresses = info.getHostAddresses();
            if (addresses != null){
                sb.append("addresses: ");
                for (String addr : addresses){
                    sb.append(addr).append(", ");
                }
                sb.append("\n");
            }
            sb.append("port: ").append(info.getPort());
        }
        return sb.toString();
    }

    public static String getNameAndTypeString(ServiceEvent event){
        StringBuffer sb = new StringBuffer();
        sb.append("name: ").append(event.getName()).append("\n");
        sb.append("type: ").append(event.getType()).append("\n");
        return sb.toString();
    }

    public static String getNameAndTypeString(ServiceInfo info){
        StringBuffer sb = new StringBuffer();
        sb.append("name: ").append(info.getName()).append("\n");
        sb.append("type: ").append(info.getType()).append("\n");
        return sb.toString();
    }

    public static String getNRandomDigits(int n){
        String ret = "";
        Random rand = new Random();
        for (int i=0; i<n; i++){
            ret += rand.nextInt(10);
        }
        return ret;
    }

}
