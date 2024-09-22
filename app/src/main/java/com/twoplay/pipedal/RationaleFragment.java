package com.twoplay.pipedal;

import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class RationaleFragment extends Fragment {

    public interface RationaleResult {
        void OnRationaleResult(boolean proceed);
    };
    public RationaleFragment() {
        // Required empty public constructor
    }

    public static RationaleFragment newInstance() {
        RationaleFragment fragment = new RationaleFragment();
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
        View v = inflater.inflate(R.layout.fragment_rationale, container, false);

        TextView rationaleText = (TextView) v.findViewById(R.id.rationale_text);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            rationaleText.setText(R.string.nearby_device_rationale_text);
        }

        Button proceedButton = v.findViewById(R.id.proceed_button);
        proceedButton.setOnClickListener((View vw) ->{
            setResult(true);
        });
        Button quitButton = v.findViewById(R.id.button_quit);
        quitButton.setOnClickListener((View vw) -> {

        });

        Toolbar appBar = v.findViewById(R.id.app_bar);
        appBar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        appBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        return v;
    }
}