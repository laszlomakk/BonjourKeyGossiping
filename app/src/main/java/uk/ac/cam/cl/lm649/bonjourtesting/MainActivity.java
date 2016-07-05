/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
 */

package uk.ac.cam.cl.lm649.bonjourtesting;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.TreeMap;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;

import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class MainActivity extends Activity {

    public static final String SERVICE_TYPE = "_verysecretstuff._udp.local."; // _http._tcp.local.
    public static final String SERVICE_NAME_DEFAULT = "cool_name";
    private String serviceName = "";

    protected View rootView;

    private static final String TAG = "MainActivity";
    private Context context;
    private ListView listView;
    private ArrayAdapter<String> listAdapter;
    private TreeMap<String, String> listElements = new TreeMap<>(); //Key is ID, Value is what to display to user
    private TextView textViewDeviceIp;
    private TextView textViewOwnService;
    private TextView textViewAppNotFrozen;

    protected JmDNS jmdns;
    protected InetAddress inetAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        setupUI();
        refreshAppIsNotFrozen();
    }

    private void setupUI(){
        LayoutInflater li = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rootView = li.inflate(R.layout.activity_main, null);
        setContentView(rootView);

        //listView
        listView = (ListView) findViewById(R.id.mainListView);
        listAdapter = new ArrayAdapter<>(this, R.layout.row_in_list, new ArrayList<String>());
        listView.setAdapter(listAdapter);

        //top area
        textViewDeviceIp = (TextView) findViewById(R.id.deviceIp);
        textViewOwnService = (TextView) findViewById(R.id.ownService);
        textViewAppNotFrozen = (TextView) findViewById(R.id.appNotFrozen);
        textViewAppNotFrozen.setText("\\");
    }

    private void refreshAppIsNotFrozen(){
        rootView.postDelayed(new Runnable() {
            @Override
            public void run() {
                String curSymbol = (String) textViewAppNotFrozen.getText();
                String nextSymbol = "\\";
                switch (curSymbol){
                    case "\\":
                        nextSymbol = "|";
                        break;
                    case "|":
                        nextSymbol = "/";
                        break;
                    case "/":
                        nextSymbol = "-";
                        break;
                }
                textViewAppNotFrozen.setText(nextSymbol);
                refreshAppIsNotFrozen();
            }
        }, 2000);
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
        inetAddress = HelperMethods.getWifiIpAddress(MainActivity.this);
        Log.i(TAG, "Device IP: "+inetAddress.getHostAddress());
        rootView.post(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "ping");
                textViewDeviceIp.setText(inetAddress.getHostAddress());
                Log.e(TAG, "pong");
            }
        });
        Log.i(TAG, "Creating jmDNS. Starting discovery...");
        jmdns = JmDNS.create(inetAddress);
    }

    private void startDiscovery(){
        jmdns.addServiceListener(SERVICE_TYPE, new CustomServiceListener(MainActivity.this));
    }

    private void registerOurService() throws IOException {
        final ServiceInfo serviceInfo = ServiceInfo.create(SERVICE_TYPE, SERVICE_NAME_DEFAULT, 57126, "");
        jmdns.registerService(serviceInfo);
        serviceName = serviceInfo.getName();
        String serviceIsRegisteredNotification = "Registered service. Name ended up being: "+serviceName;
        rootView.post(new Runnable() {
            @Override
            public void run() {
                textViewOwnService.setText(HelperMethods.getNameAndTypeString(serviceInfo));
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
        listAdapter.clear();
    }

    protected void displayMsgToUser(final String msg){
        rootView.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    protected synchronized void addItemToList(final ServiceEvent event, boolean serviceIsResolved){
        String displayToUser = "";
        if (serviceIsResolved) displayToUser += "* ";
        displayToUser += HelperMethods.getDetailedString(event);
        listElements.put(
                HelperMethods.getNameAndTypeString(event),
                displayToUser
        );
        updateListView();
    }

    protected synchronized void removeItemFromList(final ServiceEvent event){
        listElements.remove(HelperMethods.getNameAndTypeString(event));
        updateListView();
    }

    private void updateListView(){
        rootView.post(new Runnable() {
            @Override
            public void run() {
                try {
                    listAdapter.clear();
                    listAdapter.addAll(listElements.values());
                    listAdapter.notifyDataSetChanged();
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
