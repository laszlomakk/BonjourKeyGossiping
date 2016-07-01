/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
 */

package uk.ac.cam.cl.lm649.bonjourtesting;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class CustomResolveListener implements NsdManager.ResolveListener {

    public static int nResolutionFinished = 0;

    private static final String TAG = "CResolveListener";
    private MainActivity mainActivity;

    private CustomResolveListener(MainActivity mainActivity){
        this.mainActivity = mainActivity;
    }

    @Override
    public void onResolveFailed(final NsdServiceInfo serviceInfo, int errorCode) {
        Log.e(TAG, "onResolveFailed() " + errorCode);
        nResolutionFinished++;
        ResolutionWorker.getInstance(mainActivity).available.release();
        switch (errorCode) {
            case NsdManager.FAILURE_ALREADY_ACTIVE:
                Log.e(TAG, "FAILURE_ALREADY_ACTIVE");
                // try again...
                new Thread(){
                    @Override
                    public void run(){
                        mainActivity.resolutionWorker.resolveService(serviceInfo);
                    }
                }.start();
                break;
            case NsdManager.FAILURE_INTERNAL_ERROR:
                Log.e(TAG, "FAILURE_INTERNAL_ERROR");
                break;
            case NsdManager.FAILURE_MAX_LIMIT:
                Log.e(TAG, "FAILURE_MAX_LIMIT");
                break;
        }
    }

    @Override
    public void onServiceResolved(NsdServiceInfo serviceInfo) {
        Log.d(TAG, "onServiceResolved() " + serviceInfo);
        nResolutionFinished++;
        ResolutionWorker.getInstance(mainActivity).available.release();

        /*if (serviceInfo.getServiceName().equals(serviceName)) {
            // this is our own service
            Log.d(TAG, "Same machine: " + serviceName);
            return;
        }*/

        mainActivity.addItemToList(HelperMethods.getDetailedStringFromServiceInfo(serviceInfo));
    }

    protected static void resolveService(MainActivity mainActivity, NsdServiceInfo serviceInfo){
        CustomResolveListener resolveListener = new CustomResolveListener(mainActivity);
        mainActivity.nsdManager.resolveService(serviceInfo, resolveListener);
        Log.d(TAG, "service resolution started asynchronously by library");
    }

}
