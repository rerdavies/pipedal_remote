package com.twoplay.pipedal.model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.twoplay.pipedal.Completion;
import com.twoplay.pipedal.Promise;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

/**
 * Copyright (c) 2022-2024, Robin Davies
 * Created by Robin on 11/04/2022.
 */

/*
    Establish a connection. The various cases:

    1. We have an ethernet connection, and we can locate an instance of pipedal.
        - scan and connect.
    2. We have an SSID that we know has pipedal, and we're already connectd.
        - scan and connect.

    3. we have an SSID that we don't know has pipedal, and we're already connected.
         - look for mdns service.
         - if we find it, auto-conned.
         - If the connection succeeds, add the current SSID as a known network.
         - If not, prompt user to select a wi-fi network.
     4. we don't have a wifi connection
        - if we have known networks, do a wifi scan, and wait for a known connection.
        - if that fails,prompt use to connect to a nework.
        - Wait for network to change.
        - scan the available network.
        - if the scan succeeds, auto-connect.
        - if the scan fails, prompt for a network.

    UI menu items:  X Auto-connect to Wi-Fi networks.
              Forget remembered wifi connections.

 */

@SuppressLint("MissingPermission")
public class DeviceScanner {
    static final int SCAN_TIME_MS = 20000;
    static final int SEARCH_TIME_MS = 20000;
    private static final String PIPEDAL_SD_SERVICE_TYPE = "_pipedal._tcp";

    private final WifiManager wifiManager;
    private final Model model;
    private KnownPipedalNetworks knownPipedalNetworks = new KnownPipedalNetworks();
    private Context context;
    private Context getContext() { return context; }

    public DeviceScanner(Model model,Context context)
    {
        this.model = model;
        this.context = context.getApplicationContext();
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        knownPipedalNetworks.Load();
        this.nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);

    }

    private void resetDeviceLists()
    {
        this.pipedalDevices.setValue(new ArrayList<>());
    }
    private static String TAG = "DeviceScanner";

    private void onError(Exception e)
    {
        Log.e(TAG,e.getMessage());
    }

    private String targetDeviceInstance = "";
    public void searchForDevice(String targetDeviceInstance)
    {
        this.targetDeviceInstance = targetDeviceInstance;
        resetDeviceLists();
        model.setScanState(ScanState.SearchingForInstance);

        asyncStartScan_()
                .andThen((voidVal)->{})
                .andCatch((exception)->{ onError(exception); })
        ;

    }

    public void stopScan() {
        model.setScanState(ScanState.ScanComplete);
        asyncStopScan()
                .andCatch((exception)->{ onError(exception);});
    }

    public void restartScan() {
        resetDeviceLists();
        model.setScanState(ScanState.Searching);

        asyncStartScan_()
                .andThen((voidVal)->{})
                .andCatch((exception)->{ onError(exception); })
        ;

    }

    private final MutableLiveData<List<PiPedalConnection>> pipedalDevices = new MutableLiveData<>(new ArrayList<PiPedalConnection>());

    public MutableLiveData<List<PiPedalConnection>> getPiPedalDevices() {
        return pipedalDevices;

    }
    public interface StatusChangedListener {
        void onStatusChanged(PiPedalConnection connection);

    }

    private StatusChangedListener statusChangedListener;

    public void setStatusChangedListener(StatusChangedListener listener) {
        this.statusChangedListener = listener;
    }


    private void onStatusChanged(PiPedalConnection piPedalConnection) {
        if (statusChangedListener != null) {
            statusChangedListener.onStatusChanged(piPedalConnection);
        }
    }

    public MutableLiveData<String> scanForDeviceMessage = new MutableLiveData<>("");

    private enum ScanForDeviceState {
        Idle,
        Scanning,
        Connecting, Resolving,
    }

    private NsdManager nsdManager;

    private Handler handler = new Handler();
    private boolean isScanning = false;


    private Promise<Void> asyncStopScan() {
        // yyy
        cancelScanTimeout();
        return asyncStopScan_();
    }
    private Promise<Void> asyncStopScan_()
    {
        return new Promise<Void>(
                (completion)->
                {
                    Promise<Void> serviceDiscoveryPromise;
                    if (nsdDiscoveryListener != null) {
                        serviceDiscoveryPromise = nsdDiscoveryListener.asyncStopDiscovery();
                    } else {
                        serviceDiscoveryPromise = Promise.<Void>EmptyPromise();
                    }
                    serviceDiscoveryPromise.andThen(
                            (voidVal)->{
                                completion.fulfill(null);
                            })
                        .andCatch((exception)->{
                           completion.reject(exception);
                        });
                }
        );
    }

    private Runnable timeoutRunnable;

    void cancelScanTimeout() {
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
    }

    private void setScanTimeout(Runnable runnable) {
        cancelScanTimeout();
        timeoutRunnable = runnable;
        handler.postDelayed(runnable, SEARCH_TIME_MS);
    }

    @SuppressLint("MissingPermission")
    private Promise<Void> asyncStartScan_() {
        isScanning = true;
        return new Promise<Void>(handler, (completion) -> {
            this.nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);

            setScanTimeout(
                    () -> {
                        isScanning = false;
                        model.setScanState(ScanState.ScanComplete);
                    }
            );


            completion.onCancelled(() -> {
                asyncStopScan().andThen((voidVal) -> {
                    completion.fulfill(null);
                });
            });
            asyncRestartDnsSdQuery()
                    .andThen((voidVal) -> {
                        completion.fulfill(null);
                    })
                    .andCatch((Exception e) -> {
                        completion.reject(e);
                    });

        });

    }
    private void onNsdServiceFound(NsdServiceInfo serviceInfo) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // if only an IPv6 address, discard the announcement.
            boolean goodAddress = false;
            for (InetAddress address: serviceInfo.getHostAddresses()) {
                if (address instanceof Inet4Address) {
                    goodAddress = true;
                    break;
                }
            }
            if (!goodAddress) {
                return;
            }

        }

        String instanceId = PiPedalConnection.parseInstanceId(serviceInfo);
        if (instanceId.isEmpty()) {
            return;
        }

        PiPedalConnection connection = null;
        for (PiPedalConnection device: this.pipedalDevices.getValue())
        {
            if (device.getInstanceId().equals(instanceId))
            {
                connection = device;
                break;
            }
        }
        if (connection != null)
        {
            connection.addServiceInfo(serviceInfo);
        } else {
            connection = new PiPedalConnection(wifiManager,serviceInfo);
            List<PiPedalConnection> oldList = this.pipedalDevices.getValue();

            ArrayList<PiPedalConnection> newList = new ArrayList<PiPedalConnection>(oldList);
            newList.add(connection);
            this.pipedalDevices.setValue(newList);
        }

        if (this.model.getScanState() == ScanState.SearchingForInstance)
        {
            if (connection.getInstanceId().equals(this.targetDeviceInstance))
            {
                onConnectDeviceFound(connection);
            }
        }
    }



    // Instantiate a new DiscoveryListener
    // Instantiate a new DiscoveryListener
    class DiscoveryListener implements NsdManager.DiscoveryListener {


        private ArrayList<NsdServiceInfo> pendingNdsResolves = new ArrayList<>();
        private boolean resolvePending = false;

        private NsdManager.ResolveListener resolveListener;


        Completion<Void> discoveryStoppedCompletion;

        Promise<Void> asyncStopDiscovery() {
            return new Promise<Void>((completion) -> {
                if (closed) {
                    completion.fulfill(null);
                }
                closed = true;
                discoveryStoppedCompletion = completion;
                try {
                    if (this.resolveListener != null)
                    {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            nsdManager.stopServiceResolution(this.resolveListener);
                        }
                        this.resolvePending = false;
                        this.resolveListener = null;
                    }
                } catch (Exception e)
                {

                }
                try {
                    nsdManager.stopServiceDiscovery(this);
                } catch (Exception e) {
                    Log.e(TAG, "stopServiceDiscovery failed: " + e.getMessage());
                    completion.fulfill(null);
                }
                completion.fulfill(null);
            });
        }

        private Completion<Void> discoverServicesCompletion;

        public Promise<Void> asyncDiscoverServices() {
            return new Promise<Void>((completion) -> {
                discoverServicesCompletion = completion;
                nsdManager.discoverServices(
                        PIPEDAL_SD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, this); // has to be unique each time (see docs).

            });

        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "OnStartDiscoveryFailed: Error code:" + errorCode);
            closed = true;
            if (discoverServicesCompletion != null) {
                Exception e = new Exception("OnStartDiscoveryFailed: Error code:" + errorCode);
                discoverServicesCompletion.reject(e);
                discoverServicesCompletion = null;
            }
        }

        // Called as soon as service discovery begins.
        @Override
        public void onDiscoveryStarted(String regType) {
            Log.i(TAG, "Service discovery started");
            if (discoverServicesCompletion != null) {
                discoverServicesCompletion.fulfill(null);
                discoverServicesCompletion = null;
            }
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "onStopDiscoveryFailed (" + errorCode + ")");
            resolvePending = false;
            closed = true;
            if (discoveryStoppedCompletion != null) {
                nsdDiscoveryListener = null;
                discoveryStoppedCompletion.fulfill(null);
                discoveryStoppedCompletion = null;
            }

        }

        @Override
        public void onDiscoveryStopped(String regType) {
            resolvePending = false;
            closed = true;
            nsdDiscoveryListener = null;

            if (discoveryStoppedCompletion != null) {
                discoveryStoppedCompletion.fulfill(null);
                discoveryStoppedCompletion = null;
            }
        }

        private void resolveNext() {
            if (closed) {
                Log.d(TAG, "Discarding NDS request. (Closed)");
                return;
            }
            if (pendingNdsResolves.size() != 0) {
                if (resolvePending) {
                    Log.d(TAG, "Queueing NDS request.");
                    return;
                }
                resolvePending = true;
                try {

                    if (!isScanning) {
                        Log.d(TAG, "Discarding NDS request. (Not scanning)");
                        pendingNdsResolves.clear();
                        resolvePending = false;
                        return;
                    }
                    NsdServiceInfo next = pendingNdsResolves.remove(0);
                    Log.d(TAG, "Resolving mDNS service " + next.getServiceName());
                    resolveListener =
                            new NsdManager.ResolveListener() {
                                @Override
                                public void onResolutionStopped(@NonNull NsdServiceInfo serviceInfo) {
                                    NsdManager.ResolveListener.super.onResolutionStopped(serviceInfo);
                                    resolvePending = false;
                                }

                                @Override
                                public void onStopResolutionFailed(@NonNull NsdServiceInfo serviceInfo, int errorCode) {
                                    NsdManager.ResolveListener.super.onStopResolutionFailed(serviceInfo, errorCode);
                                    resolvePending = false;
                                }

                                public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                                    String errorMessage;
                                    switch (errorCode) {
                                        case NsdManager.FAILURE_ALREADY_ACTIVE:
                                            errorMessage = "FAILURE_ALREADY_ACTIVE";
                                            break;

                                        case NsdManager.FAILURE_INTERNAL_ERROR:
                                            errorMessage = "FAILURE_INTERNAL_ERROR";
                                            break;
                                        case NsdManager.FAILURE_MAX_LIMIT:
                                            errorMessage = "FAILURE_MAX_LIMIT";
                                            break;
                                        default:
                                            errorMessage = "UNKNOWN ERROR";
                                            break;
                                    }
                                    Log.e(TAG, "NsdManager.ResolveListener failed. " + errorMessage + "= " + errorCode);
                                    resolvePending = false;
                                    resolveNext();
                                }

                                public void onServiceResolved(NsdServiceInfo serviceInfo) {
                                    try {
                                        Log.i(TAG, "mDNS Service found: " + serviceInfo);
                                        if (closed)
                                            return;

                                        handler.post(() -> {
                                            if (closed) {
                                                return;
                                            }
                                            onNsdServiceFound(serviceInfo);
                                        });
                                    } catch (Exception e) {
                                        Log.e(TAG, "Failed to add discovered service." + e.getMessage());
                                    }
                                    resolvePending = false;
                                    resolveNext();
                                }
                            };

                    nsdManager.resolveService(next, resolveListener);

                } catch (Exception e) {
                    Log.e(TAG, "Unhandled exception in resolveNext(): " + e.getMessage());
                    resolvePending = false;
                }
            }
        }


        @Override
        public void onServiceFound(NsdServiceInfo service) {



            if (closed) return;
            if (service.getServiceType().startsWith(PIPEDAL_SD_SERVICE_TYPE)) {
                Log.i(TAG, "Queuing mDNS service " + service.getServiceName() + " " + service.getHost() + " : " + service.getPort());
                if (nsdManager != null) {
                    pendingNdsResolves.add(service);
                    resolveNext();
                } else {
                    throw new RuntimeException("ndsManager not initialized.");
                }
            }
        }


        @Override
        public void onServiceLost(NsdServiceInfo service) {
            // TODO:
        }

        private boolean closed = false;

    }


    private DiscoveryListener nsdDiscoveryListener = null;

    DiscoveryListener makeDiscoveryListener() {
        nsdDiscoveryListener = new DiscoveryListener();
        return nsdDiscoveryListener;
    }


    Promise<Void> asyncRestartDnsSdQuery() {
        return new Promise<Void>((completion) -> {
            Promise<Void> stopPromise = Promise.<Void>EmptyPromise();
            if (this.nsdDiscoveryListener != null) {
                Log.d(TAG, "asyncRestartDnsSdQuery");
                stopPromise = nsdDiscoveryListener.asyncStopDiscovery();
                this.nsdDiscoveryListener = null;
            }
            stopPromise.andThen((voidArg) -> {
                makeDiscoveryListener().asyncDiscoverServices()
                        .andThen((voidArg2) -> {
                            completion.fulfill(null);
                        })
                        .andCatch((exception)->
                        {
                            completion.reject(exception);
                        });
            });
        });
    }

    private void onConnectDeviceFound(PiPedalConnection service) {
        asyncStopScan().andCatch((e) -> {
            Log.e(TAG, "asyncStopScan failed. " + e.getMessage());
        });
        model.setConnection(service);
    }

}
