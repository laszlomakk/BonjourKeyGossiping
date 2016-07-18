/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
 */

package uk.ac.cam.cl.lm649.bonjourtesting.bonjour;

import android.util.Log;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

import uk.ac.cam.cl.lm649.bonjourtesting.bonjour.BonjourService;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgServer;
import uk.ac.cam.cl.lm649.bonjourtesting.util.ServiceStub;

public class CustomServiceListener implements ServiceListener {

    private static final String TAG = "CustomServiceListener";
    private BonjourService bonjourService;
    private static final long SERVICE_RESOLUTION_TIMEOUT_MSEC = 8000;

    protected CustomServiceListener(BonjourService bonjourService){
        this.bonjourService = bonjourService;
    }

    @Override
    public void serviceAdded(ServiceEvent event) {
        if (event.getName().equals(bonjourService.getNameOfOurService())){
            Log.d(TAG, "Discovered our own service: " + event.getInfo());
            return;
        }
        Log.d(TAG, "Service added: " + event.getInfo());
        if (null == bonjourService.jmdns){
            Log.e(TAG, "jmDNS is null");
            return;
        }
        bonjourService.addServiceToRegistry(event);
        bonjourService.jmdns.requestServiceInfo(
                event.getType(), event.getName(), SERVICE_RESOLUTION_TIMEOUT_MSEC);
    }

    @Override
    public void serviceRemoved(ServiceEvent event) {
        Log.d(TAG, "Service removed: " + event.getInfo());

        bonjourService.removeServiceFromRegistry(event);
        MsgServer.getInstance().serviceToMsgClientMap.remove(new ServiceStub(event));
    }

    @Override
    public void serviceResolved(ServiceEvent event) {
        if (event.getName().equals(bonjourService.getNameOfOurService())){
            Log.d(TAG, "Tried to resolve our own service: " + event.getInfo());
            return;
        }
        Log.d(TAG, "Service resolved: " + event.getInfo());

        bonjourService.addServiceToRegistry(event);
        MsgClient msgClient = new MsgClient(event.getInfo());
        MsgServer.getInstance().serviceToMsgClientMap.put(new ServiceStub(event), msgClient);
        msgClient.sendWhoAreYouMessage();
    }

}
