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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;

public class HelperMethods {

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

    public static String getWifiIpAddress(Context context){
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        String ipString = String.format(Locale.US, "%d.%d.%d.%d",
                (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
        return ipString;
    }

    public static String getDetailedString(ServiceEvent event){
        StringBuffer sb = new StringBuffer();
        sb.append("name: ").append(event.getName()).append("\n");
        sb.append("type: ").append(event.getType()).append("\n");
        ServiceInfo info = event.getInfo();
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

    public static String getNamePlusTypeString(ServiceEvent event){
        StringBuffer sb = new StringBuffer();
        sb.append("name: ").append(event.getName()).append("\n");
        sb.append("type: ").append(event.getType()).append("\n");
        return sb.toString();
    }

}
