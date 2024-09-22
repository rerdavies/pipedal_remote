package com.twoplay.pipedal;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.billingclient.api.SkuDetails;
import com.google.android.material.appbar.MaterialToolbar;
import com.twoplay.pipedal.model.BillingModel;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Copyright (c) 2015, sRobin Davies
 * Created by Robin on 27/04/2022.
 */
public class SponsorshipFragment extends Fragment
implements BillingErrorDialogFragment.CancelListener
{
    private BillingModel billingModel;
    private RecyclerView donorRecyclerView,sponsorRecyclerView;
    private MyAdapter donorAdapter,sponsorAdapter;

    public interface BackListener {
        void onReturnFromSponsorship();
    }

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view =  inflater.inflate(R.layout.fragment_sponsorship, container, false);
        this.billingModel = new ViewModelProvider(this).get(BillingModel.class);

        this.donorRecyclerView = view.findViewById(R.id.donor_recycler_view);
        this.sponsorRecyclerView = view.findViewById(R.id.sponsor_recycler_view);
        this.donorAdapter = new SponsorshipFragment.MyAdapter(billingModel.donorSkuDetails.getValue());
        this.sponsorAdapter = new SponsorshipFragment.MyAdapter(billingModel.sponsorSkuDetails.getValue());
        billingModel.donorSkuDetails.observe(this.getViewLifecycleOwner(),(items)-> {
            donorAdapter.setItems(items);
        });
        billingModel.sponsorSkuDetails.observe(this.getViewLifecycleOwner(),(items)-> {
            sponsorAdapter.setItems(items);
        });

        MaterialToolbar appBar = view.findViewById(R.id.app_bar);

        appBar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        appBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((BackListener)getActivity()).onReturnFromSponsorship();
            }
        });




        donorRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        donorRecyclerView.setAdapter(donorAdapter);

        sponsorRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        sponsorRecyclerView.setAdapter(sponsorAdapter);

        return view;
    }



    void showBillingError(String message)
    {
        if (message != "") {
            BillingErrorDialogFragment.execute(this, message, getString(R.string.app_name));
        }
    }

    Handler handler = new Handler();

    @Override
    public void onBillingErrorDialogCancelled() {
        handler.post(()-> {
            if (billingModel.hasError())
            {
                showingBillingError = true;
                BillingErrorDialogFragment.execute(this,billingModel.takeErrorMessage(), getString(R.string.app_name));
            }
            showingBillingError = false;
        });
    }

    private boolean showingBillingError = false;
    private void onBillingError()
    {
        if (!showingBillingError)
        {
            showingBillingError = true;
            BillingErrorDialogFragment.execute(this,billingModel.takeErrorMessage(), getString(R.string.app_name));
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        billingModel.setErrorListener(()-> onBillingError());
        showingBillingError = getChildFragmentManager().findFragmentByTag(BillingErrorDialogFragment.TAG) != null;
        if (!showingBillingError && billingModel.hasError())
        {
            BillingErrorDialogFragment.execute(this,billingModel.takeErrorMessage(),getString(R.string.app_name));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    class MyViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageView;
        private View card;
        private TextView primaryText;
        private TextView secondaryText;
        private TextView priceText;
        private SkuDetails skuDetails;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_background);
            card.setOnClickListener((view)-> {
                if (skuDetails != null)
                {
                    onSkuClicked(skuDetails);
                }
            });
            primaryText = itemView.findViewById(R.id.primary_text);
            imageView = itemView.findViewById(R.id.medallion);
            secondaryText = itemView.findViewById(R.id.secondary_text);
            priceText = itemView.findViewById(R.id.price_text);
        }

        public void bind(SkuDetails skuDetails) {
            this.skuDetails = skuDetails;
            int ridPrimary;
            int ridSecondary;
            int ridImage;

            switch (skuDetails.getSku())
            {
                case "bronze_sponsorship":
                    ridPrimary = R.string.bronze_donor;
                    ridSecondary = R.string.one_time_donation;
                    ridImage = R.drawable.ic_circle_24px_bronze;
                    break;
                case "silver_sponsorship":
                    ridPrimary = R.string.silver_donor;
                    ridSecondary = R.string.one_time_donation;
                    ridImage = R.drawable.ic_circle_24px_silver;
                    break;
                case "gold_sponsorship":
                    ridPrimary = R.string.gold_donor;
                    ridSecondary = R.string.one_time_donation;
                    ridImage = R.drawable.ic_circle_24px_gold;
                    break;
                case "bronze_subscription":
                    ridPrimary = R.string.bronze_sponsor;
                    ridSecondary = R.string.monthly_donation;
                    ridImage = R.drawable.ic_circle_24px_bronze;
                    break;
                case "silver_subscription":
                    ridPrimary = R.string.silver_sponsor;
                    ridSecondary = R.string.monthly_donation;
                    ridImage = R.drawable.ic_circle_24px_silver;
                    break;
                case "gold_subscription":
                    ridPrimary = R.string.gold_sponsor;
                    ridSecondary = R.string.monthly_donation;
                    ridImage = R.drawable.ic_circle_24px_gold;
                    break;
                default:
                    throw new RuntimeException("Unexpected sku: " + skuDetails.getSku());
            }
            primaryText.setText(getString(ridPrimary));
            secondaryText.setText(getString(ridSecondary));
            priceText.setText(skuDetails.getPrice());
            imageView.setImageResource(ridImage);
        }
    }

    private void onSkuClicked(SkuDetails skuDetails) {
        // launch purchase flow.
        try {
            billingModel.launchPurchaseFlow(this.getActivity(), skuDetails);
        } catch (Exception e)
        {
            ErrorDialogFragment.execute(this,e.getMessage(),getString(R.string.app_name));
        }

    }

    class MyAdapter extends RecyclerView.Adapter<SponsorshipFragment.MyViewHolder> {

        List<SkuDetails> items;

        public MyAdapter(List<SkuDetails> items)
        {
            this.items = items;
        }
        public void setItems(List<SkuDetails> items)
        {
            this.items = items;
            this.notifyDataSetChanged();
        }
        @NonNull
        @Override
        public SponsorshipFragment.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View v =  layoutInflater.inflate(R.layout.item_sku,parent,false);
            return new SponsorshipFragment.MyViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull SponsorshipFragment.MyViewHolder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }


}