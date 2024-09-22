package com.twoplay.pipedal.model;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * Copyright (c) 2015, sRobin Davies
 * Created by Robin on 05/08/2024.
 */
public class P2PConnectionManager {
    private Context context;
    private ConnectivityManager connectivityManager;

    private static String TAG = "P2PConnectivityManager";

    ConnectivityManager.NetworkCallback myNetworkCallback =  new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            Log.i(TAG,"OnAvailable");
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            Log.i(TAG,"OnCapabilitiesChanged");
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            Log.i("TAG","OnLost");
        }
    };

    public void OnCreate(Activity activity) {
        context = activity.getApplicationContext();

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_WIFI_P2P)
                .build();

        connectivityManager =
                (ConnectivityManager) context.getSystemService(ConnectivityManager.class);
        connectivityManager.requestNetwork(networkRequest, myNetworkCallback);
    }

    void OnDestroy(Activity activity)
    {
        if (connectivityManager != null)
        {
            connectivityManager.unregisterNetworkCallback(myNetworkCallback);
        }
    }
}
