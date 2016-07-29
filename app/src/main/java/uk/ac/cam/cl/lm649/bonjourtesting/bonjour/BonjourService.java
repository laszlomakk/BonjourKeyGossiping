/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
 */

package uk.ac.cam.cl.lm649.bonjourtesting.bonjour;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Locale;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;

import uk.ac.cam.cl.lm649.bonjourtesting.CustomActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.receivers.ConnectivityChangeReceiver;
import uk.ac.cam.cl.lm649.bonjourtesting.Constants;
import uk.ac.cam.cl.lm649.bonjourtesting.CustomApplication;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgServer;
import uk.ac.cam.cl.lm649.bonjourtesting.settings.SaveSettingsData;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;
import uk.ac.cam.cl.lm649.bonjourtesting.util.NetworkUtil;
import uk.ac.cam.cl.lm649.bonjourtesting.util.ServiceStub;

public class BonjourService extends Service {

    private static final String TAG = "BonjourService";
    private CustomApplication app;
    private Context context;
    private String strServiceState = "-";

    private static final long TIME_TO_WAIT_BETWEEN_STARTUP_RETRIES = 15_000;

    private final IBinder binder = new BonjourServiceBinder();

    public class BonjourServiceBinder extends Binder {
        public BonjourService getService() {
            return BonjourService.this;
        }
    }

    protected JmDNS jmdns;
    private InetAddress inetAddressOfThisDevice;
    private CustomServiceListener serviceListener;
    private ConnectivityChangeReceiver connectivityChangeReceiver;
    private final TreeMap<ServiceStub, ServiceEvent> serviceRegistry = new TreeMap<>();
    private String nameOfOurService = "";
    private ServiceInfo serviceInfoOfOurService;

    private SaveSettingsData saveSettingsData;

    private Handler handler;
    private boolean started = false;

    @Override
    public void onCreate() {
        FLogger.i(TAG, "onCreate() called.");
        super.onCreate();
        app = (CustomApplication) getApplication();
        context = app;

        saveSettingsData = SaveSettingsData.getInstance(context);

        HandlerThread thread = new HandlerThread("BonjServ-handler");
        thread.start();
        handler = new Handler(thread.getLooper());
    }

    @Override
    public void onDestroy() {
        FLogger.i(TAG, "onDestroy() called.");
        unregisterReceiver(connectivityChangeReceiver);
        stopAndCloseWork();
        synchronized (serviceRegistry) {
            serviceRegistry.clear();
        }
        started = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        FLogger.i(TAG, "onStartCommand() called.");
        if (!started){
            started = true;
            FLogger.i(TAG, "onStartCommand(). doing start-up.");
            startWork(false);
            registerConnectivityChangeReceiver();
        } else {
            FLogger.w(TAG, "onStartCommand(). already started.");
        }
        return START_STICKY;
    }

    private void createJmDNS() throws IOException {
        inetAddressOfThisDevice = NetworkUtil.getWifiIpAddress(context);
        FLogger.i(TAG, "Device IP: "+ inetAddressOfThisDevice.getHostAddress());
        changeServiceState("creating JmDNS");
        FLogger.i(TAG, "Creating jmDNS.");
        jmdns = JmDNS.create(inetAddressOfThisDevice);
    }

    private void startDiscovery(){
        FLogger.i(TAG, "Starting discovery.");
        changeServiceState("starting discovery");
        synchronized (serviceRegistry) {
            serviceRegistry.clear();
            CustomActivity.forceRefreshUIInTopActivity();
        }
        if (null == jmdns){
            FLogger.e(TAG, "startDiscovery(). jmdns is null");
            return;
        }
        if (serviceListener != null){
            FLogger.i(TAG, "startDiscovery(). serviceListener wasn't null. Removing prev listener");
            jmdns.removeServiceListener(saveSettingsData.getServiceType(), serviceListener);
        }
        serviceListener = new CustomServiceListener(this);
        jmdns.addServiceListener(saveSettingsData.getServiceType(), serviceListener);
        //HelperMethods.displayMsgToUser(context, "Starting discovery...");
    }

    private void registerOurService() throws IOException {
        FLogger.i(TAG, "Registering our own service.");
        changeServiceState("registering our service");
        if (SaveSettingsData.getInstance(this).isUsingRandomServiceName()) {
            nameOfOurService = Constants.RANDOM_SERVICE_NAMES_START_WITH + HelperMethods.getNRandomDigits(5);
        } else {
            nameOfOurService = SaveSettingsData.getInstance(this).getCustomServiceName();
        }
        String payload;
        if (Constants.FIXED_DNS_TXT_RECORD){
            payload = Constants.DNS_TXT_RECORD;
        } else {
            payload = HelperMethods.getRandomString();
        }
        int port = MsgServer.getInstance().getPort();
        serviceInfoOfOurService = ServiceInfo.create(saveSettingsData.getServiceType(), nameOfOurService, port, payload);
        jmdns.registerService(serviceInfoOfOurService);

        CustomActivity.forceRefreshUIInTopActivity();

        nameOfOurService = serviceInfoOfOurService.getName();
        String serviceIsRegisteredNotification = "Registered service. Name ended up being: " + nameOfOurService;
        FLogger.i(TAG, serviceIsRegisteredNotification);
        //HelperMethods.displayMsgToUser(context, serviceIsRegisteredNotification);
    }

    private void registerConnectivityChangeReceiver() {
        FLogger.i(TAG, "Registering ConnectivityChangeReceiver.");
        changeServiceState("registering cc-receiver");
        connectivityChangeReceiver = new ConnectivityChangeReceiver();
        registerReceiver(
                connectivityChangeReceiver,
                new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    }

    public void restartDiscovery() {
        FLogger.i(TAG, "restartDiscovery() called.");
        if (null != serviceListener && serviceListener.getDiscoveredOurOwnService()) {
            FLogger.i(TAG, "restartDiscovery(). service seems to be working correctly, " +
                    "as we DISCOVERED ourselves last time. Proceeding with normal operation: discovery.");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    startDiscovery();
                    changeServiceState("READY");
                }
            });
        } else {
            FLogger.w(TAG, "restartDiscovery(). we DID NOT DISCOVER our own mDNS service since last " +
                    "discovery restart. Calling restartWork() instead.");
            restartWork(false);
        }

    }

    public void reregisterOurService() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                jmdns.unregisterAllServices();
                try {
                    registerOurService();
                    changeServiceState("READY");
                } catch (IOException e) {
                    FLogger.e(TAG, "reregisterOurService() failed to register service. IOE - " + e.getMessage());
                    e.printStackTrace();
                    changeServiceState("ERROR - rereg failed");
                }
            }
        });
    }

    private void startWork(final boolean iHaveRestartSemaphore) {
        FLogger.i(TAG, "startWork() called. Caller has restartSemaphore: " + iHaveRestartSemaphore);
        handler.post(new Runnable() {
            @Override
            public void run() {
                FLogger.i(TAG, "startWork() is being executed.");
                if (null != jmdns) {
                    FLogger.e(TAG, "jmdns was not null. This is not a clean start-up...");
                }
                try {
                    createJmDNS();
                    registerOurService();
                    startDiscovery();
                    changeServiceState("READY");
                    //HelperMethods.displayMsgToUser(context, "BonjourService started w/o error");
                    if (iHaveRestartSemaphore) {
                        FLogger.i(TAG, "startWork() finished without error. Releasing restartSemaphore.");
                        restartSemaphore.release();
                    } else {
                        FLogger.i(TAG, "startWork() finished without error. Don't have restartSemaphore to release.");
                    }
                } catch (IOException e) {
                    FLogger.e(TAG, "startWork(). Error during start-up: IOE - " + e.getMessage());
                    //HelperMethods.displayMsgToUser(context, "Error during start-up: IOE");
                    changeServiceState("error during start-up: IOE");
                    e.printStackTrace();
                    FLogger.i(TAG, String.format(
                            Locale.US, "sleeping for %d seconds and then retrying start-up...",
                            TIME_TO_WAIT_BETWEEN_STARTUP_RETRIES/1000));
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            FLogger.i(TAG, "sleep ended. retrying start-up... We have restartSemaphore: " + iHaveRestartSemaphore);
                            restartWork(iHaveRestartSemaphore);
                        }
                    }, TIME_TO_WAIT_BETWEEN_STARTUP_RETRIES);
                }
            }
        });
    }

    public void stopAndCloseWork() {
        FLogger.i(TAG, "stopAndCloseWork() called.");
        handler.post(new Runnable() {
             @Override
             public void run() {
                 FLogger.i(TAG, "stopAndCloseWork() is being executed.");
                 if (jmdns != null) {
                     FLogger.i(TAG, "Stopping jmDNS...");
                     jmdns.unregisterAllServices();
                     try {
                         jmdns.close();
                     } catch (IOException e) {
                         FLogger.e(TAG, "error closing jmdns. IOE - " + e.getMessage());
                         e.printStackTrace();
                     } finally {
                         jmdns = null;
                     }
                 }
             }
        });
    }

    private Semaphore restartSemaphore = new Semaphore(2);
    // only allow two restarts to be queued
    // without this, restarts could theoretically queue up e.g. during sleep and then result in a restart loop
    // we need to allow two though, as if an important event occurs while a restart is going on,
    // another restart might be needed
    public void restartWork(boolean iHaveRestartSemaphore) {
        FLogger.i(TAG, "restartWork() called. Caller has restartSemaphore: " + iHaveRestartSemaphore);
        if (!iHaveRestartSemaphore) {
            if (restartSemaphore.tryAcquire()) {
                FLogger.i(TAG, "restartWork() acquired restartSemaphore. Going forward.");
            } else {
                FLogger.i(TAG, "restartWork() could not acquire restartSemaphore. Cancelling restart.");
                return;
            }
        }

        stopAndCloseWork();
        startWork(true);
    }

    protected void addServiceToRegistry(final ServiceEvent event) {
        synchronized (serviceRegistry){
            ServiceStub serviceStub = new ServiceStub(event);
            serviceRegistry.put(serviceStub, event);
            CustomActivity.forceRefreshUIInTopActivity();
        }
    }

    protected void removeServiceFromRegistry(final ServiceEvent event) {
        synchronized (serviceRegistry) {
            ServiceStub serviceStub = new ServiceStub(event);
            serviceRegistry.remove(serviceStub);
            CustomActivity.forceRefreshUIInTopActivity();
        }
    }

    private void changeServiceState(final String state) {
        strServiceState = state;
        CustomActivity.forceRefreshUIInTopActivity();
    }

    public String getIPAddress() {
        String ret = "999.999.999.999";
        if (null != inetAddressOfThisDevice) ret = inetAddressOfThisDevice.getHostAddress();
        return ret;
    }

    public String getNameOfOurService() {
        return nameOfOurService;
    }

    public ServiceInfo getServiceInfoOfOurService() {
        return serviceInfoOfOurService;
    }

    public TreeMap<ServiceStub, ServiceEvent> getServiceRegistry() {
        return serviceRegistry;
    }

    public String getStrServiceState() {
        return strServiceState;
    }

}
