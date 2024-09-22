package com.twoplay.pipedal.model;

import android.content.Context;

import com.twoplay.pipedal.Preferences;

/**
 * Copyright (c) 2015, sRobin Davies
 * Created by Robin on 02/08/2024.
 */
public class LastP2Pconnection {
    public LastP2Pconnection() {}
    public LastP2Pconnection(Context context) {
        Load(context);
    }
    public void Load(Context context) {
        instanceId = Preferences.getSelectedServerInstanceId(context);
        serverName = Preferences.getSelectedServerName(context);
        portNumber = Preferences.getSelectedServerPort(context);
        valid = (!instanceId.isEmpty())
                && (portNumber != -1);

    }
    public void Save(Context context) {
         Preferences.setSelectedServer(context,serverName,instanceId,portNumber);
    }
    public boolean valid = false;
    public String serverName;
    public String instanceId;
    public int portNumber = -1;

}
