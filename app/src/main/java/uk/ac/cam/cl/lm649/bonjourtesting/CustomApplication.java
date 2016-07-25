package uk.ac.cam.cl.lm649.bonjourtesting;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import org.slf4j.LoggerFactory;

import java.io.IOException;

import ch.qos.logback.classic.Level;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.ActiveBadgePollerService;
import uk.ac.cam.cl.lm649.bonjourtesting.bonjour.BonjourService;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgServer;
import uk.ac.cam.cl.lm649.bonjourtesting.receivers.DeviceIdleBroadcastReceiver;
import uk.ac.cam.cl.lm649.bonjourtesting.receivers.LoggingBroadcastReceiver;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public class CustomApplication extends Application {

    private static final String TAG = "CustomApplication";
    private static CustomApplication INSTANCE = null;

    private ServiceConnection bonjourServiceConnection = new ServiceConnection() {
        private static final String TAG = "BonjourServiceConn";
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            FLogger.i(TAG, "onServiceConnected() called.");
            BonjourService.BonjourServiceBinder binder = (BonjourService.BonjourServiceBinder) service;
            bonjourService = binder.getService();
            bonjourServiceBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName className) {
            FLogger.i(TAG, "onServiceDisconnected() called.");
            bonjourServiceBound = false;
        }
    };

    private BonjourService bonjourService;
    private boolean bonjourServiceBound = false;

    private CustomActivity topActivity = null;

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate() called.");
        super.onCreate();
        INSTANCE = this;

        initLogger();

        FLogger.i(TAG, "application version: " + HelperMethods.getVersionName(this));

        configJmDnsLogLevel();
        initMsgServer();
        ActiveBadgePollerService.schedulePolling(this);
        startBonjourService();
        registerReceivers();
    }

    private void initLogger() {
        try {
            FLogger.init(this);
        } catch (IOException e) {
            Log.e(TAG, "onCreate(). Failed to init Logger. IOE - " + e.getMessage());
            HelperMethods.displayMsgToUser(this, "failed to init Logger");
            e.printStackTrace();
        }
    }

    private void initMsgServer() {
        try {
            MsgServer.initInstance();
        } catch (IOException e) {
            FLogger.e(TAG, "onCreate(). Failed to init MsgServer. IOE - " + e.getMessage());
            HelperMethods.displayMsgToUser(this, "failed to init MsgServer");
            e.printStackTrace();
        }
    }

    private void startBonjourService() {
        FLogger.i(TAG, "Starting and binding BonjourService.");
        Intent intent = new Intent(this, BonjourService.class);
        startService(intent); // explicit start will keep the service alive
        bindService(intent, bonjourServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void registerReceivers() { // docs/forums say these receivers can't be set in the manifest
        LoggingBroadcastReceiver loggingBroadcastReceiver = new LoggingBroadcastReceiver();
        registerReceiver(loggingBroadcastReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        registerReceiver(loggingBroadcastReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));

        if (Build.VERSION.SDK_INT >= 17) {
            registerReceiver(loggingBroadcastReceiver, new IntentFilter(Intent.ACTION_USER_BACKGROUND));
            registerReceiver(loggingBroadcastReceiver, new IntentFilter(Intent.ACTION_USER_FOREGROUND));
        }
        if (Build.VERSION.SDK_INT >= 21) {
            registerReceiver(loggingBroadcastReceiver, new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED));
        }
        if (Build.VERSION.SDK_INT >= 23) {
            DeviceIdleBroadcastReceiver deviceIdleBroadcastReceiver = new DeviceIdleBroadcastReceiver();
            registerReceiver(deviceIdleBroadcastReceiver, new IntentFilter(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED));
        }
    }

    private void configJmDnsLogLevel() {
        org.slf4j.Logger rootLogger = LoggerFactory.getLogger(
                ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        if (rootLogger instanceof ch.qos.logback.classic.Logger) {
            ch.qos.logback.classic.Logger root2 = (ch.qos.logback.classic.Logger) rootLogger;
            root2.setLevel(Level.DEBUG);
        } else {
            Log.e(TAG, "configJmDnsLogLevel(). Didn't recognise slf4j binding type...");
        }

    }

    public BonjourService getBonjourService() {
        return bonjourService;
    }

    public boolean isBonjourServiceBound() {
        return bonjourServiceBound;
    }

    public void setTopActivity(CustomActivity topActivity) {
        this.topActivity = topActivity;
    }

    public CustomActivity getTopActivity() {
        return topActivity;
    }

    public static CustomApplication getInstance() {
        return INSTANCE;
    }

}
