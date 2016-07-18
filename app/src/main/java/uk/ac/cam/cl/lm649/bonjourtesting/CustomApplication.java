package uk.ac.cam.cl.lm649.bonjourtesting;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

import uk.ac.cam.cl.lm649.bonjourtesting.bonjour.BonjourService;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;
import uk.ac.cam.cl.lm649.bonjourtesting.util.Logger;

public class CustomApplication extends Application {

    private static final String TAG = "CustomApplication";
    private static CustomApplication INSTANCE = null;

    private ServiceConnection bonjourServiceConnection = new ServiceConnection() {
        private static final String TAG = "BonjourServiceConn";
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG, "onServiceConnected() called.");
            BonjourService.BonjourServiceBinder binder = (BonjourService.BonjourServiceBinder) service;
            bonjourService = binder.getService();
            bonjourServiceBound = true;
            if (null != bonjourDebugActivity) bonjourService.attachBonjourDebugActivity(bonjourDebugActivity);
        }
        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.i(TAG, "onServiceDisconnected() called.");
            bonjourServiceBound = false;
            bonjourService.attachBonjourDebugActivity(null);
        }
    };

    private BonjourService bonjourService;
    private boolean bonjourServiceBound = false;

    private BonjourDebugActivity bonjourDebugActivity = null;

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate() called.");
        super.onCreate();

        INSTANCE = this;

        try {
            Logger.init(this);
        } catch (IOException e) {
            Log.e(TAG, "onCreate(). Failed to init Logger.");
            HelperMethods.displayMsgToUser(this, "failed to init Logger");
            e.printStackTrace();
        }

        try {
            MsgServer.initInstance();
        } catch (IOException e) {
            Log.e(TAG, "onCreate(). Failed to init MsgServer.");
            HelperMethods.displayMsgToUser(this, "failed to init MsgServer");
            e.printStackTrace();
        }

        Log.i(TAG, "Starting and binding BonjourService.");
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

    public void setBonjourDebugActivity(BonjourDebugActivity bonjourDebugActivity) {
        this.bonjourDebugActivity = bonjourDebugActivity;
        if (bonjourServiceBound) bonjourService.attachBonjourDebugActivity(bonjourDebugActivity);
    }

    public static CustomApplication getInstance() {
        return INSTANCE;
    }

}
