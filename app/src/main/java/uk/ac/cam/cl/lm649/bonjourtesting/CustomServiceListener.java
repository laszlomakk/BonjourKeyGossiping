package uk.ac.cam.cl.lm649.bonjourtesting;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

public class CustomServiceListener implements ServiceListener {

    private static final String TAG = "CustomServiceListener";
    private MainActivity mainActivity;
    private static final long SERVICE_RESOLUTION_TIMEOUT_MSEC = 8000;

    protected CustomServiceListener(MainActivity mainActivity){
        this.mainActivity = mainActivity;
    }

    @Override
    public void serviceAdded(ServiceEvent event) {
        if (event.getName().equals(mainActivity.getServiceName())){
            Log.d(TAG, "Discovered our own service: " + event.getInfo());
            return;
        }
        Log.d(TAG, "Service added: " + event.getInfo());
        if (null == mainActivity.jmdns){
            Log.e(TAG, "jmDNS is null");
            return;
        }
        mainActivity.addItemToList(event);
        mainActivity.jmdns.requestServiceInfo(
                event.getType(), event.getName(), SERVICE_RESOLUTION_TIMEOUT_MSEC);
    }

    @Override
    public void serviceRemoved(ServiceEvent event) {
        Log.d(TAG, "Service removed: " + event.getInfo());
        mainActivity.removeItemFromList(event);
    }

    @Override
    public void serviceResolved(ServiceEvent event) {
        if (event.getName().equals(mainActivity.getServiceName())){
            Log.d(TAG, "Tried to resolve our own service: " + event.getInfo());
            return;
        }
        Log.d(TAG, "Service resolved: " + event.getInfo());
        mainActivity.addItemToList(event);
    }

}
