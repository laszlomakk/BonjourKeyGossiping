/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
 */

package uk.ac.cam.cl.lm649.bonjourtesting;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;

import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class MainActivity extends Activity {

    public static final String SERVICE_TYPE = "_vsecserv3._tcp.local.";
    private String serviceName = "";
    private int port = 45267; // arbitrary default value, will get changed

    private static final String TAG = "MainActivity";
    private Context context;

    private ArrayAdapter<String> listAdapterForDisplayedListOfServices;
    private TreeMap<ServiceStub, ServiceEvent> servicesFoundMap = new TreeMap<>();
    private ArrayList<ServiceEvent> servicesFoundArrList = new ArrayList<>();
    private final Object servicesFoundLock = new Object();

    private TextView textViewAppState;
    private TextView textViewDeviceIp;
    private TextView textViewLocalPort;
    private TextView textViewOwnService;
    private TextView textViewNumServicesFound;

    protected JmDNS jmdns;
    private InetAddress inetAddressOfThisDevice;
    private CustomServiceListener serviceListener;
    private MsgServer msgServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        setupUI();
    }

    private void setupUI(){
        setContentView(R.layout.activity_main);

        // listView for displaying the services we find on the network
        ListView listView = (ListView) findViewById(R.id.mainListView);
        listAdapterForDisplayedListOfServices = new ArrayAdapter<>(this, R.layout.row_in_list, new ArrayList<String>());
        listView.setAdapter(listAdapterForDisplayedListOfServices);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                synchronized (servicesFoundLock){
                    ServiceInfo serviceInfo = servicesFoundArrList.get(position).getInfo();
                    if (null == serviceInfo){
                        Log.e(TAG, "onItemClick(). error sending msg: serviceInfo is null");
                        displayMsgToUser("error sending msg: serviceInfo is null (1)");
                        return;
                    }
                    String msg = "Hy there! I see your payload is "+serviceInfo.getNiceTextString();
                    MsgServer.sendMessage(MainActivity.this, serviceInfo, serviceName, msg);
                }
            }
        });

        // top area
        textViewAppState = (TextView) findViewById(R.id.appState);
        textViewDeviceIp = (TextView) findViewById(R.id.deviceIp);
        textViewLocalPort = (TextView) findViewById(R.id.localPort) ;
        textViewOwnService = (TextView) findViewById(R.id.ownService);
        textViewNumServicesFound = (TextView) findViewById(R.id.numberOfServicesFound);

        // settings button
        findViewById(R.id.settingsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent("uk.ac.cam.cl.lm649.bonjourtesting.SETTINGS"));
            }
        });

        // refresh button
        findViewById(R.id.refreshButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetListOfServicesFound();
                startDiscovery();
                changeAppState("READY");
            }
        });

        initUI();
    }

    private void initUI(){
        textViewAppState.setText("-");
        textViewDeviceIp.setText("-");
        textViewLocalPort.setText("-");
        textViewOwnService.setText("-");
        textViewNumServicesFound.setText("-");
    }

    private void resetUI(){
        Log.i(TAG, "Resetting UI.");
        initUI();
        resetListOfServicesFound();
    }

    private void resetListOfServicesFound(){
        servicesFoundMap.clear();
        updateListView();
    }

    @Override
    protected void onStart(){
        Log.i(TAG, "Activity starting up.");
        super.onStart();
        new Thread(){
            @Override
            public void run(){
                try {
                    createServerForIncomingMessages();
                    createJmDNS();
                    registerOurService();
                    startDiscovery();
                    changeAppState("READY");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void createJmDNS() throws IOException {
        Log.i(TAG, "Creating jmDNS.");
        inetAddressOfThisDevice = HelperMethods.getWifiIpAddress(MainActivity.this);
        Log.i(TAG, "Device IP: "+ inetAddressOfThisDevice.getHostAddress());
        changeAppState("creating JmDNS");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewDeviceIp.setText(inetAddressOfThisDevice.getHostAddress());
            }
        });
        jmdns = JmDNS.create(inetAddressOfThisDevice);
    }

    private void createServerForIncomingMessages() throws IOException {
        Log.i(TAG, "Creating MsgServer.");
        changeAppState("creating Msg Server");
        msgServer = new MsgServer(MainActivity.this);
        port = msgServer.getPort();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewLocalPort.setText(String.format(Locale.US,"%d",port));
            }
        });
    }

    private void startDiscovery(){
        Log.i(TAG, "Starting discovery.");
        changeAppState("starting discovery");
        if (null == jmdns){
            Log.e(TAG, "startDiscovery(). jmdns is null");
            return;
        }
        if (serviceListener != null){
            Log.i(TAG, "startDiscovery(). serviceListener wasn't null. Removing prev listener");
            jmdns.removeServiceListener(SERVICE_TYPE, serviceListener);
        }
        serviceListener = new CustomServiceListener(this);
        jmdns.addServiceListener(SERVICE_TYPE, serviceListener);
        displayMsgToUser("Starting discovery...");
    }

    private void registerOurService() throws IOException {
        Log.i(TAG, "Registering our own service.");
        changeAppState("registering our service");
        if (SaveSettingsData.getInstance(this).isUsingRandomServiceName()) {
            serviceName = Constants.RANDOM_SERVICE_NAMES_START_WITH + HelperMethods.getNRandomDigits(5);
        } else {
            serviceName = SaveSettingsData.getInstance(this).getCustomServiceName();
        }
        String payload;
        if (Constants.FIXED_DNS_TXT_RECORD){
            payload = Constants.DNS_TXT_RECORD;
        } else {
            payload = HelperMethods.getRandomString();
        }
        final ServiceInfo serviceInfo = ServiceInfo.create(SERVICE_TYPE, serviceName, port, payload);
        jmdns.registerService(serviceInfo);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String text = HelperMethods.getNameAndTypeString(serviceInfo)
                        + HelperMethods.getPayloadString(serviceInfo);
                textViewOwnService.setText(text);
            }
        });

        serviceName = serviceInfo.getName();
        String serviceIsRegisteredNotification = "Registered service. Name ended up being: "+serviceName;
        Log.i(TAG, serviceIsRegisteredNotification);
        displayMsgToUser(serviceIsRegisteredNotification);
    }

    @Override
    protected void onStop(){
        Log.i(TAG, "Activity stopping.");
        super.onStop();
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
        msgServer.stop();
        resetUI();
    }

    protected void displayMsgToUser(final String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    protected void addItemToList(final ServiceEvent event){
        synchronized (servicesFoundLock){
            ServiceStub serviceStub = new ServiceStub(event);
            servicesFoundMap.put(serviceStub, event);
            updateListView();
        }
    }

    protected void removeItemFromList(final ServiceEvent event){
        synchronized (servicesFoundLock) {
            ServiceStub serviceStub = new ServiceStub(event);
            servicesFoundMap.remove(serviceStub);
            updateListView();
        }
    }

    private void updateListView(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (servicesFoundLock) {
                        textViewNumServicesFound.setText(String.valueOf(servicesFoundMap.size()));
                        listAdapterForDisplayedListOfServices.clear();
                        servicesFoundArrList.clear();
                        for (ServiceEvent serviceEvent : servicesFoundMap.values()) {
                            listAdapterForDisplayedListOfServices.add(HelperMethods.getDetailedString(serviceEvent));
                            servicesFoundArrList.add(serviceEvent);
                        }
                        listAdapterForDisplayedListOfServices.notifyDataSetChanged();
                    }
                } catch (RuntimeException e){
                    e.printStackTrace();
                }
            }
        });
    }

    public String getServiceName() {
        return serviceName;
    }

    private void changeAppState(final String state){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewAppState.setText(state);
            }
        });
    }

    //use this as: serviceInfo.setText(createPayloadMapForService());
    private Map<String, byte[]> createPayloadMapForService(){
        Map<String, byte[]> payloadMap = new HashMap<>();
        // long hardcoded data to test large payloads
        /*byte[] s1 = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        payloadMap.put("s1", s1);
        byte[] s2 = new byte[] {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31};
        payloadMap.put("s2", s2);*/
        // short hardcoded data to test other things
        byte[] s3 = new byte[]{7,8,3,5};
        payloadMap.put("pear", s3);
        return payloadMap;
    }

}
