package com.twoplay.pipedal;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

/**
 * Copyright (c) 2015, sRobin Davies
 * Created by Robin on 11/04/2022.
 */
public class HelpDialogFragment extends DialogFragment {

    public static void execute(Fragment parent)
    {
        Bundle bundle = new Bundle();
        HelpDialogFragment dlg = new HelpDialogFragment();
        dlg.setArguments(bundle);
        dlg.show(parent.getChildFragmentManager(),TAG);

    }
    public static void execute(Activity activity, String message, String title)
    {

        Bundle bundle = new Bundle();
        HelpDialogFragment dlg = new HelpDialogFragment();
        dlg.setArguments(bundle);

        FragmentActivity fragmentActivity = (FragmentActivity)activity;
        dlg.show(fragmentActivity.getSupportFragmentManager(),TAG);
    }

    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = this.getArguments();

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.help_text_view, null);

        Dialog dlg = new MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_help_24px)
                .setTitle(R.string.help_title)
                .setView(dialogView)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    onClosed();
                } )
                .setOnCancelListener((v)-> {
                    onClosed();
                })
                .create();
        TextView textView = dialogView.findViewById(R.id.help_text);
        String content = readRawResource(R.raw.help_text_html);
        Spanned spanned = IndentTextHandler.Translate(getActivity(), content);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(spanned);
//        WebView webView = dialogView.findViewById(R.id.help_text);
//        String content = readRawResource(R.raw.help_text_html);
//        String encodedHtml = Base64.encodeToString(content.getBytes(),
//                Base64.NO_PADDING);
//        webView.loadData(encodedHtml, "text/html", "base64");
        return dlg;
    }

    private String readRawResource(int resourceId) {
        StringBuilder content = new StringBuilder();
        Resources resources = getResources();

        try (InputStream inputStream = resources.openRawResource(resourceId);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
                content.append('\n');
            }
        } catch (IOException e) {
            return "Error reading resource: " + e.getMessage();
        }

        return content.toString();
    }
    private void onClosed() {

    }
    public static String TAG = "BillingErrorDialog";
}
