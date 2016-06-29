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

    private static final String TAG = "CDiscoveryListener";
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
        mainActivity.nsdManager.stopServiceDiscovery(this);
        mainActivity.displayMsgToUser("onStopDiscoveryFailed");
    }

    @Override
    public void onDiscoveryStarted(String serviceType){
        Log.d(TAG, ">>-------------- onDiscoveryStarted(). type: "+serviceType);
    }

    @Override
    public void onDiscoveryStopped(String serviceType) {
        Log.d(TAG, ">>-------------- onDiscoveryStopped(). type: "+serviceType);
    }

    @Override
    public void onServiceFound(final NsdServiceInfo serviceInfo) {
        Log.d(TAG, "onServiceFound(). "+HelperMethods.getDetailedStringFromServiceInfo(serviceInfo));
        if (!HelperMethods.equalsTrimmedFromDots(serviceInfo.getServiceType(), MainActivity.SERVICE_TYPE)) {
            Log.d(TAG, "Unknown Service Type: " + serviceInfo.getServiceType());
        }/* else if (serviceInfo.getServiceName().equals(serviceName)) {
            // we've just found our own service
            Log.d(TAG, "Same machine: " + serviceName);
        }*/ else /*if (serviceInfo.getServiceName().contains("gossip"))*/{
            new Thread(){
                @Override
                public void run(){
                    mainActivity.resolutionWorker.resolveService(serviceInfo);
                }
            }.start();
        }
    }

    @Override
    public void onServiceLost(NsdServiceInfo serviceInfo) {
        Log.d(TAG, "onServiceLost(). "+HelperMethods.getDetailedStringFromServiceInfo(serviceInfo));
        //TODO think about whether this removal works
        mainActivity.removeItemFromList(HelperMethods.getDetailedStringFromServiceInfo(serviceInfo));
    }

}
