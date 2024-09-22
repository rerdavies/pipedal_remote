package com.twoplay.pipedal.model;

import android.net.Network;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PiPedalConnection implements Cloneable {

    public PiPedalConnection()
    {
        id_ = ++nextId;
    }
    static private long nextId  = 0;
    private String displayName;
    private String instanceId;
    private String bestConnection;


    public static class ServiceLocation {
        public ServiceLocation(String address) {
            this.address = address;
            this.network = "";
        }

        public String address;
        public String network;
    };
    private  ArrayList<ServiceLocation> serviceLocations = new ArrayList<>();
    private ConnectionStatus connectionStatus = ConnectionStatus.AvailableOnLocalNetwork;
    private long id_;

    public PiPedalConnection(WifiManager wifiManager, NsdServiceInfo serviceInfo) {
        this();
        displayName = serviceInfo.getServiceName();
        instanceId = PiPedalConnection.parseInstanceId(serviceInfo);
        addServiceInfo(serviceInfo);
    }

    void addServiceAddress(InetAddress address,NsdServiceInfo serviceInfo)
    {
        String serviceAddress = "http://" + address.getHostAddress() + ":" + serviceInfo.getPort();
        boolean found = false;
        for (ServiceLocation serviceLocation: serviceLocations)
        {
            if (!serviceLocation.address.equals(serviceAddress))
            {
                found = true;
            }
        }
        if (!found)
        {
            serviceLocations.add(new ServiceLocation(serviceAddress));
        }

    }
    void addServiceInfo(NsdServiceInfo serviceInfo)
    {

        addServiceAddress(serviceInfo.getHost(),serviceInfo);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            for (InetAddress address: serviceInfo.getHostAddresses())
            {
                if (address instanceof Inet4Address)
                {
                    addServiceAddress(address,serviceInfo);
                }
            }
        }
    }

    public static String parseInstanceId(NsdServiceInfo serviceInfo) {
        byte[] idbytes = serviceInfo.getAttributes().get("id");
        if (idbytes == null)
        {
            return "";
        }
        String result = new String(idbytes);
        return result;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getBestConnection() {
        return serviceLocations.size() == 0 ? "": serviceLocations.get(0).address;
    }

    public List<ServiceLocation> getServiceLocations() {
        return serviceLocations;
    }

    public ConnectionStatus getStatus() {
        return connectionStatus;
    }

    public boolean isSameDevice(PiPedalConnection newDevice) {
        return instanceId == newDevice.instanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PiPedalConnection)) return false;
        PiPedalConnection that = (PiPedalConnection) o;
        return id_ == that.id_
                && Objects.equals(displayName, that.displayName)
                && Objects.equals(instanceId, that.instanceId)
                && Objects.equals(bestConnection, that.bestConnection)
                && connectionStatus == that.connectionStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(displayName, instanceId, bestConnection,  connectionStatus, id_);
    }

    public long id() {
        return id_;
    }
}
