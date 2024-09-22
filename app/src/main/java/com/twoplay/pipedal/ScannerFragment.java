package com.twoplay.pipedal;

import android.animation.ObjectAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.twoplay.pipedal.model.ConnectionType;
import com.twoplay.pipedal.model.Model;
import com.twoplay.pipedal.model.PiPedalConnection;
import com.twoplay.pipedal.model.ScanState;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

public class ScannerFragment extends Fragment {

    private Model mModel;
    private ConstraintLayout searchingView;
    private RecyclerView recyclerView;
    private MaterialToolbar appBar;
    private DeviceAdapter adapter;
    private ConstraintLayout errorView;
    private TextView errorTextView;
    private TextView captionView;
    private TextView searchingTextView;
    private MaterialButton wifiSettingsButton;
    MaterialButton scanButton;
    MaterialButton cancelButton;


    private void showCancel(boolean show)
    {
        if (scanButton != null) {
            scanButton.setVisibility(show ? View.GONE: View.VISIBLE);
        }
        if (cancelButton != null)
        {
            cancelButton.setVisibility(show? View.VISIBLE: View.GONE);
        }
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        PackageManager packageManager = getActivity().getPackageManager();
        Intent wifiSettingsIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        List<ResolveInfo> activities = packageManager.queryIntentActivities(wifiSettingsIntent, 0);
        boolean hasWifiSettingsHandler = !activities.isEmpty();

        View v = inflater.inflate(R.layout.scanner_fragment, container, false);
        this.captionView = (TextView) v.findViewById(R.id.caption);
        this.searchingView = (ConstraintLayout) v.findViewById(R.id.searching_panel);
        this.searchingTextView = (TextView) v.findViewById(R.id.searching_text);
        this.errorView = (ConstraintLayout) v.findViewById(R.id.error_panel);
        this.recyclerView = (RecyclerView) v.findViewById(R.id.recycler_view);
        this.appBar = (MaterialToolbar) v.findViewById(R.id.app_bar);
        this.errorTextView = (TextView) v.findViewById(R.id.error_text);
        wifiSettingsButton = (MaterialButton)v.findViewById(R.id.wifi_button);
        scanButton = v.findViewById(R.id.scan_again_button);
        cancelButton = v.findViewById(R.id.cancel_button);

        if (hasWifiSettingsHandler) {
            wifiSettingsButton.setOnClickListener((View vv) -> {
                Intent launchIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                getActivity().startActivity(launchIntent);

            });
        } else {
            wifiSettingsButton.setVisibility(View.GONE);
        }
        scanButton.setOnClickListener((View vv) -> {
            mModel.getDeviceScanner().restartScan();
        });
        cancelButton.setOnClickListener((View vv) -> {
            mModel.getDeviceScanner().stopScan();
        });
        showCancel(false);
        MaterialButton helpButton = v.findViewById(R.id.help_button);
        helpButton.setOnClickListener((View v3) -> {
            HelpDialogFragment.execute(this);
        });

        appBar.inflateMenu(R.menu.scanner_menu);
        appBar.setOnMenuItemClickListener((MenuItem item) -> {
//            if (item.getItemId() == R.id.menu_refresh)
//            {
//                refreshDevices();
//                return true;
//            }
            return false;
        });
        appBar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        appBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectAndFinish();
            }
        });


        return v;
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

    private void refreshDevices() {
        mModel.getDeviceScanner().restartScan();
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {

        private final TextView textView;
        private final String[] statusStrings;
        private final ImageView wifiIcon;

        PiPedalConnection piPedalConnection;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);

            itemView.setOnClickListener((View v) -> {
                ScannerFragment.this.onConnectionClicked(piPedalConnection);
            });
            statusStrings = itemView.getContext().getResources().getStringArray(R.array.connection_status);
            wifiIcon = (ImageView) itemView.findViewById(R.id.wifi_icon);
            this.textView = (TextView) itemView.findViewById(R.id.primary_text);
        }

        void bindTo(PiPedalConnection deviceConnection) {
            this.piPedalConnection = deviceConnection;
            textView.setText(deviceConnection.getDisplayName());
            int ridIcon = R.drawable.ic_wifi_normal_black_24dp;
            wifiIcon.setImageResource(ridIcon);

        }
    }

    private void onConnectionClicked(PiPedalConnection piPedalConnection) {
        switch (piPedalConnection.getStatus()) {
            case NotConnected:
            case Failed:
                return;
            case AvailableOnLocalNetwork:
            case Connected:
                mModel.setConnection(piPedalConnection);
                break;
            case Connecting:
            case WaitingForIpAddress:
            case ConnectedNoServiceAddress:
                promptForCancelInvitation(piPedalConnection);
                break;
        }
    }

    void promptForCancelInvitation(final PiPedalConnection piPedalConnection) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage("Do you want to cancel the connection attempt?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cancelInvitation(piPedalConnection);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setCancelable(true);

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    void cancelInvitation(final PiPedalConnection piPedalConnection)
    {
    }

    public static final DiffUtil.ItemCallback<PiPedalConnection> DIFF_CALLBACK = new DiffUtil.ItemCallback<PiPedalConnection>() {
        @Override
        public boolean areItemsTheSame(@NonNull PiPedalConnection oldDevice, @NonNull PiPedalConnection newDevice) {
            return oldDevice.isSameDevice(newDevice);                    // User properties may have changed if reloaded from the DB, but ID is fixed
        }

        @Override
        public boolean areContentsTheSame(@NonNull PiPedalConnection oldDevice, @NonNull PiPedalConnection newDevice) {
            return oldDevice.equals(newDevice);
        }
    };

    class DeviceAdapter extends ListAdapter<PiPedalConnection, DeviceViewHolder> {
        public DeviceAdapter() {
            super(DIFF_CALLBACK);
            setHasStableIds(true);
        }

        @NonNull
        @Override
        public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item, parent, false);
            return new DeviceViewHolder(view);
        }


        @Override
        public long getItemId(int position) {
            return getItem(position).id();
        }

        @Override
        public void onBindViewHolder(DeviceViewHolder holder, int position) {
            holder.bindTo(getItem(position));
        }

        public void onConnectionChanged(PiPedalConnection connection) {
            for (int i = 0; i < getItemCount(); ++i) {
                PiPedalConnection item = getItem(i);
                if (item == connection) {
                    this.notifyItemChanged(i);
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!Preferences.getDontshowHelpAgain(getContext()))
        {
            Preferences.setDontShowHelpAgain(getContext(),true);
            HelpDialogFragment.execute(this);
        }
    }

    private void updateDisplayLayout() {
        ScanState scanState = mModel.scanState.getValue();
        if (mModel.getDeviceScanner() == null) return;
        if (mModel.getDeviceScanner().getPiPedalDevices().getValue() == null) return;
        int nDevices = mModel.getDeviceScanner().getPiPedalDevices().getValue().size();
        if (scanState == ScanState.ErrorState) {
            showSearchingView(false);
            recyclerView.setVisibility(View.GONE);
            errorView.setVisibility(View.VISIBLE);
            showCancel(false);
        } else if (scanState == ScanState.SearchingForInstance) {
            recyclerView.setVisibility(View.GONE);
            captionView.setText(R.string.reconnecting);
            showSearchingView(true);
            searchingTextView.setText(R.string.searching_for_device);
            errorView.setVisibility(View.GONE);
            showCancel(true);

        } else if (scanState == ScanState.Searching) {
            captionView.setText(R.string.select_a_device_to_connect_to);
            errorView.setVisibility(View.GONE);
            if (nDevices == 0)
            {
                recyclerView.setVisibility(View.GONE);
                showSearchingView(true);
                showCancel(true);

            } else {
                recyclerView.setVisibility(View.VISIBLE);
                showSearchingView(false);
                showCancel(false);
            }
        } else {
            captionView.setText(R.string.select_a_device_to_connect_to);
            showSearchingView(false);
            showCancel(false);
            if (nDevices == 0) {
                errorView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                errorTextView.setText(R.string.no_devices_found);
            } else {
                errorView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    ObjectAnimator fadeInAnimator = null;

    private void fadeInAnimate(View targetView) {
        // Set initial alpha to 0 (completely transparent)
        targetView.setAlpha(0f);

        if (fadeInAnimator != null)
        {
            fadeInAnimator.cancel();
            fadeInAnimator = null;
        }

        // Create the ObjectAnimator
        ObjectAnimator animator = ObjectAnimator.ofFloat(targetView, "alpha", 0f, 1f);

        // Set the total duration to 2000 milliseconds (2 seconds)
        animator.setDuration(200);

        // Set a linear interpolator for smooth animation
        animator.setInterpolator(new LinearInterpolator());

        // Set the start delay to 1000 milliseconds (1 second)
        animator.setStartDelay(1000);

        // Start the animation
        animator.start();
        this.fadeInAnimator = animator;
    }
    private void showSearchingView(boolean show) {
        if (show)
        {
            boolean fadeInAnimation = searchingView.getVisibility() != View.VISIBLE;
            searchingView.setVisibility(View.VISIBLE);
            if (fadeInAnimation)
            {
                fadeInAnimate(searchingView);
            }
        } else {
            searchingView.setVisibility(View.GONE);
        }
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.mModel = new ViewModelProvider(requireActivity()).get(Model.class);

        adapter = new DeviceAdapter();

        mModel.getDeviceScanner().setStatusChangedListener((PiPedalConnection connection) -> {
            this.onPiPedalStatusChanged(connection);
        });
        mModel.getDeviceScanner().getPiPedalDevices().observe(this.getViewLifecycleOwner(), list -> {
            if (list.size() != 0) {
                searchingView.setVisibility(View.GONE);
            }
            updateDisplayLayout();
            adapter.submitList(list);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        recyclerView.setAdapter(adapter);

        mModel.scanState.observe(getViewLifecycleOwner(), (value) -> {
            updateDisplayLayout();
        });
        mModel.scanError.observe(getViewLifecycleOwner(), (value) -> {
            errorTextView.setText(value);
            updateDisplayLayout();
        });

    }

    private void onPiPedalStatusChanged(PiPedalConnection connection) {
        adapter.onConnectionChanged(connection);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.scanner_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onStart() {
        super.onStart();
        // checkForIpv6DirectConnection();
    }

    private void checkForIpv6DirectConnection() {
        boolean hasHotspot = false;
        boolean hasDataConnection = false;
        InetAddress hotspotLinkLocalAddress = null;
        String hotspotLinkLocalHostAddress = "";
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface networkInterface : Collections.list(networkInterfaces)) {
                boolean isHotspot = false;
                String interfaceName = networkInterface.getName();
                InetAddress interfaceHotspotLinkLocalAddress = null;
                String interfaceHotspotLinkLocalHostAddress = "";

                if (interfaceName.equals("wlan0")) {
                    List<InterfaceAddress> inetfaceAddresses = networkInterface.getInterfaceAddresses();
                    for (InterfaceAddress interfaceAddress : inetfaceAddresses) {
                        InetAddress address = interfaceAddress.getAddress();
                        if (address != null) {
                            if (address.isLinkLocalAddress()) {
                                interfaceHotspotLinkLocalAddress = address;
                                interfaceHotspotLinkLocalHostAddress = address.getHostAddress();
                            } else if (address instanceof Inet4Address) {
                                Inet4Address inet4Address = (Inet4Address) address;
                                byte[] addressBytes = inet4Address.getAddress();
                                if (addressBytes[0] == 10 && addressBytes[1] == 42 && addressBytes[2] == 0) {
                                    isHotspot = true;
                                }
                            }
                        }
                    }
                    if (isHotspot) {
                        hasHotspot = true;
                        hotspotLinkLocalHostAddress = interfaceHotspotLinkLocalHostAddress;
                        hotspotLinkLocalHostAddress = interfaceHotspotLinkLocalHostAddress;
                    }
                }
            }
            if (hasHotspot)
            {
                new AlertDialog.Builder(getActivity())
                        .setTitle(("Hotspot found."))
                        .setMessage(
                                "llAddress: " + hotspotLinkLocalHostAddress)
                        .setPositiveButton("OK",
                                (DialogInterface dialog, int which)-> {

                                })
                        .create().show();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

}


