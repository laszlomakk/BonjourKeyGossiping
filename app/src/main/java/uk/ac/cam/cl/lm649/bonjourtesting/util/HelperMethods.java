/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
 */

package uk.ac.cam.cl.lm649.bonjourtesting.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

import uk.ac.cam.cl.lm649.bonjourtesting.Constants;

public final class HelperMethods {

    private static final String TAG = "HelperMethods";

    private HelperMethods() {}

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

    /**
     * @param time java-style timestamp (milliseconds elapsed since 1970 UTC)
     * @return nice and concise human-readable string with the time
     */
    public static String getTimeStamp(long time) {
        return new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss.SSSZ", Locale.US)
                .format(new Timestamp(time));
    }

    public static String getVersionName(Context context) {
        String versionName = "UNKNOWN";
        try {
            versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            FLogger.e(TAG,
                    "getVersionName(). PackageManager.NameNotFoundException: " + e);
        }
        return versionName;
    }

    public static String getVersionNameExtended(Context context) {
        return getVersionName(context);
    }

    /**
     * @return uniform random long A, such that min <= A < max
     */
    public static long getRandomLongBetween(long min, long max) {
        Random random = new Random();
        long diff = max - min;
        long ret = random.nextLong() % diff;
        if (ret < 0) ret += diff;
        ret += min;
        return ret;
    }

    public static String formatStackTraceAsString(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    public static UUID uuidFromStringDefensively(String str) {
        UUID ret;
        try {
            ret = UUID.fromString(str);
        } catch (Exception e) {
            FLogger.e(TAG, "uuidFromStringDefensively() encountered Exception: " + e);
            FLogger.d(TAG, e);
            return null;
        }
        return ret;
    }

    public static byte[] getNLowBitsOfByteArray(@NonNull byte[] byteArr, int nBitsToReveal) {
        nBitsToReveal = Math.min(nBitsToReveal, byteArr.length * 8);
        nBitsToReveal = Math.max(nBitsToReveal, 0);

        int nBytesNeededToStoreNBits = (nBitsToReveal + 7) / 8;
        byte[] revealedBitsOfHash = new byte[nBytesNeededToStoreNBits];
        System.arraycopy(byteArr, 0, revealedBitsOfHash, 0, revealedBitsOfHash.length);

        int nBitsToKeepInLastByte = nBitsToReveal % 8;
        if (nBitsToKeepInLastByte != 0) { // zero means we keep the whole byte
            // need to hide a few bits of last byte
            int mask = (1 << nBitsToKeepInLastByte) - 1;
            revealedBitsOfHash[revealedBitsOfHash.length-1] &= mask;
        }

        return revealedBitsOfHash;
    }

    public static void askForPermissionToReadContacts(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.READ_CONTACTS},
                Constants.MY_PERMISSIONS_REQUEST_READ_CONTACTS);
    }

    public static boolean doWeHavePermissionToReadContacts(Context context) {
        return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

}
