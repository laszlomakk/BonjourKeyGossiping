package uk.ac.cam.cl.lm649.bonjourtesting.bonjour;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;

public class ServiceStub implements Comparable<ServiceStub> {

    public final String type;
    public final String name;

    public ServiceStub(String type, String name){
        if (null == type || null == name) throw new IllegalArgumentException("type or name is null");
        this.type = type;
        this.name = name;
    }

    public ServiceStub(ServiceEvent serviceEvent){
        this(serviceEvent.getType(), serviceEvent.getName());
    }

    public ServiceStub(ServiceInfo serviceInfo){
        this(serviceInfo.getType(), serviceInfo.getName());
    }

    @Override
    public int compareTo(ServiceStub other){
        String s1 = type+name;
        String s2 = other.type+other.name;
        return s1.compareTo(s2);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ServiceStub) {
            ServiceStub other = (ServiceStub) o;
            return type.equals(other.type) && name.equals(other.name);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (type+name).hashCode();
    }

    @Override
    public String toString() {
        return name + "." + type;
    }

}
