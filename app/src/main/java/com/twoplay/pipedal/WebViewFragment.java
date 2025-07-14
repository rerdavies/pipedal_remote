package com.twoplay.pipedal;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.twoplay.pipedal.model.Model;
import com.twoplay.pipedal.model.WebProbe;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.UnknownHostException;

public class WebViewFragment extends Fragment {

    private static final String WEB_URL_EXTRA = "web_url";
    private static final String TAG = "WebViewFrag";
    private PiPedalViewViewModel mViewModel;
    private WebView webView;

    private ValueCallback<Uri> mUploadMessage;
    public ValueCallback<Uri[]> uploadMessage;
    public static final int REQUEST_SELECT_FILE = 100;
    private final static int FILE_CHOOSER_RESULT_CODE = 1;

    private Model mModel;
    private String url = "";

    private boolean retainedWebView = false;
    private MaterialToolbar appBar;
    private TextView addressTextView;


    static class ImeInfo {
        public ImeInfo(Rect containerPosition, int imeHeight) {
            this.containerPosition = containerPosition;
            this.imeHeight = imeHeight;
        }
        public Rect containerPosition;
        public int imeHeight;
    }
    ImeInfo imeInfo = null;

    static class FocusBoundsInfo {
        public FocusBoundsInfo(float left, float top, float right, float bottom, float windowWidth, float windowHeight) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.windowWidth = windowWidth;
            this.windowHeight = windowHeight;
        }

        public float left;
        public float top;
        public float right;
        public float bottom;
        public float windowWidth;
        public float windowHeight;
    }
    interface FocusedElementBoundsCallback {
        void onComplete(FocusBoundsInfo bounds);
    }

    private void getFocusedElementBounds(FocusedElementBoundsCallback callback)
    {
        String javascript = "javascript:getFocusedElementBounds()";
        webView.evaluateJavascript(javascript,value-> {
            try {
                if (value.equals("null")) {
                    callback.onComplete(null);
                    return;
                }
                JSONObject object = (JSONObject) new JSONTokener(value).nextValue();

                double left = object.getDouble("left");
                double top = object.getDouble("top");
                double right = object.getDouble("right");
                double bottom = object.getDouble("bottom");
                double windowWidt = object.getDouble("windowWidth");
                double windowHeight = object.getDouble("windowHeight");
                callback.onComplete(new FocusBoundsInfo((float) left,
                        (float) top,
                        (float) right,
                        (float) bottom,
                        (float) windowWidt,
                        (float) windowHeight));
                return;

            } catch (Exception ignored)
            {
            }
            callback.onComplete(null);
        });


    }

    private FocusBoundsInfo focusBoundsInfo = null;
    public void adjustWebviewForKeyboard()
    {
        int dy = 0;
        if (this.imeInfo != null)
        {
            Log.i(TAG,"IME Rect: " + this.imeInfo.containerPosition.toString() + "ime height: " + imeInfo.imeHeight);
        }
        if (this.focusBoundsInfo != null && this.imeInfo != null )
        {

            int webViewHeight = this.webView.getHeight();
            final float dpToPix = getContext().getResources().getDisplayMetrics().density;
            double margin = 64*dpToPix;
            double webToPxScale = webViewHeight/this.focusBoundsInfo.windowHeight;

            double keyboardTop = webViewHeight - this.imeInfo.imeHeight;

            double focusTop = this.focusBoundsInfo.top * webToPxScale; // top of element in webV    iew pixels.
            double focusBottom = this.focusBoundsInfo.bottom * webToPxScale; // bottom of element in webView pixels.

            double translatedBottom = focusBottom;
            if (translatedBottom+margin > keyboardTop) {
                translatedBottom = keyboardTop - margin;
            }
            double translatedTop = translatedBottom - (focusBottom - focusTop);
            if (translatedTop < margin) {
                // center in available space.
                translatedTop = (keyboardTop/2) -(focusBottom-focusTop)/2;
            }
            dy = (int)(translatedTop-focusTop);
            if (webViewHeight+dy < keyboardTop)
            {
                dy = (int)(keyboardTop-webViewHeight);
            }
            if (dy > 0) dy = 0;


        }
        webView.setTranslationY(dy);

//        var lp = webView.getLayoutParams();
//        if (!(lp instanceof FrameLayout.LayoutParams)){
//           throw new RuntimeException("WebView parent is not a constraint layout.");
//        }
//        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)lp;
//        if (layoutParams.topMargin != dy) {
//            layoutParams.topMargin = dy;
//            layoutParams.bottomMargin = -dy;
//            webView.setLayoutParams(layoutParams);
//        }
    }

    public void setImeInsets(Rect windowPosition, int imeHeight) {
        var oldHeight = this.imeInfo != null? this.imeInfo.imeHeight : 0;
        this.imeInfo = new ImeInfo(windowPosition,imeHeight);
        // starting to show ime?
        if (oldHeight == 0 && imeHeight != 0)
        {
            // then asynchronously request the bounds of the focused element from the broser.
            this.getFocusedElementBounds((bounds) -> {
                this.focusBoundsInfo = bounds;
                this.adjustWebviewForKeyboard();
            });
        }
        this.adjustWebviewForKeyboard();
    }


    public interface ShowSponsorshipListener {
        void onShowSponsorship();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(getActivity());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                onUrlLoaded(url);
            }
        });
        prepareWebView(webView);
    }

    public boolean NavigateBack() {
        if (webView == null) return false;
        if (webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return false;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void prepareWebView(WebView webView) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().getAllowContentAccess();
        WebSettings wss = webView.getSettings();
        wss.setJavaScriptEnabled(true);
        wss.setDisabledActionModeMenuItems(WebSettings.MENU_ITEM_SHARE | WebSettings.MENU_ITEM_WEB_SEARCH | WebSettings.MENU_ITEM_PROCESS_TEXT);
        wss.setDomStorageEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            wss.setForceDark(WebSettings.FORCE_DARK_OFF);
        }
        wss.setLoadsImagesAutomatically(true);
        //noinspection deprecation
        wss.setSavePassword(false);

        webCallbacks.hostVersion = BuildConfig.VERSION_NAME;
        webView.addJavascriptInterface(webCallbacks, "AndroidHost");

        webView.setWebChromeClient(new WebChromeClient() {
            // For 3.0+ Devices (Start)
            // onActivityResult attached before constructor
            protected void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*");
                //noinspection deprecation
                startActivityForResult(Intent.createChooser(i, "File Browser"), FILE_CHOOSER_RESULT_CODE);
            }


            // For Lollipop 5.0+ Devices
            public boolean onShowFileChooser(WebView mWebView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);

                }

                uploadMessage = filePathCallback;

                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, REQUEST_SELECT_FILE);
                } catch (ActivityNotFoundException e) {
                    uploadMessage = null;
                    Toast.makeText(getActivity().getApplicationContext(), "Cannot Open File Chooser", Toast.LENGTH_LONG).show();
                    return false;
                }
                return true;
            }

            //For Android 4.1 only
            protected void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                mUploadMessage = uploadMsg;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "File Browser"), FILE_CHOOSER_RESULT_CODE);
            }

            protected void openFileChooser(ValueCallback<Uri> uploadMsg) {
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                startActivityForResult(Intent.createChooser(i, "File Chooser"), FILE_CHOOSER_RESULT_CODE);
            }
        });


    }

    private FrameLayout webViewFrame;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_web_view, container, false);

        webViewFrame = (FrameLayout) (v.findViewById(R.id.web_view_container));
        webViewFrame.addView(webView);

        addressTextView = (TextView) v.findViewById(R.id.address_view);

        this.loadingProgressView = v.findViewById((R.id.web_view_loading_progress));
        this.appBar = (MaterialToolbar) v.findViewById(R.id.app_bar);
        this.appBar.setNavigationOnClickListener((view) -> {
            disconnectAndFinish();
        });

        //noinspection deprecation because we're retaining a webView.
        setRetainInstance(true);

        this.mModel = new ViewModelProvider(requireActivity()).get(Model.class);
        this.mModel.setPageUnloadListener(() -> {
            return unloadPage();
        });

        this.showPageLoading_ = mModel.showPageLoading();
        this.loadingProgressView.setVisibility(
                showPageLoading() ? View.VISIBLE : View.GONE
        );


        ApplicationInfo appInfo = requireActivity().getApplicationInfo();

        if (0 != (appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        // if we were recreated from CreateView (i.e. WebView was retained) do NOT reload.

        // if we were recreated from Create, the web view was not retained. reload the URL.
        if (savedInstanceState != null) {
            if (!retainedWebView) {
                this.url = savedInstanceState.getString(WEB_URL_EXTRA, "");
                if (!url.isEmpty()) {
                    webView.loadUrl(url);
                }
            }
        }
        retainedWebView = true;

// Whitelist our url?
//        Uri webUri = Uri.parse(url);
//        String hostName = webUri.getHost();
//
//        ArrayList<String> hosts = new ArrayList<>();
//        hosts.add(hostName);
//        webView.setSafeBrowsingWhitelist(hosts,(c)->{

        return v;
    }

    private boolean showPageLoading_ = false;

    private boolean showPageLoading() {
        return showPageLoading_;
    }

    private Handler handler = new Handler();

    Runnable removeLoadingRunnable = new Runnable() {
        @Override
        public void run() {
            showPageLoading(false);
        }
    };

    private void showPageLoading(boolean value) {
        if (showPageLoading_ != value) {
            showPageLoading_ = value;
            if (mModel != null) {
                mModel.showPageLoading(value);
            }
            if (loadingProgressView != null) {
                loadingProgressView.setVisibility(value ? View.VISIBLE : View.GONE);
            }
            if (value) {
                handler.postDelayed(removeLoadingRunnable, 20 * 1000);
            } else {
                handler.removeCallbacks(removeLoadingRunnable);
            }
        }
    }

    @Override
    public void onDetach() {
        showPageLoading(false);
        super.onDetach();
        ViewGroup parent = (ViewGroup) webView.getParent();
        if (parent != null) {
            parent.removeView(webView);
        }
    }

    private void disconnectAndFinish() {
        if (getActivity() != null) {
            if (mModel != null) {
                mModel.p2pDisconnect(() -> {
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                });
            } else {
                getActivity().finish();
            }
        }

    }

    private boolean hasRouting(InetAddress address)
    {
        NetworkInterface networkInterface = NetworkHelper.getMatchingInterface(address);
        return networkInterface != null;
    }

    private void returnToDeviceSearch()
    {
        handler.post(() -> {
            url = ""; // even if we get the same device, reload the page.
            if (getActivity() == null) {
                deferredChooseNewDevice = true;
            } else {
                mModel.webCallbackChooseNewDevice(getActivity());
            }
        });
    }
    private Promise<Boolean> asyncWaitForRouting(final String url_) {
        // wait for the p2p connection to have an IP4 address. It takes some time for DHCP and routing to complete on a shiny new connection.
        // This avoids routing bizareness when wifi or data connections are disabled (e.g. sending our web requests out on an phone data connection!)


        Promise<Boolean> result = Promise.asyncExec(
            handler,
            (continuation)-> {
                URL url = null;
                try {
                    url = new URL(url_);
                } catch (Exception e)
                {
                    Log.e("asyncWaitForRouting",e.toString());
                    continuation.fulfill(true);
                    return;
                }

                InetAddress inetAddress = null;
                try {
                    inetAddress = InetAddress.getByName(url.getHost());
                } catch (UnknownHostException e) {
                    Log.e("asyncWaitForRouting",e.toString());
                    continuation.fulfill(true);
                    return;
                }
                // Only need to wait for routing to settle if it's a 172.24.xx.xx address
                if (inetAddress instanceof Inet4Address)
                {
                    Inet4Address inet4Address = (Inet4Address) inetAddress;
                    byte[] address = inet4Address.getAddress();
                    if (address[0] != 172 || address[1] != 24) {
                        continuation.fulfill(true);
                        return;
                    }

                }
                int port = url.getPort();


                int MAX_DELAY_MS = 40000;
                int DELAY_TIME = 100;
                for (int retry = 0; retry < MAX_DELAY_MS/DELAY_TIME; ++retry) {
                    if (hasRouting(inetAddress)) {
                        continuation.fulfill(true);
                        return;
                    }
                    try {
                        Thread.sleep(DELAY_TIME);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                continuation.fulfill(false);

            }
        );
        return result;
    }


    public void setUrl(String connectionAddress) {

        // convert to canonical webView form.
        if (connectionAddress.endsWith(":80"))
        {
            connectionAddress = connectionAddress.substring(0,connectionAddress.length()-3);
        }
        if (addressTextView != null)
        {
            addressTextView.setText(connectionAddress);
        }
        // the theory is that it takes a while for the network to be reconfigured after the p2p connection connects.
        // so probe for the website, and delay if neccessary.

        final String myConnectionAddress = connectionAddress;
        showPageLoading(true);

        asyncWaitForRouting(myConnectionAddress).andThen((hasRouting)-> {
            if (!hasRouting)
            {

                mModel.showError("Connection doesn't have an IP address.","Error");
                returnToDeviceSearch();
                return;
            }
            Log.d(TAG,"asyncWaitForRouting: Web address has routing.");
            Promise<Boolean> siteCheckPromise = Promise.asyncExec(
                    handler,
                    (completion) -> {
                        int retries = 0;
                        while (true) {
                            try {
                                boolean result = WebProbe.checkForPiPedalWebsite(myConnectionAddress);
                                Log.d(TAG,"WebProbe: site is a PiPedal website.");
                                completion.fulfill(result);
                                return;
                            } catch (Exception e) {
                                if (retries++ == 20) {
                                    completion.reject(e);
                                    return;
                                }
                            }
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                break;
                            }

                        }
                    }
            );
            siteCheckPromise.andThen(
                    (result) -> {
                        webView.setNetworkAvailable(true);
                        if (!result) {
                            mModel.showError("Website doesn't appear to be a PiPedal website.", "Error");
                        }
                        if (!this.url.equals(myConnectionAddress)) {
                            this.url = myConnectionAddress;
                            String lastUrl = this.webView.getUrl();
                            webView.clearHistory();

                            Log.i(TAG, "WebView: Load " + this.url);
                            this.webView.loadUrl(myConnectionAddress);

                            // force a reload even if the url is the same. (WebView won't reload in this case unless we force it)
                            String thisUrl = this.webView.getUrl(); // a normalized version of our own url
                            if (lastUrl != null) {
                                if (lastUrl.equals(thisUrl)) {
                                    webView.reload();
                                }
                            }
                        } else {
                            showPageLoading(false);
                        }
                    }
            ).andCatch((exception2) ->
            {
                mModel.showError(exception2.getMessage(), "Error");
                if (getActivity() == null) {
                    deferredChooseNewDevice = true;
                } else {
                    mModel.webCallbackChooseNewDevice(getActivity());
                }
            });
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        checkForDeferredActions();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(WEB_URL_EXTRA, this.url);
    }


    private View loadingProgressView;
    private boolean deferredChooseNewDevice = false;
    private boolean deferredLostConnection = false;
    private boolean bgIsDisconnected = false;

    private void checkForDeferredActions() {
        if (deferredChooseNewDevice) {
            mModel.webCallbackChooseNewDevice(getActivity());
        } else if (deferredLostConnection) {
            if (!getActivity().isFinishing()) {
                mModel.webCallbackOnLostConnection(getActivity(), bgIsDisconnected);
            }
        }
        deferredLostConnection = false;
        deferredChooseNewDevice = false;
    }

    private class WebCallbacks {

        String hostVersion = "0.1";

        @JavascriptInterface
        public boolean isAndroidHosted() {
            Log.d(TAG,"isAndroidHosted() callback.");

            return true;
        }

        @JavascriptInterface
        public void showSponsorship() {
            handler.post(() -> {
                ShowSponsorshipListener activityCallback = (ShowSponsorshipListener) getActivity();
                activityCallback.onShowSponsorship();
            });
        }

        private int themePreference = ThemeUtils.getUserPreferedTheme().toInt();
        @JavascriptInterface
        public void setThemePreference(final int value) {
            themePreference = value;
            handler.post(() -> {
                ThemeUtils.setUserPreferredTheme(ThemeUtils.ColorTheme.fromInt(value));
            });
        }
        @JavascriptInterface
        public int getThemePreference() {

            return themePreference;

        }

        @JavascriptInterface
        public boolean isDarkTheme()
        {
            return ThemeUtils.isDarkModeEnabled(getContext());
        }


        @JavascriptInterface
        public String getHostVersion() {
            Log.d(TAG,"getHostVersion() callback.");
            return "Android host: v" + hostVersion;
        }

        @JavascriptInterface
        public void chooseNewDevice() {
            handler.post(() -> {
                url = ""; // even if we get the same device, reload the page.
                if (getActivity() == null) {
                    deferredChooseNewDevice = true;
                } else {
                    mModel.webCallbackChooseNewDevice(getActivity());
                }
            });
        }

        @JavascriptInterface
        public void setDisconnected(boolean disconnected) {
            handler.post(() -> {
                bgIsDisconnected = disconnected;
                if (getActivity() == null) {
                    deferredLostConnection = true;
                } else {
                    if (!getActivity().isFinishing()) {
                        mModel.webCallbackOnLostConnection(getActivity(), bgIsDisconnected);
                    }
                }
            });
        }
        private String[] whiteList  = {
                "https://rerdavies.github.io/pipedal",
                "https://github.com/rerdavies/pipedal"
        };
        @JavascriptInterface
        public boolean launchExternalUrl(String url)
        {
            boolean whiteListed = false;
            for (int i = 0; i < whiteList.length; ++i)
            {
                if (url.startsWith(whiteList[i]))
                {
                    whiteListed = true;
                    break;
                }
            }
            if (whiteListed)
            {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                if (getActivity() != null) {
                    getActivity().startActivity(intent);
                }
                return true;
            }
            return false;

        }
    }

    private interface PageUnloadedListener {
        void onPageUnloaded();
    }

    PageUnloadedListener pageUnloadedListener;
    private void onUrlLoaded(String url) {
        Log.i(TAG, "WebView: Loaded " + this.url);
        if ("about:blank".equals(url) && pageUnloadedListener != null) {
            pageUnloadedListener.onPageUnloaded();
            pageUnloadedListener = null;
            showPageLoading(true); // hide white flash in dark mode.

        } else {
            showPageLoading(false);
            try {
                URL url_ = new URL(url);
                String ref = url_.getRef();
                if (ref == null || ref.isEmpty() || ref.equals("#"))
                {
                    webView.clearHistory();
                }
            } catch (MalformedURLException ignored) {
            }
        }
    }

    public Promise<Void> unloadPage() {
        return new Promise<Void> ((continuation) -> {

            if (webView == null) {
                continuation.fulfill(null);
            } else {
                pageUnloadedListener = new PageUnloadedListener() {
                    @Override
                    public void onPageUnloaded() {
                        continuation.fulfill(null);
                    }
                };
                Log.i(TAG, "WebView: page unloaded.");

                webView.loadUrl("about:blank");
            }
        });
    }


    WebCallbacks webCallbacks = new WebCallbacks();

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            if (requestCode == REQUEST_SELECT_FILE)
            {
                if (uploadMessage == null)
                    return;
                uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
                uploadMessage = null;
            }
        }
        else if (requestCode == FILE_CHOOSER_RESULT_CODE)
        {
            if (null == mUploadMessage)
                return;
            // Use MainActivity.RESULT_OK if you're implementing WebView inside Fragment
            // Use RESULT_OK only if you're implementing WebView inside an Activity
            Uri result = intent == null || resultCode != MainActivity.RESULT_OK ? null : intent.getData();
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        }
        else
            Toast.makeText(getActivity().getApplicationContext(), "Failed to Upload Image", Toast.LENGTH_LONG).show();

    }

    @SuppressWarnings("deprecation")
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(PiPedalViewViewModel.class);
    }

}