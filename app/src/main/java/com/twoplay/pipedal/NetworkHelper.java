package com.twoplay.pipedal;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Copyright (c) 2015, sRobin Davies
 * Created by Robin on 14/04/2022.
 */
public class NetworkHelper {

    private static final String TAG = "DetailedIPAddr";
    public static NetworkInterface getMatchingInterface(InetAddress address)
    {
        boolean isv4 = isIpV4(address);
        byte[] addressBuf = address.getAddress();


        Enumeration<NetworkInterface> i = null;
        try {
            i = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            return null;
        }

        NetworkInterface bestInterface = null;


        while (i.hasMoreElements())
        {
            NetworkInterface iff = i.nextElement();

            boolean matches = false;
            for(InterfaceAddress ifAddr:  iff.getInterfaceAddresses())
            {
                if (addressMatches(ifAddr,addressBuf))
                {
                    matches = true;
                    break;
                }
            }
            if (matches)
            {
                return iff;
            }
        }
        return null;
    }
    public static boolean isIpV4(InetAddress address)
    {
        return (address.getClass().isAssignableFrom(Inet4Address.class));
    }
    public static boolean isIpV6(InetAddress address)
    {
        return (address.getClass().isAssignableFrom(Inet6Address.class));
    }

    private static boolean addressMatches(InterfaceAddress ifAddr, byte[] address) {
        int len = ifAddr.getNetworkPrefixLength();
        byte[] ifBuf = ifAddr.getAddress().getAddress();
        if (ifBuf.length != address.length) return false;

        int ix = 0;
        while (len > 8)
        {
            if (ifBuf[ix] != address[ix]) return false;
            ++ix;
            len -= 8;
        }
        if (len > 0)
        {
            int mask = (0xFF << (8-len)) & 0x00FF;
            if( (ifBuf[ix] & mask) != (address[ix] & mask) ) return false;
        }
        return true;
    }
}
