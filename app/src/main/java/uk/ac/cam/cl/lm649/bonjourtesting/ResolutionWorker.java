/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
 */

package uk.ac.cam.cl.lm649.bonjourtesting;

import android.net.nsd.NsdServiceInfo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class ResolutionWorker {

    private static ResolutionWorker INSTANCE = null;
    private ExecutorService threadPool = Executors.newFixedThreadPool(1);
    private MainActivity mainActivity;
    protected Semaphore available = new Semaphore(1);

    private ResolutionWorker(MainActivity mainActivity){
        this.mainActivity = mainActivity;
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

    public void resolveService(final NsdServiceInfo serviceInfo){
        try {
            available.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                CustomResolveListener.resolveService(mainActivity, serviceInfo);
            }
        });
    }

}
