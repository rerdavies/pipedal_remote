package com.twoplay.pipedal;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Tone3000DialogFragment extends DialogFragment {

    public static final String TAG = "Tone3000Dialog";
    public static final String URL_ARG = "url";
    public static final String UPLOAD_DIRECTORY_ARG = "upload_directory";
    private WebView webView;


    public static Tone3000DialogFragment newInstance(Fragment parent, String url, String uploadDirectory) {
        Tone3000DialogFragment dlg = new Tone3000DialogFragment();
        Bundle args = new Bundle();
        args.putString(URL_ARG, url);
        args.putString(UPLOAD_DIRECTORY_ARG, uploadDirectory);
        dlg.setArguments(args);
        dlg.show(parent.getChildFragmentManager(), TAG);

        return new Tone3000DialogFragment();
    }

    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> uploadMessage;

    private String url;
    private String uploadDirectory;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.Theme_PiPedal_DarkMode);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout
        View view = inflater.inflate(R.layout.tone3000_dialog_fragment, container, false);

        this.webView = view.findViewById(R.id.web_view);
        prepareWebView(webView);
        var rootView = view.findViewById(R.id.rootWindow);
        ImageButton closeView = view.findViewById(R.id.close_button);
        closeView.setOnClickListener((View v)->{
            dismiss();
        });
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            var statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            var ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            var nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());

            ViewGroup.MarginLayoutParams mpl = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mpl.topMargin = statusBars.top + nav.top;
            mpl.bottomMargin = statusBars.bottom + nav.bottom + ime.bottom;
            mpl.leftMargin = statusBars.left + nav.left;
            mpl.rightMargin = statusBars.right + nav.right;

            v.setLayoutParams(mpl);
            return insets;
        });


        return view;
    }

    public void onStart() {
        super.onStart();
        // Make dialog full screen
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void prepareWebView(WebView webView) {
        var settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setBlockNetworkLoads(false);

        WebSettings wss = webView.getSettings();
        wss.setJavaScriptEnabled(true);
        wss.setDisabledActionModeMenuItems(WebSettings.MENU_ITEM_SHARE | WebSettings.MENU_ITEM_WEB_SEARCH | WebSettings.MENU_ITEM_PROCESS_TEXT);
        wss.setDomStorageEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            wss.setForceDark(WebSettings.FORCE_DARK_OFF);
        }
        wss.setLoadsImagesAutomatically(true);
        //noinspection deprecation
        wss.setSavePassword(true);

        webView.setWebChromeClient(new WebChromeClient() {

        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                var url = request.getUrl().toString();
                // keep in browser.
                if (url.startsWith("https://www.tone3000.com/search")) {
                    return super.shouldOverrideUrlLoading(view, request);
                }
                // launch in external browser.
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                if (getActivity() != null) {
                    getActivity().startActivity(intent);
                }
                return true;
            }
        });
        //noinspection Convert2Lambda
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                        String mimeType, long contentLength) {
                handleDownload(url, contentDisposition, mimeType, contentLength);
            }
        });
        webView.setNetworkAvailable(true);
        webView.addJavascriptInterface(downloadCallbacks, "piPedalDownloader");

    }

    private String generateFilename(String contentDisposition, String mimeType) {
        // Try to extract filename from content disposition
        if (contentDisposition != null && contentDisposition.contains("filename=")) {
            String filename = contentDisposition.substring(contentDisposition.indexOf("filename=") + 9);
            if (filename.startsWith("\"") && filename.endsWith("\"")) {
                filename = filename.substring(1, filename.length() - 1);
            }
            return filename;
        }

        // Generate filename based on MIME type
        String extension = ".zip";
        return "download_" + System.currentTimeMillis() + extension;
    }

    class DownloaderCallbacks {

        DownloaderTask getTask(int handle) {
            if (currentDownloaderTask == null || currentDownloaderTask.getHandle() != handle)
            {
                return null;
            }
            return currentDownloaderTask;

        }
        @JavascriptInterface
        public void log(String message) {
            Log.i(TAG,message);
        }

        @JavascriptInterface
        public void startDownload(int handle) {
            DownloaderTask task = getTask(handle);
            if (task != null) {
                task.onStartDownload();
            }
        }
        @JavascriptInterface
        public void write(int handle, String data, double offset) {
            DownloaderTask task = getTask(handle);
            if (task != null) {
                task.onWrite(data, offset);
            }
        }
        @JavascriptInterface
        public void endDownload(int handle) {
            DownloaderTask task = getTask(handle);
            if (task != null) {
                task.onEndDownload();
            }

        }
        @JavascriptInterface
        public void error(int handle, String message) {
            ErrorDialogFragment.execute(Tone3000DialogFragment.this,message,"Download error");
        }

    }

    private String readStringFromResource(int resId)
    {
        try (InputStream inputStream = getResources().openRawResource(resId)) {
            byte[] buffer = new byte[inputStream.available()];
            //noinspection ResultOfMethodCallIgnored
            inputStream.read(buffer);
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.e(TAG, "Error reading resource: " + resId, e);
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Error reading resource: " + resId, Toast.LENGTH_LONG).show();
            });
            return "";
        }
    }
    DownloaderCallbacks downloadCallbacks = new DownloaderCallbacks();
    private void prepareForDownload() {
        //webView.addJavascriptInterface(downloadCallbacks, "piPedalDownloader");
        String jsCode;
        try {
            //noinspection CharsetObjectCanBeUsed
            jsCode = readStringFromResource(R.raw.tone3000_downloader);
        } catch (Exception e)
        {
            jsCode = ""; // should not happen.
        }
        webView.evaluateJavascript(jsCode, (result) -> {
            Log.d(TAG, "Download preparation complete.");
        }
        );



    }

    private static int downloadTaskHandle = 1;
    private static class DownloaderTask {
        public DownloaderTask(String url, String contentDisposition, String mimeType) {
            this.handle = downloadTaskHandle++;
            this.url = url;
            this.contentDisposition = contentDisposition;
            this.mimeType = mimeType;
        }
        public void release() {

        }
        private int handle;
        private String url;
        private String contentDisposition;
        private String mimeType;

        public int getHandle() {
            return handle;
        }

        public String getUrl() {
            return url;
        }

        public String getContentDisposition() {
            return contentDisposition;
        }

        public String getMimeType() {
            return mimeType;
        }
        void onStartDownload()
        {

        }
        void onWrite(String data, double offset) {

        }
        void onEndDownload() {

        }
    }
    private DownloaderTask currentDownloaderTask = null;

    private void handleBlobDownload(String blobUrl, String contentDisposition, String mimeType) {

        // JavaScript code to convert blob to base64
        prepareForDownload();

        currentDownloaderTask = new DownloaderTask(blobUrl,contentDisposition,mimeType);

        String jsCode = "piPedal_download_blob('"
                + blobUrl
                + "', '" + mimeType +
                "', " + currentDownloaderTask.getHandle() + ")";
        webView.evaluateJavascript(jsCode, null);
    }
    private void handleDownload(String url, String contentDisposition, String mimeType, long contentLength) {
        // The Tone3000 server is badly behaved. It frequently gets the contentDisposition wrong.
        // So we will ignore it.
        if (url.startsWith("blob:")) {
            handleBlobDownload(url, contentDisposition, mimeType);
            return;
        }

        throw new RuntimeException("Not implemented.");
//        String extension = ".unknown";
//        if (mimeType != null)
//        {
//            if (mimeType.equals("application/zip"))
//            {
//                extension = ".zip";
//            }
//        }
//        String filename = UUID.randomUUID().toString() + extension;
//
//        File directory = requireContext().getExternalFilesDir("downloads");
//        File file = new File(directory,filename);
//
//        new AsyncTask<Void,Void,Void>()
//        {
//            public AsyncTask
//            @Override
//            protected Void doInBackground(Void... voids) {
//                try {
//                    URL u = new URL(url);
//                    try (InputStream inputStream = u.openStream())
//                    {
//                        try (OutputStream outputStream = new FileOutputStream(file))
//                        {
//                            byte[] buffer = new byte[8192];
//                            int bytesRead;
//                            while ((bytesRead = inputStream.read(buffer)) != -1) {
//                                outputStream.write(buffer,0,bytesRead);
//                            }
//                        }
//                    }
//                    requireActivity().runOnUiThread(() -> {
//                        Toast.makeText(getContext(), "Download complete: " + filename, Toast.LENGTH_LONG).show();
//                    });
//
//                } catch (IOException e) {
//                    Log.e(TAG,"Error downloading file.",e);
//                    requireActivity().runOnUiThread(() -> {
//                        Toast.makeText(getContext(), "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
//                    });
//                }
//                return null;
//            }
//        }.execute();
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        webView.loadUrl("https://www.tone3000.com/search");

    }
}

