/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
 */

package uk.ac.cam.cl.lm649.bonjourtesting.bonjour;

import java.io.IOException;
import java.util.UUID;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.BadgeStatus;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.JPAKEClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgServer;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes.Message;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes.MsgThisIsMyIdentity;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes.MsgWhoAreYouQuestion;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
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
        MsgServer.getInstance().serviceToMsgClientMap.remove(new ServiceStub(event));
    }

    @Override
    public void serviceResolved(ServiceEvent event) {
        if (event.getName().equals(bonjourService.getNameOfOurService())){
            FLogger.i(TAG, "Tried to resolve our own service: " + event.getInfo());
            return;
        }
        FLogger.i(TAG, "Service resolved: " + event.getInfo());

        String badgeIdOfOtherDevice = event.getInfo().getPropertyString(BonjourService.DNS_TXT_RECORD_MAP_KEY_FOR_BADGE_ID);
        FLogger.i(TAG, "serviceResolved(). service claims to have badgeID: " + badgeIdOfOtherDevice);

        bonjourService.addServiceToRegistry(event);

        MsgClient msgClient = getMsgClientForService(event, badgeIdOfOtherDevice);
        startMessaging(msgClient);
    }

    private MsgClient getMsgClientForService(ServiceEvent event, String badgeIdOfOtherDevice) {
        ServiceStub serviceStub = new ServiceStub(event);
        MsgClient msgClient = MsgServer.getInstance().serviceToMsgClientMap.get(serviceStub);
        if (null == msgClient || msgClient.isClosed()) {
            FLogger.d(TAG, "getMsgClientForService(). ++ creating new MsgClient for " + serviceStub);
            msgClient = new MsgClient(event.getInfo());
            if (null != badgeIdOfOtherDevice) {
                msgClient.reconfirmBadgeId(UUID.fromString(badgeIdOfOtherDevice));
            } else {
                FLogger.w(TAG, "getMsgClientForService(). badgeIdOfOtherDevice is null.");
            }
            MsgServer.getInstance().serviceToMsgClientMap.put(serviceStub, msgClient);
        } else {
            FLogger.d(TAG, "getMsgClientForService(). __ reusing MsgClient for " + serviceStub);
        }
        return msgClient;
    }

    public boolean getDiscoveredOurOwnService() {
        return discoveredOurOwnService;
    }

    private void startMessaging(MsgClient msgClient) {
        Message msgWhoAreYouQuestion = new MsgWhoAreYouQuestion();
        msgClient.sendMessage(msgWhoAreYouQuestion);

        Message msgThisIsMyId = new MsgThisIsMyIdentity(BadgeStatus.constructMyCurrentBadgeStatus());
        msgClient.sendMessage(msgThisIsMyId);

        if (JPAKEClient.shouldWeRunJPAKE(msgClient)) {
            try {
                JPAKEClient jpakeClient = msgClient.getJpakeClient();
                if (null == jpakeClient) {
                    FLogger.e(TAG, "startMessaging(). jpakeClient is null BUT IT SHOULDN'T BE NULL HERE !");
                    return;
                }
                jpakeClient.round1Send(msgClient);
            } catch (IOException e) {
                FLogger.e(TAG, "startMessaging() - JPAKEClient.round1Send(). IOE - " + e.getMessage());
            }
        }
    }

}
