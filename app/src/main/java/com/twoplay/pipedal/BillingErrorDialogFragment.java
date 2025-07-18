package com.twoplay.pipedal;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

/**
 * Copyright (c) 2015, sRobin Davies
 * Created by Robin on 11/04/2022.
 */
public class BillingErrorDialogFragment extends DialogFragment {
    private static String MESSAGE_EXTRA = "message";
    private static String TITLE_EXTRA = "title";

    public static void execute(Fragment parent, String message, String title)
    {
        Bundle bundle = new Bundle();
        bundle.putString(MESSAGE_EXTRA,message);
        bundle.putString(TITLE_EXTRA,title);
        BillingErrorDialogFragment dlg = new BillingErrorDialogFragment();
        dlg.setArguments(bundle);
        dlg.show(parent.getChildFragmentManager(),TAG);

    }
    public static void execute(Activity activity, String message, String title)
    {

        Bundle bundle = new Bundle();
        bundle.putString(MESSAGE_EXTRA,message);
        bundle.putString(TITLE_EXTRA,title);
        BillingErrorDialogFragment dlg = new BillingErrorDialogFragment();
        dlg.setArguments(bundle);

        FragmentActivity fragmentActivity = (FragmentActivity)activity;
        dlg.show(fragmentActivity.getSupportFragmentManager(),TAG);
    }

    @NonNull
    @SuppressLint("DialogFragmentCallbacksDetector")
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = this.getArguments();
        String title = args.getString(TITLE_EXTRA);
        String message = args.getString(MESSAGE_EXTRA);

        return new MaterialAlertDialogBuilder(requireContext())
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    onClosed();
                } )
                .create();
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        onClosed();
    }

    private void onClosed() {
    }
    public static String TAG = "BillingErrorDialog";
}
