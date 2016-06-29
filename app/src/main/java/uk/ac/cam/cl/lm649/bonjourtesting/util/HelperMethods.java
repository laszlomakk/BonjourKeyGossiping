/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
 */

package uk.ac.cam.cl.lm649.bonjourtesting.util;

import android.net.nsd.NsdServiceInfo;

import java.net.InetAddress;

public class HelperMethods {

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

    public static String getDetailedStringFromServiceInfo(NsdServiceInfo serviceInfo){
        return getNameAndTypeStringFromServiceInfo(serviceInfo) + "\n "
                + getHostAndPortStringFromServiceInfo(serviceInfo);
    }

}
