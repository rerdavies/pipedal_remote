package com.twoplay.pipedal;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.util.Log;

import androidx.core.app.ActivityCompat;

/**
 * Copyright (c) 2015, sRobin Davies
 * Created by Robin on 11/05/2022.
 */
public class DisconnectAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "DisconnectAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        Handler handler = new Handler();
        WifiP2pManager p2pManager;

        final WifiP2pManager wifiP2pManager = (WifiP2pManager) (context.getSystemService(Context.WIFI_P2P_SERVICE));
        final WifiP2pManager.Channel wifiP2pChannel = wifiP2pManager.initialize(context, context.getMainLooper(), null);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Unable to close Wi-Fi Direct connection. Permission denied.");
            return;
        }
        wifiP2pManager.requestGroupInfo(wifiP2pChannel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {
                if (group != null && wifiP2pManager != null && wifiP2pChannel != null) {
                    wifiP2pManager.removeGroup(wifiP2pChannel, new WifiP2pManager.ActionListener() {

                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Wi-Fi Direct connection closed.");
                        }

                        @Override
                        public void onFailure(int reason) {
                            Log.e(TAG, "Failed to close Wi-Fi Direct connection." + reason);
                        }
                    });
                }
            }
        });

    }
}
