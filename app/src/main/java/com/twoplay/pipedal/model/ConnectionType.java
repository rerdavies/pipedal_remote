package com.twoplay.pipedal.model;

/**
 * Copyright (c) 2022, Robin Davies
 * Created by Robin on 10/04/2022.
 */


public enum ConnectionType {
    // in order of preference.
    Loopback,
    Ethernet,
    Wifi,
    WifiDirect,
    Unknown
};

