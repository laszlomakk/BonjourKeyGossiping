/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
 */

package uk.ac.cam.cl.lm649.bonjourtesting;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
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

import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class BonjourService extends Service {

    private static final String TAG = "BonjourService";
    private Context context;
    private MainActivity mainActivity;
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
    private final TreeMap<ServiceStub, ServiceEvent> serviceRegistry = new TreeMap<>();
    private String nameOfOurService = "";
    private ServiceInfo serviceInfoOfOurService;

    private final ExecutorService workerThread = Executors.newFixedThreadPool(1);
    private boolean started = false;

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate() called.");
        super.onCreate();
        context = getApplicationContext();
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
            workerThread.execute(new Runnable() {
                @Override
                public void run() {
                    startWork();
                }
            });
        } else {
            Log.w(TAG, "onStartCommand(). already started.");
        }
        return START_STICKY;
    }

    private void createJmDNS() throws IOException {
        Log.i(TAG, "Creating jmDNS.");
        inetAddressOfThisDevice = HelperMethods.getWifiIpAddress(context);
        Log.i(TAG, "Device IP: "+ inetAddressOfThisDevice.getHostAddress());
        changeServiceState("creating JmDNS");
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
        HelperMethods.displayMsgToUser(context, "Starting discovery...");
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

        if (null != mainActivity) mainActivity.refreshTopUI();

        nameOfOurService = serviceInfoOfOurService.getName();
        String serviceIsRegisteredNotification = "Registered service. Name ended up being: " + nameOfOurService;
        Log.i(TAG, serviceIsRegisteredNotification);
        HelperMethods.displayMsgToUser(context, serviceIsRegisteredNotification);
    }

    protected void restartDiscovery() {
        workerThread.execute(new Runnable() {
            @Override
            public void run() {
                serviceRegistry.clear();
                startDiscovery();
                changeServiceState("READY");
            }
        });
    }

    protected void reregisterOurService() {
        workerThread.execute(new Runnable() {
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
        if (null != jmdns) {
            Log.e(TAG, "jmdns was not null. This is not a clean start-up...");
        }
        try {
            createJmDNS();
            registerOurService();
            startDiscovery();
            changeServiceState("READY");
            Log.i(TAG, "startWork() finished without error.");
        } catch (IOException e) {
            Log.e(TAG, "startWork(). Error during start-up.");
            e.printStackTrace();
        }
    }

    private void stopAndCloseWork() {
        Log.i(TAG, "stopAndCloseWork() called.");
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

    private void restartWork() {
        Log.i(TAG, "restartWork() called.");
        stopAndCloseWork();
        changeServiceState("restarting...");
        startWork();
    }

    public void attachActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        if (null != mainActivity) mainActivity.updateListView(serviceRegistry);
    }

    protected void addServiceToRegistry(final ServiceEvent event) {
        synchronized (serviceRegistry){
            ServiceStub serviceStub = new ServiceStub(event);
            serviceRegistry.put(serviceStub, event);
            if (null != mainActivity) mainActivity.updateListView(serviceRegistry);
        }
    }

    protected void removeServiceFromRegistry(final ServiceEvent event) {
        synchronized (serviceRegistry) {
            ServiceStub serviceStub = new ServiceStub(event);
            serviceRegistry.remove(serviceStub);
            if (null != mainActivity) mainActivity.updateListView(serviceRegistry);
        }
    }

    private void changeServiceState(final String state) {
        strServiceState = state;
        if (null != mainActivity) mainActivity.refreshTopUI();
    }

    public String getIPAdress() {
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
