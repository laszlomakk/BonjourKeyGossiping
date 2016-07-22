/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
 */

package uk.ac.cam.cl.lm649.bonjourtesting.util;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Random;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;

public class HelperMethods {

    private static final String TAG = "HelperMethods";

    private HelperMethods() {}

    public static String getDetailedString(ServiceEvent event){
        return getNameAndTypeString(event)
                +getAddressesAndPortString(event)
                +"\n"+getPayloadString(event);
    }

    public static String getDetailedString(ServiceInfo info){
        return getNameAndTypeString(info)
                +getAddressesAndPortString(info)
                +"\n"+getPayloadString(info);
    }

    public static String getAddressesAndPortString(ServiceEvent event){
        return getAddressesAndPortString(event.getInfo());
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

    public static String getPayloadString(ServiceEvent event){
        return getPayloadString(event.getInfo());
    }

    public static String getPayloadString(ServiceInfo info){
        StringBuffer sb = new StringBuffer();
        if (info != null){
            sb.append("payload: ").append(info.getNiceTextString());
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

    public static String getRandomString() {
        SecureRandom random = new SecureRandom();
        return new BigInteger(60, random).toString(Character.MAX_RADIX);
    }

    public static void displayMsgToUser(final Context context, final String msg) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                FLogger.d(TAG, "displayed toast to user with msg: " + msg);
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void debugIntent(String logTag, Intent intent) {
        FLogger.d(logTag, "debugIntent(). action: " + intent.getAction());
        FLogger.d(logTag, "debugIntent(). component: " + intent.getComponent());
        Bundle extras = intent.getExtras();
        if (null != extras) {
            for (String key: extras.keySet()) {
                FLogger.d(logTag, "debugIntent(). key [" + key + "]: " + extras.get(key));
            }
        }
        else {
            FLogger.d(logTag, "debugIntent(). no extras");
        }
    }

    public static String getTimeStamp(long time) {
        return new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss.SSS", Locale.US)
                .format(new Timestamp(time));
    }

}
