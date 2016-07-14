package uk.ac.cam.cl.lm649.bonjourtesting;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

public class CustomApplication extends Application {

    private static final String TAG = "CustomApplication";

    private ServiceConnection bonjourServiceConnection = new ServiceConnection() {
        private static final String TAG = "BonjourServiceConn";
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG, "onServiceConnected() called.");
            BonjourService.BonjourServiceBinder binder = (BonjourService.BonjourServiceBinder) service;
            bonjourService = binder.getService();
            bonjourServiceBound = true;
            if (null != mainActivity) bonjourService.attachActivity(mainActivity);
        }
        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.i(TAG, "onServiceDisconnected() called.");
            bonjourServiceBound = false;
            bonjourService.attachActivity(null);
        }
    };

    private BonjourService bonjourService;
    private boolean bonjourServiceBound = false;

    private MainActivity mainActivity = null;

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate() called.");
        super.onCreate();

        try {
            MsgServer.initInstance();
        } catch (IOException e) {
            Log.e(TAG, "onCreate(). Failed to init MsgServer.");
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

    public MainActivity getMainActivity() {
        return mainActivity;
    }

    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        if (bonjourServiceBound) bonjourService.attachActivity(mainActivity);
    }

}
