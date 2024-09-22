package com.twoplay.pipedal;

import android.net.wifi.p2p.WifiP2pDevice;

import java.util.Objects;

/**
 * Copyright (c) 2022, Robin Davies
 * Created by Robin on 20/03/2022.
 */
public class WifiEntry {
    public  int status;
    public  boolean isGroupOwner;
    public String deviceName;
    public String deviceAddress;

    WifiEntry() {

    }
    public WifiEntry(WifiP2pDevice device)
    {
        this.deviceName = device.deviceName;
        this.deviceAddress = device.deviceAddress;
        this.status = device.status;
        this.isGroupOwner = device.isGroupOwner();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WifiEntry wifiEntry = (WifiEntry) o;
        return status == wifiEntry.status && isGroupOwner == wifiEntry.isGroupOwner && deviceName.equals(wifiEntry.deviceName) && deviceAddress.equals(wifiEntry.deviceAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, isGroupOwner, deviceName, deviceAddress);
    }
}
