package com.twoplay.pipedal.model;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import com.twoplay.pipedal.NetworkHelper;
import com.twoplay.pipedal.PiPedalApplication;
import com.twoplay.pipedal.R;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * Copyright (c) 2015, sRobin Davies
 * Created by Robin on 14/04/2022.
 */
public class DetailedIpAddress
{


    public String bssid;

    public DetailedIpAddress(Context context, InetAddress address, int port) {
        this.address = address;
        this.port = port;
        getNetworkDetails(context,address);
    }

    private static Context getContext() {
        return PiPedalApplication.getContext();
    }
    private static String getString(int rid)
    {
        return PiPedalApplication.getContext().getString(rid);
    }

    private void getNetworkDetails(Context context,InetAddress address)
    {

        try {
            NetworkInterface networkInterface = NetworkHelper.getMatchingInterface(address);
            if (networkInterface == null) {

                networkName =  context.getString(R.string.wifi_direct_connection); // i.e. from a connection-to-be.
                networkType = ConnectionType.WifiDirect;

            } else {
                networkName = networkInterface.getName();
                if (networkName.startsWith("lo")) {
                    networkType = ConnectionType.Loopback;
                     networkName = getString(R.string.loopback_network_name);
                } else if (networkName.startsWith("eth")) {
                    networkName = getString(R.string.ethernet_network_name);
                    networkType = ConnectionType.Ethernet;
                } else if (networkName.startsWith("p2p-"))
                {
                    networkName = getString(R.string.wifi_direct_connection);
                    networkType = ConnectionType.WifiDirect;
                } else if (networkName.startsWith("wlan")) {
                    WifiManager wifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
                    WifiInfo info = wifiManager.getConnectionInfo();
                    networkName = getString(R.string.wifi_network_name);
//                    if (info != null) {
//                        String ssid = info.getSSID();
//                        if (ssid.length() != 0) {
//                            networkName = ssid;
//                        }
//                    }
                    networkType = ConnectionType.Wifi;
                } else {
                    networkType = ConnectionType.WifiDirect;
                    networkName = getString(R.string.wifi_direct_connection);

                    bssid = bsidToString(networkInterface.getHardwareAddress());

                }
            }
        } catch (Exception e)
        {
            networkName = context.getString(R.string.default_network_name);
            networkType= ConnectionType.Unknown;
        }
        if (address.isLoopbackAddress())
        {
            networkType = ConnectionType.Loopback;
        }
        sortPriority = (networkType.ordinal())*2;
        // prefer ipv4 to ipv6.
        if (!address.getClass().isAssignableFrom(Inet4Address.class ))
        {
            ++sortPriority;
        }
    }

    private String bsidToString(byte[] hardwareAddress) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hardwareAddress.length; ++i)
        {
            if (i != 0)
            {
                sb.append(":");
            }
            sb.append(String.format("%02x",hardwareAddress[i]));
        }
        return sb.toString();
    }

    @Override
    public String toString() {

        if (NetworkHelper.isIpV6(address))
        {
            String a = address.toString();
            if (a.startsWith("/")) a = a.substring(1);
            return "http://[" + a + "]:" + port; // [::01]:port  (i.e. http format.

        } else if (NetworkHelper.isIpV4(address))
        {
            String a = address.toString();
            if (a.startsWith("/")) a = a.substring(1);
            return "http://" + a + ":" + port;
        } else {
            throw new RuntimeException("Invalid ip address.");
        }
    }

    public DetailedIpAddress(Context context, String serviceAddress) {
        int nPos = serviceAddress.lastIndexOf(':');
        String strPort = serviceAddress.substring(nPos+1);
        int start = 0;
        while (start < serviceAddress.length() && serviceAddress.charAt(start) == '/')
        {
            ++start;
        }
        String addrPart = serviceAddress.substring(start,nPos);

        try {
            this.address = InetAddress.getByName(addrPart);
        } catch (UnknownHostException ignored)
        {
            throw new RuntimeException("Fatal error.");
        }
        this.port = Integer.parseInt(strPort);

        getNetworkDetails(context,address);
    }

    public ConnectionType networkType;

    public int sortPriority = 0;
    public String networkName;
    public InetAddress address;
    public int port;

}
