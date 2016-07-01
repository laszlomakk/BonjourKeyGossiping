/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
 */

package uk.ac.cam.cl.lm649.bonjourtesting;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import com.android.internal.util.AsyncChannel;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class ResolutionWorker {

    private static final String TAG = "ResolutionWorker";
    private static ResolutionWorker INSTANCE = null;
    private ExecutorService threadPool = Executors.newFixedThreadPool(1);
    private MainActivity mainActivity;
    protected Semaphore available = new Semaphore(1);

    private int oldNResolutionFinished = 0;
    private boolean wasWorkingLastTime = false;
    private static final int CHECK_MAKING_PROGRESS_PERIOD = 5000;
    protected int requestIdInNsdService;

    private ResolutionWorker(MainActivity mainActivity){
        this.mainActivity = mainActivity;
        checkWorkerIsMakingProgress();
    }

    protected static ResolutionWorker getInstance(MainActivity mainActivity){
        if (null == INSTANCE){
            INSTANCE = new ResolutionWorker(mainActivity);
        } else if (INSTANCE.mainActivity != mainActivity) {
            INSTANCE.threadPool.shutdown();
            INSTANCE = new ResolutionWorker(mainActivity);
        }
        return INSTANCE;
    }

    private void checkWorkerIsMakingProgress(){
        boolean working = available.availablePermits() == 0;
        if (working && wasWorkingLastTime
                && oldNResolutionFinished == CustomResolveListener.nResolutionFinished){
            //worker is stuck
            Log.d(TAG, "worker is stuck. trying to resolve...");

            try {
                Field f = mainActivity.nsdManager.getClass().getDeclaredField("mAsyncChannel");
                f.setAccessible(true);
                AsyncChannel mAsyncChannel = (AsyncChannel) f.get(mainActivity.nsdManager);
                mAsyncChannel.disconnect();
                Log.d(TAG, "disconnect sent to NsdManager and NsdService");
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "resetting nsdManager. releasing semaphore.");
            mainActivity.nsdManager = (NsdManager) mainActivity.getSystemService(Context.NSD_SERVICE);
            available.release();
        }

        oldNResolutionFinished = CustomResolveListener.nResolutionFinished;
        wasWorkingLastTime = working;
        mainActivity.rootView.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkWorkerIsMakingProgress();
            }
        }, CHECK_MAKING_PROGRESS_PERIOD);
    }

    public void resolveService(final NsdServiceInfo serviceInfo){
        try {
            available.acquire();
            Log.d(TAG, "semaphore acquired");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "threadPool starts resolution on: \n"
                        + HelperMethods.getDetailedStringFromServiceInfo(serviceInfo));
                CustomResolveListener.resolveService(mainActivity, serviceInfo);
            }
        });
    }

}
