/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
*/

package uk.ac.cam.cl.lm649.bonjourtesting;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class CustomDiscoveryListener implements NsdManager.DiscoveryListener{

    private static final String TAG = "CustomDiscoveryListener";
    private MainActivity mainActivity;

    protected CustomDiscoveryListener(MainActivity mainActivity){
        this.mainActivity = mainActivity;
    }

    @Override
    public void onStartDiscoveryFailed(String serviceType, int errorCode) {
        Log.e(TAG, "onStartDiscoveryFailed() "+serviceType+", "+errorCode);
        mainActivity.displayMsgToUser("onStartDiscoveryFailed");
    }

    @Override
    public void onStopDiscoveryFailed(String serviceType, int errorCode) {
        Log.e(TAG, "onStopDiscoveryFailed() "+serviceType+", "+errorCode);
        mainActivity.displayMsgToUser("onStopDiscoveryFailed");
    }

    @Override
    public void onDiscoveryStarted(String serviceType){
        Log.d(TAG, ">>-------------- onDiscoveryStarted()"+serviceType);
    }

    @Override
    public void onDiscoveryStopped(String serviceType) {
        Log.d(TAG, ">>-------------- onDiscoveryStopped()"+serviceType);
    }

    @Override
    public void onServiceFound(NsdServiceInfo serviceInfo) {
        Log.d(TAG, "onServiceFound()");
        Log.d(TAG, HelperMethods.getNameAndTypeStringFromServiceInfo(serviceInfo));
        Log.d(TAG, HelperMethods.getHostAndPortStringFromServiceInfo(serviceInfo));
        mainActivity.addItemToList(HelperMethods.getDetailedStringFromServiceInfo(serviceInfo));
    }

    @Override
    public void onServiceLost(NsdServiceInfo serviceInfo) {
        Log.d(TAG, "onServiceLost()");
        Log.d(TAG, HelperMethods.getNameAndTypeStringFromServiceInfo(serviceInfo));
        Log.d(TAG, HelperMethods.getHostAndPortStringFromServiceInfo(serviceInfo));
        mainActivity.removeItemFromList(HelperMethods.getDetailedStringFromServiceInfo(serviceInfo));
    }

}
