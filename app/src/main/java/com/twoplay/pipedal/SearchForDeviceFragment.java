package com.twoplay.pipedal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.twoplay.pipedal.model.Model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

/**
 * Copyright (c) 2015, sRobin Davies
 * Created by Robin on 25/04/2022.
 */

public class SearchForDeviceFragment extends Fragment {

    private TextView searchingText;
    private Model model;

    public interface RationaleResult {
        void OnRationaleResult(boolean proceed);
    };
    public SearchForDeviceFragment() {
        // Required empty public constructor
    }

    public static SearchForDeviceFragment newInstance() {
        SearchForDeviceFragment fragment = new SearchForDeviceFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void setResult(boolean proceed)
    {
        RationaleResult result = (RationaleResult)(Object)requireActivity();
        result.OnRationaleResult(proceed);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_search_for_device, container, false);
        searchingText = v.findViewById(R.id.searching_text);
        Toolbar appBar = v.findViewById(R.id.app_bar);
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
        if (getActivity() != null)
        {
            if (model != null)
            {
                model.p2pDisconnect(()-> {
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                });
            } else {
                getActivity().finish();
            }
        }

    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.model = new ViewModelProvider(requireActivity()).get(Model.class);
        model.getDeviceScanner().scanForDeviceMessage.observe(this.getViewLifecycleOwner(),(String value) -> {
            searchingText.setText(value);
        });
    }


    @Override
    public void onStart() {
        super.onStart();
    }
}