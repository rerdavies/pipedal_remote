package com.twoplay.pipedal;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.webkit.WebView;

import com.twoplay.pipedal.model.Model;
import com.twoplay.pipedal.model.ScanState;
import com.twoplay.pipedal.model.LastP2Pconnection;
import com.twoplay.pipedal.model.TerminatingViewModel;
import com.twoplay.pipedal.model.LastP2Pconnection;

import java.net.MalformedURLException;
import java.net.URL;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

public class MainActivity extends AppCompatActivity
        implements RationaleFragment.RationaleResult,
            WebViewFragment.ShowSponsorshipListener,
            SponsorshipFragment.BackListener
{
    private Model model;
    private OnBackPressedCallback onBackPressed = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            disconnectAndFinish();
        }
    };
    private TerminatingViewModel terminatingViewModel;

    private void disconnectAndFinish() {
        if (model != null)
        {
            model.p2pDisconnect(this::finish);
        } else {
            finish();
        }
    }

    @Override
    public void onShowSponsorship() {
        setActivityState(ActivityState.ViewSponsorship);
    }

    @Override
    public void onReturnFromSponsorship() {
        setActivityState(ActivityState.ShowWebView);
    }


    private enum ActivityState {
        Created,
        ShowRationale,
        RequestingPermission,
        ShowScanner,
        SearchingForInstance, ViewSponsorship, ShowWebView
    }



    private ActivityState activityState = ActivityState.Created;




    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    this.rationaleShown = false;
                }
                maybeRequestPermissions();
            });


    private String[] requiredPermissions12 = {
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.INTERNET,
    };
    private String[] requiredPermissions13 = {
            android.Manifest.permission.NEARBY_WIFI_DEVICES,
            // android.Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.INTERNET,
    };

    private boolean hasAllPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            for (String permission : requiredPermissions13) {
                if (ContextCompat.checkSelfPermission(
                        this, permission) !=
                        PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        } else {
            for (String permission : requiredPermissions12) {
                if (ContextCompat.checkSelfPermission(
                        this, permission) !=
                        PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }

        }

        return true;
    }

    private void maybeRequestPermissions() {
        boolean requested = false;
        if (!hasAllPermissions()) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (!rationaleShown) {
                    setActivityState(ActivityState.ShowRationale);
                    return;
                } else {
                    setActivityState(ActivityState.RequestingPermission);

                }
            }
            String[] permissions = requiredPermissions12;
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions = requiredPermissions13;
            }
            for (String permission : permissions) {

                // Use NEARBY_WIFI_DEVICES instead of ACCESS_FINE_LOCATION on Android 13+
                if (ContextCompat.checkSelfPermission(
                        this, permission) !=
                        PackageManager.PERMISSION_GRANTED) {
                    requested = true;
                    activityState = ActivityState.RequestingPermission;

                    // You can directly ask for the permission.
                    // The registered ActivityResultCallback gets the result of this request.
                    this.setActivityState(ActivityState.RequestingPermission);
                    requestPermissionLauncher.launch(
                            permission);
                    return;
                }
            }
        }
        if (!requested) {
            maybeSearchForExistingConnection();
        }
    }

    private void maybeSearchForExistingConnection() {
        model.scanState.observe(this,(ScanState scanState) ->onScanStateChanged(scanState));
        onScanStateChanged(model.scanState.getValue());
        if (model.scanState.getValue() == ScanState.Uninitialized)
        {
            model.connectToDevice(this);
        }
    }

    @Override
    public void OnRationaleResult(boolean proceed) {
        if (proceed)
        {
            this.rationaleShown = true;
            maybeRequestPermissions();
        } else {
            finish();
        }
    }



    class P2pBroadcastReceiver extends  BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (model.onP2pBroadcastReceived(context, intent)) {
                return;
            }
        }
    }

    P2pBroadcastReceiver p2pBroadcastReceiver;

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(p2pBroadcastReceiver);
        model.onActivityPause(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        p2pBroadcastReceiver = new P2pBroadcastReceiver();
        registerReceiver(p2pBroadcastReceiver,intentFilter);

        model.onActivityResume(this);
    }


    private void onScanStateChanged(ScanState scanState) {
        switch (scanState) {
//                setActivityState(ActivityState.SearchingForInstance);
//                break;
            case SearchingForInstance:
            case ChooseNewDevice:
            case ConnectionLost:
            case Searching:
            case ErrorState:
            case ScanComplete:
                setActivityState(ActivityState.ShowScanner);
                break;
            case ViewWeb:
                setActivityState(ActivityState.ShowWebView);
                break;
            default:
        }
    }

    private WebViewFragment getWebViewFragment()
    {
        if (activityState != ActivityState.ShowWebView) return null;

        WebViewFragment webviewFragment = (WebViewFragment) (getSupportFragmentManager().findFragmentById(R.id.web_container_view));
        return webviewFragment;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // if the web view can navigate back, do it.
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            WebViewFragment webViewFragment = getWebViewFragment();
            if (webViewFragment != null) {
                if (webViewFragment.NavigateBack()) {
                    return true;
                }
            }
        }
        // If it isn't the Back button or there's no web page history, bubble up to
        // the default system behavior. Probably exit the activity.
        return super.onKeyDown(keyCode, event);
    }


    private void setActivityState(ActivityState activityState) {
        if (activityState != this.activityState) {
            this.activityState = activityState;
            switch (activityState) {
                case ViewSponsorship:
                    getSupportFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .replace(R.id.fragment_container_view, SponsorshipFragment.class, null)
                            .commit();
                    break;
                case SearchingForInstance:
                    getSupportFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .replace(R.id.fragment_container_view, SearchForDeviceFragment.class, null)
                            .commit();
                    break;
                case ShowRationale:
                    getSupportFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .replace(R.id.fragment_container_view, RationaleFragment.class, null)
                            .commit();
                    break;
                case ShowScanner:
                {
                    getSupportFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .replace(R.id.fragment_container_view, ScannerFragment.class, null)
                            .commit();
                }
                break;
                case ShowWebView: {
                    Model.DeviceConnection serviceConnection = model.serviceConnection.getValue();

                    assert serviceConnection != null;
                    String connectionAddress = serviceConnection.getAddress();
                    try {
                        URL url = new URL(connectionAddress);
                        int port = url.getPort();
                        if (port == -1) port = 80;
                        Preferences.setSelectedServer(
                                this,
                                serviceConnection.getName(),
                                serviceConnection.getInstanceId(),
                                port);

                    } catch (MalformedURLException e) {

                    };

                    // Remove the scanner fragment, revealing the web view underneath.
                    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container_view);
                    if (fragment != null)
                    {
                        getSupportFragmentManager().beginTransaction()
                                .remove(fragment)
                                .commit();
                    }

                    WebViewFragment webviewFragment = (WebViewFragment) (getSupportFragmentManager().findFragmentById(R.id.web_container_view));
                    if (webviewFragment != null) {
                        webviewFragment.setUrl(connectionAddress);
                    }
                }
                break;
                default:
                {
                    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container_view);
                    if (fragment != null)
                    {
                        getSupportFragmentManager().beginTransaction()
                                .remove(fragment)
                                .commit();
                    }
                }
                break;

            }

        }

        updateSystemBarVisibility();
    }
    private boolean isLandscape()
    {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }
    void updateSystemBarVisibility()
    {


        boolean hideSystemBars = isLandscape() ;


        WindowInsetsControllerCompat insetsController = ViewCompat.getWindowInsetsController(getWindow().getDecorView());
        if (insetsController == null) return;

        // insetsController.setAppearanceLightStatusBars(activityState == ActivityState.ShowWebView);

        if (hideSystemBars)
        {
            insetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
            insetsController.hide(WindowInsetsCompat.Type.statusBars());
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars());
        }
    }

    private final IntentFilter intentFilter = new IntentFilter();

    private boolean rationaleShown = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtils.loadUserPreferredTheme(this);
        setTheme(ThemeUtils.getUserPreferredThemeResourceId());
        ThemeUtils.setUserPreferredThemeChangeListener((newTheme)->{
            this.recreate();
        });

        updateSystemBarVisibility();

        setContentView(R.layout.activity_main);
        ViewModelProvider viewModelProvider = new ViewModelProvider(this);
        this.terminatingViewModel = viewModelProvider.get(TerminatingViewModel.class);
        this.model = viewModelProvider.get(Model.class);
        if (savedInstanceState != null)
        {
            savedInstanceState.getBoolean(KEY_RATIONALE_SHOWN,false);
        }
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // create a *retained fragment containing the web view.
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.web_container_view, WebViewFragment.class, null)
                    .commit();
        }

        getOnBackPressedDispatcher().addCallback(this.onBackPressed);



        maybeRequestPermissions();
    }
    @Override
    protected void onStart() {
        cancelDisconnectAlarm();
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (model.hasWifiDirectConnection())
        {
            // setDisconnectAlarm();
        }
    }

    private PendingIntent alarmIntent;
    private void cancelDisconnectAlarm()
    {
        if (alarmIntent != null)
        {
            AlarmManager alarmManager = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(alarmIntent);
        }
    }
    private void setDisconnectAlarm()
    {
        final long DISCONNECT_TIMEOUT_MS = 15*1000L;
        AlarmManager alarmManager = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this,DisconnectAlarmReceiver.class);
        alarmIntent = PendingIntent.getBroadcast(this,0,intent,PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + DISCONNECT_TIMEOUT_MS,
                alarmIntent);


    }

    private static final String KEY_RATIONALE_SHOWN = "rationale_shown";
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_RATIONALE_SHOWN,this.rationaleShown);

    }

    @Override
    protected void onDestroy() {
        ThemeUtils.setUserPreferredThemeChangeListener(null);
        if (isFinishing())
        {
            cancelDisconnectAlarm();
            model.stopScan();
        }
        super.onDestroy();
    }

}