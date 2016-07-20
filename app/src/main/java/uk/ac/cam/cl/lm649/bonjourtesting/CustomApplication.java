package uk.ac.cam.cl.lm649.bonjourtesting;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.ActiveBadgePoller;
import uk.ac.cam.cl.lm649.bonjourtesting.bonjour.BonjourService;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgServer;
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
        initMsgServer();
        ActiveBadgePoller.getInstance();
        startBonjourService();
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
