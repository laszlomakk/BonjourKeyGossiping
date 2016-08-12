/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
 */

package uk.ac.cam.cl.lm649.bonjourtesting.bonjour;

import android.support.annotation.Nullable;

import java.net.InetAddress;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import uk.ac.cam.cl.lm649.bonjourtesting.messaging.JPAKEClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgServerManager;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.JmdnsUtil;
import uk.ac.cam.cl.lm649.bonjourtesting.util.ServiceStub;

public class CustomServiceListener implements ServiceListener {

    private static final String TAG = "CustomServiceListener";
    private BonjourService bonjourService;

    private boolean discoveredOurOwnService = false;

    private static final long SERVICE_RESOLUTION_TIMEOUT_MSEC = 8000;

    protected CustomServiceListener(BonjourService bonjourService){
        this.bonjourService = bonjourService;
    }

    @Override
    public void serviceAdded(ServiceEvent event) {
        if (event.getName().equals(bonjourService.getNameOfOurService())){
            FLogger.i(TAG, "Discovered our own service: " + event.getInfo());
            discoveredOurOwnService = true;
            return;
        }
        FLogger.i(TAG, "Service added: " + event.getInfo());
        if (null == bonjourService.jmdns){
            FLogger.e(TAG, "jmDNS is null");
            return;
        }
        bonjourService.addServiceToRegistry(event);
        bonjourService.jmdns.requestServiceInfo(
                event.getType(), event.getName(), SERVICE_RESOLUTION_TIMEOUT_MSEC);
    }

    @Override
    public void serviceRemoved(ServiceEvent event) {
        FLogger.i(TAG, "Service removed: " + event.getInfo());

        bonjourService.removeServiceFromRegistry(event);
        MsgServerManager.getInstance().serviceToMsgClientMap.remove(new ServiceStub(event));
    }

    @Override
    public void serviceResolved(ServiceEvent event) {
        if (event.getName().equals(bonjourService.getNameOfOurService())){
            FLogger.i(TAG, "Tried to resolve our own service: " + event.getInfo());
            return;
        }
        FLogger.i(TAG, "Service resolved: " + event.getInfo());

        bonjourService.addServiceToRegistry(event);

        MsgClient msgClient = getMsgClientForService(event);

        String sharedSecret = JPAKEClient.determineSharedSecret();
        JPAKEClient.startJPAKEifAppropriate(msgClient, sharedSecret);
    }

    @Nullable
    private MsgClient getMsgClientForService(ServiceEvent event) {
        ServiceStub serviceStub = new ServiceStub(event);
        MsgClient msgClient = MsgServerManager.getInstance().serviceToMsgClientMap.get(serviceStub);
        if (null == msgClient || msgClient.isClosed()) {
            FLogger.d(TAG, "getMsgClientForService(). ++ creating new MsgClient for " + serviceStub);
            ServiceInfo serviceInfo = event.getInfo();
            InetAddress address = JmdnsUtil.getAddress(serviceInfo);
            if (null == address) {
                FLogger.e(TAG, "getMsgClientForService(). address is null.");
                return null;
            }
            msgClient = new MsgClient(address, serviceInfo.getPort(), null);
            msgClient.setServiceStubWeAreBoundTo(serviceStub);
            MsgServerManager.getInstance().serviceToMsgClientMap.put(serviceStub, msgClient);
        } else {
            FLogger.d(TAG, "getMsgClientForService(). __ reusing MsgClient for " + serviceStub);
        }
        return msgClient;
    }

    public boolean getDiscoveredOurOwnService() {
        return discoveredOurOwnService;
    }

}
