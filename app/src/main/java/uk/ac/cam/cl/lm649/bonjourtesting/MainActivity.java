/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
 */

package uk.ac.cam.cl.lm649.bonjourtesting;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TreeMap;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;

import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class MainActivity extends Activity {

    public static final String SERVICE_TYPE = "_verysecretstuff._tcp.local."; // _http._tcp.local.
    public static final String SERVICE_NAME_DEFAULT = "client_";
    private String serviceName = "";
    private int port;
    private String payloadOnOurService = "cherry topped ice cream";

    protected View rootView;

    private static final String TAG = "MainActivity";
    private Context context;
    private ListView listView;
    private ArrayAdapter<String> listAdapter;
    private TreeMap<ServiceStub, ServiceEvent> servicesFound = new TreeMap<>();
    private ArrayList<ServiceEvent> servicesFoundArrList = new ArrayList<>();
    private final Object servicesFoundLock = new Object();

    private TextView textViewDeviceIp;
    private TextView textViewLocalPort;
    private TextView textViewOwnService;

    protected JmDNS jmdns;
    private InetAddress inetAddressOfThisDevice;
    private CustomServiceListener serviceListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        setupUI();

        new Thread(){
            @Override
            public void run(){
                try {
                    createServerForIncomingMessages();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void setupUI(){
        LayoutInflater li = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rootView = li.inflate(R.layout.activity_main, null);
        setContentView(rootView);

        //listView
        listView = (ListView) findViewById(R.id.mainListView);
        listAdapter = new ArrayAdapter<>(this, R.layout.row_in_list, new ArrayList<String>());
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                synchronized (servicesFoundLock){
                    ServiceInfo serviceInfo = servicesFoundArrList.get(position).getInfo();
                    if (null == serviceInfo){
                        displayMsgToUser("don't know address for that service");
                        return;
                    }
                    MsgServer.sendMessage(MainActivity.this, serviceInfo, serviceName, "Hy there!");
                }
            }
        });

        //top area
        textViewDeviceIp = (TextView) findViewById(R.id.deviceIp);
        textViewLocalPort = (TextView) findViewById(R.id.localPort) ;
        textViewOwnService = (TextView) findViewById(R.id.ownService);
    }

    private void resetUI(){
        textViewDeviceIp.setText("");
        textViewOwnService.setText("");
        listAdapter.clear();
    }

    @Override
    protected void onStart(){
        Log.i(TAG, "Activity starting up.");
        super.onStart();
        new Thread(){
            @Override
            public void run(){
                try {
                    createJmDNS();
                    registerOurService();
                    startDiscovery();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void createJmDNS() throws IOException {
        inetAddressOfThisDevice = HelperMethods.getWifiIpAddress(MainActivity.this);
        Log.i(TAG, "Device IP: "+ inetAddressOfThisDevice.getHostAddress());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewDeviceIp.setText(inetAddressOfThisDevice.getHostAddress());
            }
        });
        Log.i(TAG, "Creating jmDNS.");
        jmdns = JmDNS.create(inetAddressOfThisDevice);
    }

    private void createServerForIncomingMessages() throws IOException {
        Log.i(TAG, "Creating MsgServer.");
        port = new MsgServer(MainActivity.this).getPort();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewLocalPort.setText(String.format(Locale.US,"%d",port));
            }
        });
    }

    private void startDiscovery(){
        Log.i(TAG, "Starting discovery.");
        serviceListener = new CustomServiceListener(this);
        if (null == jmdns){
            Log.e(TAG, "jmdns is null");
            return;
        }
        jmdns.addServiceListener(SERVICE_TYPE, serviceListener);
    }

    private void registerOurService() throws IOException {
        Log.i(TAG, "Registering our own service.");
        serviceName = SERVICE_NAME_DEFAULT + HelperMethods.getNRandomDigits(5);
        final ServiceInfo serviceInfo = ServiceInfo.create(SERVICE_TYPE, serviceName, port, payloadOnOurService);
        jmdns.registerService(serviceInfo);
        serviceName = serviceInfo.getName();
        String serviceIsRegisteredNotification = "Registered service. Name ended up being: "+serviceName;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String text = HelperMethods.getNameAndTypeString(serviceInfo)
                        + HelperMethods.getPayloadString(serviceInfo);
                textViewOwnService.setText(text);
            }
        });
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
            servicesFound.put(serviceStub, event);
            updateListView();
        }
    }

    protected void removeItemFromList(final ServiceEvent event){
        synchronized (servicesFoundLock) {
            ServiceStub serviceStub = new ServiceStub(event);
            servicesFound.remove(serviceStub);
            updateListView();
        }
    }

    private void updateListView(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (servicesFoundLock) {
                        listAdapter.clear();
                        servicesFoundArrList.clear();
                        for (ServiceEvent serviceEvent : servicesFound.values()) {
                            listAdapter.add(HelperMethods.getDetailedString(serviceEvent));
                            servicesFoundArrList.add(serviceEvent);
                        }
                        listAdapter.notifyDataSetChanged();
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

}
