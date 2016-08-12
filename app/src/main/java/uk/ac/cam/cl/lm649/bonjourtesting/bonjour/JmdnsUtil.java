package uk.ac.cam.cl.lm649.bonjourtesting.bonjour;

import android.support.annotation.Nullable;

import java.net.InetAddress;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;

import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public class JmdnsUtil {

    private static final String TAG = "JmdnsUtil";

    private JmdnsUtil() {}

    public static String getDetailedString(ServiceEvent event){
        return getNameAndTypeString(event)
                +getAddressesAndPortString(event)
                +"\n"+getPayloadString(event);
    }

    public static String getDetailedString(ServiceInfo info){
        return getNameAndTypeString(info)
                +getAddressesAndPortString(info)
                +"\n"+getPayloadString(info);
    }

    public static String getAddressesAndPortString(ServiceEvent event){
        return getAddressesAndPortString(event.getInfo());
    }

    public static String getAddressesAndPortString(ServiceInfo info){
        StringBuffer sb = new StringBuffer();
        if (info != null){
            String[] addresses = info.getHostAddresses();
            if (addresses != null){
                sb.append("addresses: ");
                for (String addr : addresses){
                    sb.append(addr).append(", ");
                }
                sb.append("\n");
            }
            sb.append("port: ").append(info.getPort());
        }
        return sb.toString();
    }

    public static String getPayloadString(ServiceEvent event){
        return getPayloadString(event.getInfo());
    }

    public static String getPayloadString(ServiceInfo info){
        StringBuffer sb = new StringBuffer();
        if (info != null){
            sb.append("payload: ").append(info.getNiceTextString());
        }
        return sb.toString();
    }

    public static String getNameAndTypeString(ServiceEvent event){
        StringBuffer sb = new StringBuffer();
        sb.append("name: ").append(event.getName()).append("\n");
        sb.append("type: ").append(event.getType()).append("\n");
        return sb.toString();
    }

    public static String getNameAndTypeString(ServiceInfo info){
        StringBuffer sb = new StringBuffer();
        sb.append("name: ").append(info.getName()).append("\n");
        sb.append("type: ").append(info.getType()).append("\n");
        return sb.toString();
    }

    @Nullable
    public static InetAddress getAddress(ServiceInfo serviceInfoOfDst) {
        if (null == serviceInfoOfDst){
            FLogger.e(TAG, "getAddress(). serviceInfo is null");
            return null;
        }
        InetAddress[] arrAddresses = serviceInfoOfDst.getInet4Addresses();
        if (null == arrAddresses || arrAddresses.length < 1){
            FLogger.e(TAG, "getAddress(). inappropriate addresses");
            return null;
        }
        return arrAddresses[0];
    }

}
