package uk.ac.cam.cl.lm649.bonjourtesting;

import android.util.Log;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

public class CustomServiceListener implements ServiceListener {

    private static final String TAG = "CustomServiceListener";
    private MainActivity mainActivity;

    protected CustomServiceListener(MainActivity mainActivity){
        this.mainActivity = mainActivity;
    }

    @Override
    public void serviceAdded(ServiceEvent event) {
        Log.d(TAG, "Service added: " + event.getInfo());
        mainActivity.addItemToList(String.format("name: %s, type: %s", event.getName(), event.getType()));
    }

    @Override
    public void serviceRemoved(ServiceEvent event) {
        Log.d(TAG, "Service removed: " + event.getInfo());
        mainActivity.removeItemFromList(String.format("name: %s, type: %s", event.getName(), event.getType()));
    }

    @Override
    public void serviceResolved(ServiceEvent event) {
        Log.d(TAG, "Service resolved: " + event.getInfo());
    }

}
