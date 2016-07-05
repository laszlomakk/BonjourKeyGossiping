package uk.ac.cam.cl.lm649.bonjourtesting;

import android.util.Log;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class CustomServiceListener implements ServiceListener {

    private static final String TAG = "CustomServiceListener";
    private MainActivity mainActivity;

    protected CustomServiceListener(MainActivity mainActivity){
        this.mainActivity = mainActivity;
    }

    @Override
    public void serviceAdded(ServiceEvent event) {
        Log.d(TAG, "Service added: " + event.getInfo());
        if (null == mainActivity.jmdns){
            Log.e(TAG, "jmDNS is null");
            return;
        }
        mainActivity.jmdns.requestServiceInfo(event.getType(), event.getName(), 8000);
    }

    @Override
    public void serviceRemoved(ServiceEvent event) {
        Log.d(TAG, "Service removed: " + event.getInfo());
    }

    @Override
    public void serviceResolved(ServiceEvent event) {
        Log.d(TAG, "Service resolved: " + event.getInfo());
        mainActivity.addItemToList(HelperMethods.getDetailedString(event));
    }

}
