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

    public static final String SERVICE_TYPE = "_verysecretstuff._tcp.local."; // _http._tcp.local.
    public static final String SERVICE_NAME_DEFAULT = "client_";
    private String serviceName = "";
    private int port = 51411;

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
    protected InetAddress inetAddressOfThisDevice;

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

    private void resetUI(){
        textViewDeviceIp.setText("");
        textViewOwnService.setText("");
        listAdapter.clear();
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
                    createServerForIncomingMessages();
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
        new MsgServer(MainActivity.this, inetAddressOfThisDevice, port);
    }

    private void startDiscovery(){
        Log.i(TAG, "Starting discovery.");
        jmdns.addServiceListener(SERVICE_TYPE, new CustomServiceListener(MainActivity.this));
    }

    private void registerOurService() throws IOException {
        Log.i(TAG, "Registering our own service.");
        serviceName = SERVICE_NAME_DEFAULT + HelperMethods.getNRandomDigits(5);
        final ServiceInfo serviceInfo = ServiceInfo.create(SERVICE_TYPE, serviceName, port, "");
        jmdns.registerService(serviceInfo);
        serviceName = serviceInfo.getName();
        String serviceIsRegisteredNotification = "Registered service. Name ended up being: "+serviceName;
        runOnUiThread(new Runnable() {
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
        resetUI();
    }

    protected void displayMsgToUser(final String msg){
        runOnUiThread(new Runnable() {
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

    private synchronized void updateListView(){
        runOnUiThread(new Runnable() {
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
