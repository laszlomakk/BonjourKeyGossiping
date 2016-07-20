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
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;

import uk.ac.cam.cl.lm649.bonjourtesting.ConnectivityChangeReceiver;
import uk.ac.cam.cl.lm649.bonjourtesting.Constants;
import uk.ac.cam.cl.lm649.bonjourtesting.BonjourDebugActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.CustomApplication;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgServer;
import uk.ac.cam.cl.lm649.bonjourtesting.settings.SaveSettingsData;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;
import uk.ac.cam.cl.lm649.bonjourtesting.util.NetworkUtil;
import uk.ac.cam.cl.lm649.bonjourtesting.util.ServiceStub;

public class BonjourService extends Service {

    private static final String TAG = "BonjourService";
    private CustomApplication app;
    private Context context;
    private String strServiceState = "-";

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

    private Handler handler;
    private boolean started = false;

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate() called.");
        super.onCreate();
        app = (CustomApplication) getApplication();
        context = app;

        HandlerThread thread = new HandlerThread("BonjServ-handler");
        thread.start();
        handler = new Handler(thread.getLooper());
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy() called.");
        stopAndCloseWork();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand() called.");
        if (!started){
            started = true;
            Log.i(TAG, "onStartCommand(). doing start-up.");
            startWork();
            registerConnectivityChangeReceiver();
        } else {
            Log.w(TAG, "onStartCommand(). already started.");
        }
        return START_STICKY;
    }

    private void createJmDNS() throws IOException {
        inetAddressOfThisDevice = NetworkUtil.getWifiIpAddress(context);
        Log.i(TAG, "Device IP: "+ inetAddressOfThisDevice.getHostAddress());
        changeServiceState("creating JmDNS");
        Log.i(TAG, "Creating jmDNS.");
        jmdns = JmDNS.create(inetAddressOfThisDevice);
    }

    private void startDiscovery(){
        Log.i(TAG, "Starting discovery.");
        changeServiceState("starting discovery");
        if (null == jmdns){
            Log.e(TAG, "startDiscovery(). jmdns is null");
            return;
        }
        if (serviceListener != null){
            Log.i(TAG, "startDiscovery(). serviceListener wasn't null. Removing prev listener");
            jmdns.removeServiceListener(Constants.SERVICE_TYPE, serviceListener);
        }
        serviceListener = new CustomServiceListener(this);
        jmdns.addServiceListener(Constants.SERVICE_TYPE, serviceListener);
        //HelperMethods.displayMsgToUser(context, "Starting discovery...");
    }

    private void registerOurService() throws IOException {
        Log.i(TAG, "Registering our own service.");
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
        serviceInfoOfOurService = ServiceInfo.create(Constants.SERVICE_TYPE, nameOfOurService, port, payload);
        jmdns.registerService(serviceInfoOfOurService);

        if (app.getTopActivity() instanceof BonjourDebugActivity) ((BonjourDebugActivity)app.getTopActivity()).refreshTopUI();

        nameOfOurService = serviceInfoOfOurService.getName();
        String serviceIsRegisteredNotification = "Registered service. Name ended up being: " + nameOfOurService;
        Log.i(TAG, serviceIsRegisteredNotification);
        HelperMethods.displayMsgToUser(context, serviceIsRegisteredNotification);
    }

    private void registerConnectivityChangeReceiver() {
        Log.i(TAG, "Registering ConnectivityChangeReceiver.");
        changeServiceState("registering cc-receiver");
        connectivityChangeReceiver = new ConnectivityChangeReceiver();
        registerReceiver(
                connectivityChangeReceiver,
                new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    }

    public void restartDiscovery() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                serviceRegistry.clear();
                startDiscovery();
                changeServiceState("READY");
            }
        });
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
                    Log.e(TAG, "reregisterOurService() failed to register service.");
                    e.printStackTrace();
                    changeServiceState("ERROR - rereg failed");
                }
            }
        });
    }

    private void startWork() {
        Log.i(TAG, "startWork() called.");
        handler.post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "startWork() is being executed.");
                if (null != jmdns) {
                    Log.e(TAG, "jmdns was not null. This is not a clean start-up...");
                }
                try {
                    createJmDNS();
                    registerOurService();
                    startDiscovery();
                    changeServiceState("READY");
                    Log.i(TAG, "startWork() finished without error.");
                    HelperMethods.displayMsgToUser(context, "Start-up finished without error");
                } catch (IOException e) {
                    Log.e(TAG, "startWork(). Error during start-up: IOE - " + e.getMessage());
                    HelperMethods.displayMsgToUser(context, "Error during start-up: IOE");
                    changeServiceState("error during start-up: IOE");
                    e.printStackTrace();
                    Log.i(TAG, "sleeping for a bit and then retrying...");
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            restartWork();
                        }
                    }, 15_000);
                }
            }
        });
    }

    private void stopAndCloseWork() {
        Log.i(TAG, "stopAndCloseWork() called.");
        handler.post(new Runnable() {
             @Override
             public void run() {
                 Log.i(TAG, "stopAndCloseWork() is being executed.");
                 if (jmdns != null) {
                     Log.i(TAG, "Stopping jmDNS...");
                     jmdns.unregisterAllServices();
                     try {
                         jmdns.close();
                     } catch (IOException e) {
                         e.printStackTrace();
                     } finally {
                         jmdns = null;
                     }
                 }
             }
        });
    }

    public void restartWork() {
        Log.i(TAG, "restartWork() called.");
        stopAndCloseWork();
        startWork();
    }

    protected void addServiceToRegistry(final ServiceEvent event) {
        synchronized (serviceRegistry){
            ServiceStub serviceStub = new ServiceStub(event);
            serviceRegistry.put(serviceStub, event);
            if (app.getTopActivity() instanceof BonjourDebugActivity) ((BonjourDebugActivity)app.getTopActivity()).updateListView(serviceRegistry);
        }
    }

    protected void removeServiceFromRegistry(final ServiceEvent event) {
        synchronized (serviceRegistry) {
            ServiceStub serviceStub = new ServiceStub(event);
            serviceRegistry.remove(serviceStub);
            if (app.getTopActivity() instanceof BonjourDebugActivity) ((BonjourDebugActivity)app.getTopActivity()).updateListView(serviceRegistry);
        }
    }

    private void changeServiceState(final String state) {
        strServiceState = state;
        if (app.getTopActivity() instanceof BonjourDebugActivity) ((BonjourDebugActivity)app.getTopActivity()).refreshTopUI();
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
