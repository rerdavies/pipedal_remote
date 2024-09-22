package com.twoplay.pipedal;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

/**
 * Copyright (c) 2015, sRobin Davies
 * Created by Robin on 11/04/2022.
 */
public class ErrorDialogFragment extends DialogFragment {
    private static String MESSAGE_EXTRA = "message";
    private static String TITLE_EXTRA = "title";

    public static void execute(Fragment parent, String message, String title)
    {
        Bundle bundle = new Bundle();
        bundle.putString(MESSAGE_EXTRA,message);
        bundle.putString(TITLE_EXTRA,title);
        ErrorDialogFragment dlg = new ErrorDialogFragment();
        dlg.setArguments(bundle);
        dlg.show(parent.getChildFragmentManager(),TAG);

    }
    public static void execute(Activity activity, String message, String title)
    {

        Bundle bundle = new Bundle();
        bundle.putString(MESSAGE_EXTRA,message);
        bundle.putString(TITLE_EXTRA,title);
        ErrorDialogFragment dlg = new ErrorDialogFragment();
        dlg.setArguments(bundle);

        FragmentActivity fragmentActivity = (FragmentActivity)activity;
        dlg.show(
                fragmentActivity.getSupportFragmentManager(),TAG);
    }

    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = this.getArguments();
        String title = args.getString(TITLE_EXTRA);
        String message = args.getString(MESSAGE_EXTRA);

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {} )
                .create();
    }
    public static String TAG = "ErrorDialog";
}
