package com.twoplay.pipedal.model;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.util.Log;

import com.twoplay.pipedal.ErrorDialogFragment;
import com.twoplay.pipedal.PiPedalApplication;
import com.twoplay.pipedal.Preferences;
import com.twoplay.pipedal.Promise;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

public class Model
        extends AndroidViewModel {
    private DeviceScanner scanner;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel wifiP2pChannel;

    public Model(@NonNull Application application) {
        super(application);
        this.wifiP2pManager = (WifiP2pManager) (application.getSystemService(Context.WIFI_P2P_SERVICE));
        this.wifiP2pChannel = wifiP2pManager.initialize(application, application.getMainLooper(),null);
        this.scanner = new DeviceScanner(this,application);    }

    public boolean hasWifiDirectConnection() {
        return hasWifiDirectConnection_;
    }

    public void setHasWifiDirectConnection(boolean hasWifiDirectConnection) {
        this.hasWifiDirectConnection_ = hasWifiDirectConnection;
    }

    private boolean hasWifiDirectConnection_;



    @Override
    protected void onCleared() {

        p2pDisconnect(null);
        super.onCleared();
    }

    private boolean unfilteredScan = false;
    public boolean isUnfilteredScan() {
        return unfilteredScan;
    }

    public void setIsUnfilteredScan(boolean value)
    {
        unfilteredScan = value;
    }


    public interface DeviceFoundListener {
        void onDeviceFound(DeviceConnection deviceConnection);
    }

    public interface PageUnloadListener {
        Promise<Void> unloadPage();
    }

    private PageUnloadListener pageUnloadListener;

    public void setPageUnloadListener(PageUnloadListener listener)
    {
        this.pageUnloadListener = listener;
    }

    private DeviceFoundListener deviceFoundListener;
    public void setDeviceFoundListener(DeviceFoundListener listener)
    {
        deviceFoundListener = listener;
    }
    public void fireDeviceFound(DeviceConnection connection)
    {
        if (deviceFoundListener != null)
        {
            deviceFoundListener.onDeviceFound(connection);
        }
    }

    public void connectToDevice(Context context)
    {
        String selectedInstance = Preferences.getSelectedServerInstanceId(context);
        if (selectedInstance.isEmpty())
        {
            this.scanner.restartScan();
        } else {
            this.scanner.searchForDevice(selectedInstance);
        }
    }
    public void selectNewDevice(Context activity) {
        scanner.restartScan();
    }

    public DeviceScanner getDeviceScanner() {
        return scanner;
    }


    public void stopScan() {
        scanner.stopScan();
    }


    // relayed from the main activity.


    private String pendingTitle;
    private String pendingError;

    public void showError(String error, String title) {
        pendingTitle = title;
        pendingError = error;
        if (activity != null) {
            String thisError = pendingError;
            String thisTitle = pendingTitle;
            pendingError = null;
            pendingTitle = null;
            ErrorDialogFragment.execute(activity, thisError, thisTitle);
        }
    }

    FragmentActivity activity;

    public void onActivityResume(Activity activity) {

        this.activity = (FragmentActivity) activity;
        if (pendingError != null) {
            showError(pendingError, pendingTitle);
        }
    }

    public void onActivityPause(Activity activity) {
        this.activity = null;
    }


    public MutableLiveData<ScanState> scanState = new MutableLiveData<>(ScanState.Uninitialized);
    public MutableLiveData<String> scanError = new MutableLiveData<>("");

    public void setScanState(ScanState scanState) {
        setScanState(scanState, "");
    }
    public ScanState getScanState() { return scanState.getValue(); }

    public void setScanState(ScanState scanState, String errorText) {
        this.scanState.setValue(scanState);
        this.scanError.setValue(errorText);
    }
    private boolean choosingNewDevice = false;
    public void webCallbackChooseNewDevice(Activity activity) {
        isWebPageValid = false;

        Preferences.removeSelectedServer(PiPedalApplication.getContext());
        currentConnection = null;
        scanner.restartScan();
        choosingNewDevice = true;
    }
    public boolean isChoosingNewDevice() { return choosingNewDevice; }


    private boolean isWebViewDisconnected = false;
    public void webCallbackOnLostConnection(Activity activity, boolean isDisconnected) {
        if (isDisconnected == isWebViewDisconnected)
        {
            return;
        }
        isWebPageValid = !isDisconnected;
        isWebViewDisconnected = isDisconnected;

        currentConnection = null;

        if (isDisconnected) {
            scanner.restartScan();
            choosingNewDevice = false;
        }
        else {
            stopScan();
            this.setScanState(ScanState.ViewWeb);
        }
    }
    public void finishChoosingNewDevice(Activity activity)
    {
        stopScan();
        setScanState(ScanState.ViewWeb); // i.e. go back to the web view we had.
    }


    public boolean onP2pBroadcastReceived(Context context, Intent intent) {
        return false;
    }



    public static class DeviceConnection {
        private String name;
        private String instanceId;
        private String address;

        public DeviceConnection(@NonNull String name,@NonNull String instanceId, @NonNull String address) {
            this.name = name;
            this.instanceId = instanceId;
            this.address = address;
        }

        public DeviceConnection(PiPedalConnection device) {
            this.name = device.getDisplayName();
            this.instanceId = device.getInstanceId();
            this.address = device.getBestConnection();
        }

        public @NonNull String getInstanceId() {
            return instanceId;
        }

        public @NonNull String getAddress() {
            return address;
        }

        public @NonNull String getName() {
            return name;
        }
    }


    public MutableLiveData<DeviceConnection> serviceConnection = new MutableLiveData<>(null);

    private DeviceConnection currentConnection;

    private boolean isWebPageValid = false;
    public void setConnection(PiPedalConnection connection) {
        if (connection == null)
        {
            this.currentConnection = null;
            this.isWebPageValid = false;
            serviceConnection.setValue(null);

        } else {
            isWebViewDisconnected = false;
            DeviceConnection t = currentConnection;


            if (t == null ||
                    (!connection.getServiceLocations().contains(t.getAddress()))
                    || !isWebPageValid)
            {
                t = new DeviceConnection(connection.getDisplayName(), connection.getInstanceId(),connection.getBestConnection());

                currentConnection = t;
                serviceConnection.setValue(t);
                Log.d(TAG,"CONNECTION ADDRESS: " + t.getAddress());
                getDeviceScanner().stopScan();
                setScanState(ScanState.ViewWeb);
            }
        }
    }
    private boolean showPageLoading_ = false;
    public boolean showPageLoading() {
        return showPageLoading_;
    }
    public void showPageLoading(boolean value)
    {
        showPageLoading_ = value;
    }
    private boolean HasPermission()
    {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            if (ActivityCompat.checkSelfPermission(PiPedalApplication.getContext(), Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED)
            {
                return true;
            }
        }
        if (ActivityCompat.checkSelfPermission(PiPedalApplication.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }


    private static final String TAG ="PiPedalModel";
    public interface DisconnectCallback {
        void onDisconnected();
    }
    @SuppressLint("MissingPermission")
    public void p2pDisconnect(DisconnectCallback disconnectCallback) {
        if (pageUnloadListener != null)
        {
            pageUnloadListener.unloadPage()
                .andThen((v)-> {
                   pageUnloadListener = null;
                   Log.i(TAG,"Page unloaded.");
                   p2pDisconnect(disconnectCallback);
                })
                .andCatch((e)-> {
                    Log.e(TAG,"Page unload failed.." + e.getMessage());
                    pageUnloadListener = null;
                    p2pDisconnect(disconnectCallback);
                });

        }
        if (this.wifiP2pManager != null && this.wifiP2pChannel != null) {
            // prepare for this to go into gc..
            WifiP2pManager wifiP2pManager = this.wifiP2pManager;
            WifiP2pManager.Channel wifiP2pChannel = this.wifiP2pChannel;

            if (!HasPermission()) {
                if (disconnectCallback != null) disconnectCallback.onDisconnected();
                return;
            }
            //noinspection Convert2Lambda
            wifiP2pManager.requestGroupInfo(wifiP2pChannel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null) {
                        wifiP2pManager.removeGroup(wifiP2pChannel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                if (disconnectCallback != null) disconnectCallback.onDisconnected();
                            }

                            @Override
                            public void onFailure(int reason) {
                                if (disconnectCallback != null) disconnectCallback.onDisconnected();
                            }
                        });
                    } else {
                        if (disconnectCallback != null) disconnectCallback.onDisconnected();
                    }
                }
            });
        }
    }
}

