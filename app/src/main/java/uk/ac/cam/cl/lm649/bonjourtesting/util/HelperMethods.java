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

    public static String trimStringFromDots(String str){
        if (null == str) return null;
        while (str.startsWith(".")){
            str = str.substring(1);
        }
        while (str.endsWith(".")){
            str = str.substring(0,str.length()-1);
        }
        return str;
    }

    public static boolean equalsTrimmedFromDots(String s1, String s2){
        if (null == s1 && null == s2){
            return true;
        } else if (null == s1 || null == s2){
            return false;
        }
        s1 = trimStringFromDots(s1);
        s2 = trimStringFromDots(s2);
        return s1.equals(s2);
    }

}
