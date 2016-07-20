/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
 */

package uk.ac.cam.cl.lm649.bonjourtesting;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;
import java.util.TreeMap;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;

import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgServer;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;
import uk.ac.cam.cl.lm649.bonjourtesting.util.ServiceStub;

public class BonjourDebugActivity extends CustomActivity {

    private static final String TAG = "BonjourDebugActivity";

    private ArrayAdapter<String> listAdapterForDisplayedListOfServices;
    private ArrayList<ServiceEvent> servicesFoundArrList = new ArrayList<>();
    private final Object displayedServicesLock = new Object();

    private TextView textViewAppState;
    private TextView textViewDeviceIp;
    private TextView textViewLocalPort;
    private TextView textViewOwnService;
    private TextView textViewNumServicesFound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupUI();
    }

    private void setupUI(){
        setContentView(R.layout.bonjour_debug_view);

        // listView for displaying the services we find on the network
        ListView listView = (ListView) findViewById(R.id.mainListView);
        listAdapterForDisplayedListOfServices = new ArrayAdapter<>(this, R.layout.bonjour_debug_row_in_list, new ArrayList<String>());
        listView.setAdapter(listAdapterForDisplayedListOfServices);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                synchronized (displayedServicesLock){
                    FLogger.i(TAG, "onItemClick() - user clicked on an item in the list");
                    ServiceInfo serviceInfo = servicesFoundArrList.get(position).getInfo();
                    if (null == serviceInfo){
                        FLogger.e(TAG, "onItemClick(). error sending msg: serviceInfo is null");
                        HelperMethods.displayMsgToUser(context, "error sending msg: serviceInfo is null (1)");
                        return;
                    }
                    String msg = "Hy there! I see your payload is " + serviceInfo.getNiceTextString();
                    if (app.isBonjourServiceBound()){
                        String serviceName = app.getBonjourService().getNameOfOurService();
                        MsgClient msgClient = MsgServer.getInstance().serviceToMsgClientMap.get(new ServiceStub(serviceInfo));
                        if (null == msgClient) {
                            HelperMethods.displayMsgToUser(context, "error: msgClient not found");
                        } else {
                            msgClient.sendMessageArbitraryText(serviceName, msg);
                        }
                    } else {
                        HelperMethods.displayMsgToUser(context, "error: bonjourService not bound");
                    }
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
                refreshTopUI();
                updateListView();
                if (app.isBonjourServiceBound()) app.getBonjourService().restartDiscovery();
            }
        });

        refreshTopUI();
    }

    public void refreshTopUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refreshTopUIInternal();
            }
        });
    }

    private void refreshTopUIInternal() {
        String appStateText = "-";
        if (app.isBonjourServiceBound()) appStateText = app.getBonjourService().getStrServiceState();
        textViewAppState.setText(appStateText);

        String deviceIP = "999.999.999.999";
        if (app.isBonjourServiceBound()) deviceIP = app.getBonjourService().getIPAddress();
        textViewDeviceIp.setText(deviceIP);

        textViewLocalPort.setText(String.format(Locale.US,"%d",MsgServer.getInstance().getPort()));

        String ownServiceText = "-";
        if (app.isBonjourServiceBound() && null != app.getBonjourService().getServiceInfoOfOurService()) {
            ServiceInfo serviceInfo = app.getBonjourService().getServiceInfoOfOurService();
            ownServiceText = HelperMethods.getNameAndTypeString(serviceInfo)
                    + HelperMethods.getPayloadString(serviceInfo);
        }
        textViewOwnService.setText(ownServiceText);

        String numServicesText = "-";
        if (app.isBonjourServiceBound()) numServicesText = "" + app.getBonjourService().getServiceRegistry().size();
        textViewNumServicesFound.setText(numServicesText);
    }

    private void resetUI(){
        FLogger.i(TAG, "Resetting UI.");
        refreshTopUI();
        resetListOfDisplayedServices();
    }

    private void resetListOfDisplayedServices() {
        synchronized (displayedServicesLock) {
            servicesFoundArrList.clear();
            listAdapterForDisplayedListOfServices.clear();
            listAdapterForDisplayedListOfServices.notifyDataSetChanged();
        }
    }

    @Override
    protected void onStart(){
        FLogger.i(TAG, "Activity starting up.");
        super.onStart();
        updateListView();
    }

    @Override
    protected void onStop(){
        FLogger.i(TAG, "Activity stopping.");
        super.onStop();
        resetUI();
    }

    public void updateListView(final TreeMap<ServiceStub, ServiceEvent> serviceRegistry) {
        FLogger.v(TAG, "updateListView(TreeMap) called.");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                synchronized (serviceRegistry) {
                    synchronized (displayedServicesLock) {
                        FLogger.v(TAG, "updateListView() doing actual update.");
                        refreshTopUIInternal();
                        listAdapterForDisplayedListOfServices.clear();
                        servicesFoundArrList.clear();
                        for (ServiceEvent serviceEvent : serviceRegistry.values()) {
                            listAdapterForDisplayedListOfServices.add(HelperMethods.getDetailedString(serviceEvent));
                            servicesFoundArrList.add(serviceEvent);
                        }
                        listAdapterForDisplayedListOfServices.notifyDataSetChanged();
                    }
                }
            }
        });
    }

    private void updateListView() {
        FLogger.v(TAG, "updateListView() called.");
        if (app.isBonjourServiceBound()) {
            updateListView(app.getBonjourService().getServiceRegistry());
        }
    }

}
