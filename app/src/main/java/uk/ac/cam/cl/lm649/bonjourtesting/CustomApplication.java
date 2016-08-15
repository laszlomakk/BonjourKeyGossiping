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
import android.support.annotation.Nullable;
import android.util.Log;

import org.slf4j.LoggerFactory;

import java.io.IOException;

import uk.ac.cam.cl.lm649.bonjourtesting.bonjour.BonjourService;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgServerManager;
import uk.ac.cam.cl.lm649.bonjourtesting.receivers.DeviceIdleBroadcastReceiver;
import uk.ac.cam.cl.lm649.bonjourtesting.receivers.LoggingBroadcastReceiver;
import uk.ac.cam.cl.lm649.bonjourtesting.menu.settings.SaveSettingsData;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public class CustomApplication extends Application {

    private static final String TAG = "CustomApplication";
    private static CustomApplication INSTANCE = null;
    private Thread.UncaughtExceptionHandler defaultExceptionHandler;

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

        FLogger.i(TAG, "application version: " + HelperMethods.getVersionNameExtended(this));

        setupExceptionCatching();

        configJmDnsLogLevel();
        registerReceivers();

        startupOperationalCore();

        generateOurCryptoKeypair();
    }

    public void startupOperationalCore() {
        FLogger.i(TAG, "startupOperationalCore() called.");
        if (SaveSettingsData.getInstance(this).isAppOperationalCoreEnabled()) {
            Log.i(TAG, "onCreate(). AppOperationalCore setting is ON, so starting up.");
            startMsgServerManager();
            PollingService.automaticPollingEnabled = true;
            PollingService.schedulePolling(this);
            startBonjourService();
        } else {
            Log.i(TAG, "onCreate(). AppOperationalCore setting is OFF, won't start.");
        }
    }

    public void shutdownOperationalCore() {
        FLogger.i(TAG, "shutdownOperationalCore() called.");
        stopBonjourService();
        PollingService.automaticPollingEnabled = false;
        PollingService.cancelPolling(this);
        MsgServerManager.getInstance().stop();
    }

    private void generateOurCryptoKeypair() {
        FLogger.i(TAG, "generateOurCryptoKeypair() called.");
        new Thread() {
            @Override
            public void run() {
                SaveIdentityData.getInstance(CustomApplication.this).generateAndSaveMyKeypair(false);
            }
        }.start();
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

    private void setupExceptionCatching() {
        FLogger.i(TAG, "setupExceptionCatching() called.");
        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();

        // setup handler for uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler (new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException (Thread thread, Throwable e) {
                FLogger.e(TAG, "uncaughtException(). " + e.toString());
                FLogger.e(TAG, "uncaughtException(). " + HelperMethods.formatStackTraceAsString(e));

                // now rethrow the exception as if we didn't intercept it
                defaultExceptionHandler.uncaughtException(thread, e);
            }
        });
    }

    private void startMsgServerManager() {
        try {
            MsgServerManager.getInstance().start();
        } catch (IOException e) {
            FLogger.e(TAG, "startMsgServerManager(). IOE - " + e.getMessage());
            HelperMethods.displayMsgToUser(this, "failed to start MsgServers");
            FLogger.e(TAG, HelperMethods.formatStackTraceAsString(e));
        }
    }

    protected void startBonjourService() {
        FLogger.i(TAG, "Starting and binding BonjourService.");
        Intent intent = new Intent(this, BonjourService.class);
        startService(intent); // explicit start will keep the service alive
        bindService(intent, bonjourServiceConnection, Context.BIND_AUTO_CREATE);
    }

    protected void stopBonjourService() {
        FLogger.i(TAG, "Stopping BonjourService.");
        Intent intent = new Intent(this, BonjourService.class);
        unbindService(bonjourServiceConnection);
        stopService(intent);
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
            root2.setLevel(Constants.JmDnsLogLevel);
        } else {
            FLogger.e(TAG, "configJmDnsLogLevel(). Didn't recognise slf4j binding type...");
        }

    }

    @Nullable
    public BonjourService getBonjourService() {
        return bonjourServiceBound ? bonjourService : null;
    }

    public void setTopActivity(CustomActivity topActivity) {
        this.topActivity = topActivity;
    }

    @Nullable
    public CustomActivity getTopActivity() {
        return topActivity;
    }

    public static CustomApplication getInstance() {
        return INSTANCE;
    }

}
