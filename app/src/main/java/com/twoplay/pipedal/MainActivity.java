package com.twoplay.pipedal;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Handler;
import android.view.ViewGroup;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsAnimation;
import android.util.TypedValue;

import androidx.annotation.RequiresApi;

import com.twoplay.pipedal.model.BillingModel;
import com.twoplay.pipedal.model.Model;
import com.twoplay.pipedal.model.ScanState;
import com.twoplay.pipedal.model.TerminatingViewModel;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.List;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.WindowCompat;
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
    private BillingModel billingModel;
    private OnBackPressedCallback onBackPressed = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            disconnectAndFinish();
        }
    };
    private TerminatingViewModel terminatingViewModel;
    private ScrimLayout rootView;

    private View mainContent;
    private View webViewContainer;

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
        setShowSponsorship(true);
    }

    @Override
    public void onReturnFromSponsorship() {
        setShowSponsorship(false);

    }


    private enum ActivityState {
        Created,
        ShowRationale,
        RequestingPermission,
        ShowScanner,
        SearchingForInstance,
        WebViewLoading,
        ShowWebView
    }



    private ActivityState activityState = ActivityState.Created;

    private boolean showingSponsorship = false;


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
    @SuppressLint("InlinedApi")
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
        if (!hasAllPermissions()) {
            dismissSplashScreen();
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
        maybeSearchForExistingConnection();
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
            case Uninitialized:
            case SearchingForInstance:
                setActivityState(ActivityState.SearchingForInstance);
                break;
            case ChooseNewDevice:
            case ConnectionLost:
            case Searching:
            case ErrorState:
            case ScanComplete:
                dismissSplashScreen();
                setActivityState(ActivityState.ShowScanner);
                break;
            case WebViewLoading:
                setActivityState(ActivityState.WebViewLoading);
                break;
            case ViewWeb:
                setActivityState(ActivityState.ShowWebView);
                break;
            default:
        }
    }

    private WebViewFragment getWebViewFragment()
    {
        if (activityState != ActivityState.ShowWebView
                && activityState != ActivityState.WebViewLoading  // :-(
        ) {
            return null;
        }

        return (WebViewFragment) (getSupportFragmentManager().findFragmentById(R.id.web_container_view));
    }


    private OnBackPressedCallback backPressedCallback = null;
    private void handleBackPressed() {
        if (showingSponsorship) {
            setShowSponsorship(false);
            return;
        }
        WebViewFragment webViewFragment = getWebViewFragment();
        if (webViewFragment != null) {
            if (webViewFragment.NavigateBack()) {
                return; // we handled the back press.
            }
        }
        if (backPressedCallback != null) {
            backPressedCallback.remove();
            backPressedCallback = null;
        }
        getOnBackPressedDispatcher().onBackPressed();

    }


    private void setNormalStatusBar()
    {
        webViewContainer.setVisibility(View.GONE);

        boolean darkMode = ThemeUtils.isDarkModeEnabled(this);

        TypedValue typedValue = new TypedValue();getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);
        int paperColor = typedValue.data;


        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), this.rootView);
        insetsController.setAppearanceLightStatusBars(!darkMode);
        insetsController.setAppearanceLightNavigationBars(!darkMode);


        this.mainContent.setBackgroundColor(0);

        this.rootView.setStatusBarColor(0);
        this.rootView.setNavigationBarColorXX(0);

    }
    private void setWebviewStatusBar()
    {
        webViewContainer.setVisibility(View.VISIBLE);
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), this.rootView);
        if (ThemeUtils.isDarkModeEnabled(this)) {

            int paperColor = ContextCompat.getColor(this, R.color.webStatusBarColorDark);

//            insetsController.setAppearanceLightStatusBars(false);
//            insetsController.setAppearanceLightNavigationBars(false);
            this.rootView.setStatusBarColor(paperColor);
            this.rootView.setNavigationBarColorXX(paperColor);


        } else {
            int statusBarColor = ContextCompat.getColor(this, R.color.webStatusBarColorLight);
            int navColor = ContextCompat.getColor(this, R.color.webNavBarColorLight);
//            insetsController.setAppearanceLightStatusBars(true);
//            insetsController.setAppearanceLightNavigationBars(true);

            this.rootView.setStatusBarColor(navColor);
            this.rootView.setNavigationBarColorXX(navColor);

        }

    }
    boolean isWebView() {
        return this.activityState == ActivityState.ShowWebView;
    }
    private void setActivityState(ActivityState activityState) {
        switch (activityState) {
            case SearchingForInstance:
            case ShowScanner:
            case Created:
            case WebViewLoading:
                    break;
            default:
                dismissSplashScreen();
                break;
        }
        if (activityState != this.activityState) {
            this.activityState = activityState;
            reloadFragments();

        }

        updateSystemBarVisibility();
    }

    private void reloadFragments()
    {
        if (this.showingSponsorship)
        {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragment_container_view, SponsorshipFragment.class, null)
                    .commit();
            setNormalStatusBar();

        } else {
            switch (activityState) {
                case SearchingForInstance:
                    getSupportFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .replace(R.id.fragment_container_view, SearchForDeviceFragment.class, null)
                            .commit();
                    setNormalStatusBar();
                    break;
                case ShowRationale:
                    getSupportFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .replace(R.id.fragment_container_view, RationaleFragment.class, null)
                            .commit();
                    setNormalStatusBar();
                    break;
                case ShowScanner: {
                    getSupportFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .replace(R.id.fragment_container_view, ScannerFragment.class, null)
                            .commit();
                    setNormalStatusBar();
                }
                break;
                case WebViewLoading: {
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

                    } catch (MalformedURLException ignored) {

                    }
                    WebViewFragment webviewFragment = (WebViewFragment) (getSupportFragmentManager().findFragmentById(R.id.web_container_view));
                    if (webviewFragment != null) {
                        webviewFragment.setUrl(connectionAddress);
                    }
                }
                break;

                case ShowWebView: {

                    // Remove the scanner fragment, revealing the web view underneath.
                    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container_view);
                    if (fragment != null) {
                        getSupportFragmentManager().beginTransaction()
                                .remove(fragment)
                                .commit();
                    }

                    setWebviewStatusBar();

                }
                break;
                default: {
                    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container_view);
                    if (fragment != null) {
                        getSupportFragmentManager().beginTransaction()
                                .remove(fragment)
                                .commit();
                    }
                }
                break;
            }
        }
    }

    private boolean isLandscape()
    {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }
    void updateSystemBarVisibility()
    {

        boolean hideSystemBars = isLandscape() ;
        {
            WindowInsetsControllerCompat rootInsetsController =
                    WindowCompat.getInsetsController(getWindow(), this.rootView);


        }
        {
            // set behavior of mainContent
            WindowInsetsControllerCompat insetsController =
                    WindowCompat.getInsetsController(getWindow(), this.rootView);


            // insetsController.setAppearanceLightStatusBars(activityState == ActivityState.ShowWebView);

            if (hideSystemBars) {
                insetsController.setSystemBarsBehavior(
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
                insetsController.hide(WindowInsetsCompat.Type.statusBars());
            } else {
                insetsController.show(WindowInsetsCompat.Type.statusBars());
            }
        }
    }

    private final IntentFilter intentFilter = new IntentFilter();

    private boolean rationaleShown = false;

    private boolean isAtLeastAndroid11() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
    }


    private int containerHeight = -1;

    private void setLayoutHeight(View view, int height)
    {
        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        mlp.height = height;
        view.setLayoutParams(mlp);
    }
    private void handleUiWindowInsets(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (vx, insets) -> {
                    return doApplyInsets(insets);
                }
        );
    }
    /** @noinspection SameReturnValue*/
    private WindowInsetsCompat doApplyInsets(WindowInsetsCompat insets) {
        rootView.setScrims(insets);

        if (!isWebView()) {
            var statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            var imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            var navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());

            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) mainContent.getLayoutParams();
            mlp.leftMargin = statusBarInsets.left + navInsets.left;
            mlp.topMargin = statusBarInsets.top;
            mlp.rightMargin = statusBarInsets.right +  navInsets.right;
            mlp.bottomMargin = navInsets.bottom ; // not  under the  nav bar.
            mainContent.setLayoutParams(mlp);
            return WindowInsetsCompat.CONSUMED;
        }

        WebViewFragment webviewFragment = getWebViewFragment();

        var imeRect = insets.getInsets(WindowInsetsCompat.Type.ime());
        var statusRect = insets.getInsets(WindowInsetsCompat.Type.statusBars());
        var navRect = insets.getInsets(WindowInsetsCompat.Type.navigationBars());


        int rootHeight = rootView.getHeight();
        int rootWidth = rootView.getWidth();

        Insets containerInsets = Insets.of(
                imeRect.left + navRect.left,
                statusRect.top+imeRect.top,
                imeRect.right + navRect.right,
                navRect.bottom);
        assert webviewFragment != null;
        Rect windowPosition = new Rect(
                containerInsets.left,containerInsets.top,
                rootWidth-containerInsets.right,
                rootHeight-containerInsets.bottom);
        webviewFragment.setImeInsets(windowPosition, imeRect.bottom);

        // set layout without scroll inset.
        // the web view fragment will handle the scroll
        ViewGroup.LayoutParams layoutParams = mainContent.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
            marginLayoutParams.topMargin = statusRect.top;
            marginLayoutParams.bottomMargin = navRect.bottom;  // NOT under the nav bar.
            marginLayoutParams.leftMargin = navRect.left;
            marginLayoutParams.rightMargin = navRect.right;
            mainContent.setLayoutParams(marginLayoutParams);
        }
        // ViewCompat.requestApplyInsets(container);

        return WindowInsetsCompat.CONSUMED;
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private void animateKeyboardDisplay() {

        WindowInsetsAnimation.Callback cb = new WindowInsetsAnimation.Callback(WindowInsetsAnimation.Callback.DISPATCH_MODE_STOP) {
            @NonNull
            @Override
            public WindowInsets onProgress(@NonNull WindowInsets insets_, @NonNull List<WindowInsetsAnimation> animations) {
                WindowInsetsCompat insets = WindowInsetsCompat.toWindowInsetsCompat(insets_, mainContent);
                doApplyInsets(insets);
                return WindowInsets.CONSUMED;
            }

            @Override
            public void onEnd(@NonNull WindowInsetsAnimation animation) {
                super.onEnd(animation);
            }
        };

        mainContent.setWindowInsetsAnimationCallback(cb);
    }

    private SplashScreen splashScreen = null;

    public void dismissSplashScreen()
    {
        if (splashScreen != null) {
            splashScreen.setKeepOnScreenCondition(() -> {
                return false;
            });
            splashScreen = null;
        }
        cancelSplashScreenTimer();
    }

    private Handler handler = new Handler();
    Runnable cancelSplashScreenRunnable = null;

    private void cancelSplashScreenTimer() {
        if (cancelSplashScreenRunnable != null) {
            handler.removeCallbacks(cancelSplashScreenRunnable);
            cancelSplashScreenRunnable = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setRequestedOrientation(Preferences.getScreenOrientation(this).getSystemFlags());

        this.splashScreen = SplashScreen.installSplashScreen(this);


        // ThemeUtils.applyUserPreferredTheme(this);

        if (splashScreen != null) {

            if (isAtLeastAndroid11()) {
                splashScreen.setKeepOnScreenCondition(() -> true);
                // Keep the splash screen on for 10 seconds.
                // This is just for demonstration purposes.
                // In a real app, you would dismiss the splash screen
                // when your app is ready to display its content.
                cancelSplashScreenRunnable = new Runnable() {
                    @Override
                    public void run() {
                        dismissSplashScreen();
                    }
                };
                handler.postDelayed(cancelSplashScreenRunnable, 10_000);
            }

        }


        getWindow().getDecorView(); // workaround for bug in 1.17beta2
        WindowCompat.enableEdgeToEdge(getWindow());


        ThemeUtils.setUserPreferredThemeChangeListener((newTheme)->{
            this.recreate();
        });


        setContentView(R.layout.activity_main);



        ViewModelProvider viewModelProvider = new ViewModelProvider(this);
        this.terminatingViewModel = viewModelProvider.get(TerminatingViewModel.class);
        this.model = viewModelProvider.get(Model.class);

        this.billingModel = viewModelProvider.get(BillingModel.class);


        this.webViewContainer = findViewById(R.id.web_container_view);

        this.rootView = findViewById(R.id.app_main_frame);
        assert rootView != null;

        this.mainContent = findViewById(R.id.main_content);
        assert mainContent != null;


        updateSystemBarVisibility();

        //setIgnoreInsets(this.rootView);
        handleUiWindowInsets(this.rootView);

        rootView.requestApplyInsets();

        if (isAtLeastAndroid11()) {
              animateKeyboardDisplay();
        }

        if (savedInstanceState != null)
        {
            this.rationaleShown = savedInstanceState.getBoolean(KEY_RATIONALE_SHOWN,false);
            this.showingSponsorship = savedInstanceState.getBoolean(KEY_SHOW_SPONSORSHIP,false);
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

        setNormalStatusBar();


        maybeRequestPermissions();


        this.backPressedCallback =new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPressed();
            }
        };

        this.getOnBackPressedDispatcher().addCallback(
                backPressedCallback
        );
    }

    @Override
    protected void onStart() {
        cancelDisconnectAlarm();
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();

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
    private static final String KEY_SHOW_SPONSORSHIP = "show_sponsorship";
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_RATIONALE_SHOWN,this.rationaleShown);
        outState.putBoolean(KEY_SHOW_SPONSORSHIP,this.showingSponsorship);
    }

    @Override
    protected void onDestroy() {


        ThemeUtils.setUserPreferredThemeChangeListener(null);
        dismissSplashScreen();
        cancelSplashScreenTimer();

        if (isFinishing())
        {
            handler.postDelayed(cancelSplashScreenRunnable, 10_000);
            cancelDisconnectAlarm();
            model.stopScan();
        }
        super.onDestroy();
    }

    public void setShowSponsorship(boolean value)
    {
        this.showingSponsorship = value;
        reloadFragments();
    }

}