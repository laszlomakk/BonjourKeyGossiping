/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
 */

package uk.ac.cam.cl.lm649.bonjourtesting.menu;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;

import uk.ac.cam.cl.lm649.bonjourtesting.CustomActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.R;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.SaveBadgeData;
import uk.ac.cam.cl.lm649.bonjourtesting.bonjour.BonjourService;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.JPAKEClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgServerManager;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes.Message;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes.MsgArbitraryText;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes.MsgMyPhoneNumber;
import uk.ac.cam.cl.lm649.bonjourtesting.menu.settings.SaveSettingsData;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;
import uk.ac.cam.cl.lm649.bonjourtesting.util.JmdnsUtil;
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
        listAdapterForDisplayedListOfServices = new ArrayAdapter<>(this, R.layout.row_in_list, new ArrayList<String>());
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
                    String msgTextPayload = "Hy there! I see your payload is " + serviceInfo.getNiceTextString();
                    BonjourService bonjourService = app.getBonjourService();
                    if (null != bonjourService){
                        String serviceName = bonjourService.getNameOfOurService();
                        MsgClient msgClient = MsgServerManager.getInstance().serviceToMsgClientMap.get(new ServiceStub(serviceInfo));
                        if (null == msgClient) {
                            FLogger.e(TAG, "onItemClick(). msgClient not found");
                            HelperMethods.displayMsgToUser(context, "error: msgClient not found");
                        } else {
                            String msgText = String.format(Locale.US, "%s: %s", serviceName, msgTextPayload);
                            Message msg = new MsgArbitraryText(msgText);
                            msgClient.sendMessage(msg);
                        }
                    } else {
                        FLogger.e(TAG, "onItemClick(). bonjourService is null.");
                        HelperMethods.displayMsgToUser(context, "error: bonjourService is null");
                    }
                }
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                synchronized (displayedServicesLock){
                    FLogger.i(TAG, "onItemLongClick() - user clicked on an item in the list");
                    ServiceInfo serviceInfo = servicesFoundArrList.get(position).getInfo();
                    if (null == serviceInfo){
                        FLogger.e(TAG, "onItemLongClick(). error sending msg: serviceInfo is null");
                        HelperMethods.displayMsgToUser(context, "error sending msg: serviceInfo is null (1)");
                        return true;
                    }
                    MsgClient msgClient = MsgServerManager.getInstance().serviceToMsgClientMap.get(new ServiceStub(serviceInfo));
                    if (null == msgClient) {
                        FLogger.e(TAG, "onItemLongClick(). msgClient not found");
                        HelperMethods.displayMsgToUser(context, "error: msgClient not found");
                    } else {
                        String sharedSecret = JPAKEClient.determineSharedSecret();
                        boolean jpakeStarted = JPAKEClient.startJPAKEifAppropriate(msgClient, sharedSecret);
                        FLogger.i(TAG, "onItemLongClick(). jpakeStarted: " + jpakeStarted);
                        HelperMethods.displayMsgToUser(context, "jpakeStarted: " + jpakeStarted);
                    }
                    return true;
                }
            }
        });

        // top area
        textViewAppState = (TextView) findViewById(R.id.appState);
        textViewDeviceIp = (TextView) findViewById(R.id.deviceIp);
        textViewLocalPort = (TextView) findViewById(R.id.localPort);
        textViewOwnService = (TextView) findViewById(R.id.ownService);
        textViewNumServicesFound = (TextView) findViewById(R.id.numberOfServicesFound);

        // announce button
        findViewById(R.id.announceButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FLogger.i(TAG, "user clicked announce button");
                SaveBadgeData saveBadgeData = SaveBadgeData.getInstance(context);
                SaveSettingsData saveSettingsData = SaveSettingsData.getInstance(context);
                for (Map.Entry<ServiceStub, MsgClient> entry : MsgServerManager.getInstance().serviceToMsgClientMap.entrySet()) {
                    Message msg = new MsgMyPhoneNumber(
                            saveBadgeData.getMyBadgeCustomName(),
                            saveSettingsData.getPhoneNumber());
                    MsgClient msgClient = entry.getValue();
                    msgClient.sendMessage(msg);
                    ServiceStub serviceStub = entry.getKey();
                    FLogger.i(TAG, String.format(Locale.US, "sent %s to service %s (IP: %s)",
                            msg.getClass().getSimpleName(), serviceStub.name, msgClient.strSocketAddress));
                }
                HelperMethods.displayMsgToUser(context, "announced phone number");
            }
        });

        // settings button
        findViewById(R.id.settingsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FLogger.i(TAG, "user clicked settings button");
                startActivity(new Intent("uk.ac.cam.cl.lm649.bonjourtesting.SETTINGS"));
            }
        });

        // refresh button
        findViewById(R.id.refreshButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FLogger.i(TAG, "user clicked refresh button");
                refreshTopUI();
                updateListView();
                BonjourService bonjourService = app.getBonjourService();
                if (null != bonjourService) bonjourService.restartDiscovery();
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
        BonjourService bonjourService = app.getBonjourService();

        String appStateText = "-";
        if (null != bonjourService) appStateText = bonjourService.getStrServiceState();
        textViewAppState.setText(appStateText);

        String deviceIP = "999.999.999.999";
        if (null != bonjourService) deviceIP = bonjourService.getIPAddress();
        textViewDeviceIp.setText(deviceIP);

        String port = "" + MsgServerManager.getInstance().getMsgServerPlaintext().getPort();
        textViewLocalPort.setText(port);

        String ownServiceText = "-";
        if (null != bonjourService && null != bonjourService.getServiceInfoOfOurService()) {
            ServiceInfo serviceInfo = bonjourService.getServiceInfoOfOurService();
            ownServiceText = JmdnsUtil.getNameAndTypeString(serviceInfo)
                    + JmdnsUtil.getPayloadString(serviceInfo);
        }
        textViewOwnService.setText(ownServiceText);

        String numServicesText = "-";
        if (null != bonjourService) numServicesText = "" + bonjourService.getServiceRegistry().size();
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
        super.onStart();
        updateListView();
    }

    @Override
    protected void onStop(){
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
                            listAdapterForDisplayedListOfServices.add(JmdnsUtil.getDetailedString(serviceEvent));
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
        BonjourService bonjourService = app.getBonjourService();
        if (null != bonjourService) {
            updateListView(bonjourService.getServiceRegistry());
        }
    }

    @Override
    public void forceRefreshUI() {
        updateListView();
    }

}
