package uk.ac.cam.cl.lm649.bonjourtesting;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.net.InetAddress;

public class CustomDiscoveryListener implements NsdManager.DiscoveryListener{

    private static final String TAG = "CustomDiscoveryListener";
    private MainActivity mainActivity;

    protected CustomDiscoveryListener(MainActivity mainActivity){
        this.mainActivity = mainActivity;
    }

    @Override
    public void onStartDiscoveryFailed(String serviceType, int errorCode) {
        Log.e(TAG, "onStartDiscoveryFailed() "+serviceType+", "+errorCode);
        mainActivity.displayMsgToUser("onStartDiscoveryFailed");
    }

    @Override
    public void onStopDiscoveryFailed(String serviceType, int errorCode) {
        Log.e(TAG, "onStopDiscoveryFailed() "+serviceType+", "+errorCode);
        mainActivity.displayMsgToUser("onStopDiscoveryFailed");
    }

    @Override
    public void onDiscoveryStarted(String serviceType){
        Log.d(TAG, ">>-------------- onDiscoveryStarted()"+serviceType);
    }

    @Override
    public void onDiscoveryStopped(String serviceType) {
        Log.d(TAG, ">>-------------- onDiscoveryStopped()"+serviceType);
    }

    @Override
    public void onServiceFound(NsdServiceInfo serviceInfo) {
        Log.d(TAG, "onServiceFound()");
        Log.d(TAG, getNameAndTypeStringFromServiceInfo(serviceInfo));
        Log.d(TAG, getHostAndPortStringFromServiceInfo(serviceInfo));
        mainActivity.addItemToList(getNameAndTypeStringFromServiceInfo(serviceInfo));
    }

    @Override
    public void onServiceLost(NsdServiceInfo serviceInfo) {
        Log.d(TAG, "onServiceLost()");
        Log.d(TAG, getNameAndTypeStringFromServiceInfo(serviceInfo));
        Log.d(TAG, getHostAndPortStringFromServiceInfo(serviceInfo));
        mainActivity.removeItemFromList(getNameAndTypeStringFromServiceInfo(serviceInfo));
    }

    public static String getNameAndTypeStringFromServiceInfo(NsdServiceInfo serviceInfo){
        String sname = serviceInfo.getServiceName();
        String stype = serviceInfo.getServiceType();
        return "name: "+sname+", type: "+stype;
    }

    public static String getHostAndPortStringFromServiceInfo(NsdServiceInfo serviceInfo){
        InetAddress host = serviceInfo.getHost();
        String address = null==host ? "null" : host.getHostAddress();
        int port = serviceInfo.getPort();
        return "host: "+address+", port: "+port;
    }

}
