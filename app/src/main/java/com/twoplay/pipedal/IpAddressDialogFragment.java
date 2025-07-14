package com.twoplay.pipedal;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Button;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

/**
 * Copyright (c) 2025, Robin E. R. Davies
 * Created by Robin on 11/6/2025.
 */


public class IpAddressDialogFragment extends DialogFragment {
    interface IpAddressDialogFragmentResult {
        void onIpAddressResult(String ipAddress);
    }
    private static String IP_ADDRESS_EXTRA = "ipAddress";

    public static void execute(Fragment parent, String ipAddress)
    {
        Bundle bundle = new Bundle();
        bundle.putString(IP_ADDRESS_EXTRA,ipAddress);
        IpAddressDialogFragment dlg = new IpAddressDialogFragment();
        dlg.setArguments(bundle);
        dlg.show(parent.getChildFragmentManager(),TAG);

    }

    private View okButton;
    private TextInputEditText editText;

    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = this.getArguments();
        String ipAddress = args.getString(IP_ADDRESS_EXTRA);

        LayoutInflater inflater = (LayoutInflater) requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customView = inflater.inflate(R.layout.ip_address_edit, null);


        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(customView)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    var parentCallback = (IpAddressDialogFragmentResult) getParentFragment();
                    assert parentCallback != null;
                    parentCallback.onIpAddressResult(Objects.requireNonNull(editText.getText()).toString());
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                });
        AlertDialog dlg = builder.create();
        return dlg;
    }
    @Override
    public void onStart() {
        super.onStart();

        assert getArguments() != null;
        String text = getArguments().getString(IP_ADDRESS_EXTRA);
        assert text != null;

        AlertDialog dlg = (AlertDialog)getDialog();
        assert dlg != null;
        okButton = dlg.getButton(DialogInterface.BUTTON_POSITIVE);
        assert okButton != null;
        okButton.setEnabled(!text.isEmpty());

        editText = dlg.findViewById(R.id.ip_address_text);
        assert editText != null;

        editText.setText(text);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                okButton.setEnabled(s.length() > 0);
            }
        });
    }
    public static final String TAG = "IpAddressDialog";
}
