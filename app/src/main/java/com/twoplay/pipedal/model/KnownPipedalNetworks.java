package com.twoplay.pipedal.model;

import java.util.ArrayList;

/**
 * Copyright (c) 2015, sRobin Davies
 * Created by Robin on 18/09/2024.
 */
public class KnownPipedalNetworks {
    public void Load() {
    }
    private void Save() {

    }
    void AddGoodNetwork(String ssid)
    {
        if (!goodNetworks.contains(ssid)) {
            goodNetworks.remove(ssid);
            Save();
        }
    }
    void RemoveGoodNetwork(String ssid)
    {
        goodNetworks.remove(ssid);
        Save();
    }
    boolean IsGoodNetwork(String ssid)
    {
        return goodNetworks.contains(ssid);
    }
    void ClearKnownNetworks()
    {
        goodNetworks.clear();
        Save();
    }
    void SetAutoConnect(boolean autoConnect)
    {
        this.autoConnect = autoConnect;
        Save();
    }
    private boolean autoConnect;
    private ArrayList<String> goodNetworks = new ArrayList<>();

}
