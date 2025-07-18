package com.twoplay.pipedal;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.google.android.material.appbar.MaterialToolbar;
import com.twoplay.pipedal.model.BillingModel;

import java.util.Dictionary;
import java.util.HashMap;
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
{
    private BillingModel billingModel;
    private RecyclerView donorRecyclerView,sponsorRecyclerView;
    private MyAdapter oneTimeProductDetailsAdapter,sponsorAdapter;
    private TextView billingErrorText;

    public interface BackListener {
        void onReturnFromSponsorship();
    }
    private HashMap<String,Purchase> purchaseMap = new HashMap<>();
    void updatePurchases(List<Purchase> items) {
        purchaseMap = new HashMap<>();
        for (Purchase purchase : items) {
            for (var productId : purchase.getProducts()) {
                purchaseMap.put(productId, purchase);
            }
        }
    }

    private void handlePaymentDeclinedMessage(String message)
    {
        if (!message.isEmpty()) {
            billingModel.paymentDeclinedMessage.setValue("");
            BillingErrorDialogFragment.execute(
                    this,
                    message,
                    "PiPedal");
        }

    }

    private void updateErrorText(String message)
    {
        if (message.isEmpty())
        {
            billingErrorText.setVisibility(View.GONE);
        } else {
            billingErrorText.setVisibility(View.VISIBLE);
             billingErrorText.setText(message);
             billingErrorText.getParent().requestLayout();
//            billingErrorText.requestLayout();
//            billingErrorText.invalidate();
        }
    }

    private void updateConnectedStatus(boolean connected)
    {
        if (connected) {
            updateErrorText("");
        } else {
            updateErrorText(getString(R.string.billing_not_connected));
        }
    }
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view =  inflater.inflate(R.layout.fragment_sponsorship, container, false);

        this.billingModel = new ViewModelProvider(requireActivity()).get(BillingModel.class);
        this.billingModel.refreshPurchases();
        this.billingErrorText = view.findViewById(R.id.billing_status_text);

        updatePurchases(billingModel.purchases.getValue());

        this.billingModel.isConnected.observe(this.getViewLifecycleOwner(),(isConnected)-> {
            updateConnectedStatus(isConnected);
        });
        updateConnectedStatus(billingModel.isConnected.getValue());


        this.donorRecyclerView = view.findViewById(R.id.donor_recycler_view);
        this.sponsorRecyclerView = view.findViewById(R.id.sponsor_recycler_view);
        this.oneTimeProductDetailsAdapter = new SponsorshipFragment.MyAdapter(billingModel.oneTimeProductDetails.getValue());
        this.sponsorAdapter = new SponsorshipFragment.MyAdapter(billingModel.subscriptionProductDetails.getValue());
        billingModel.oneTimeProductDetails.observe(this.getViewLifecycleOwner(),(items)-> {
            oneTimeProductDetailsAdapter.setItems(items);
        });
        oneTimeProductDetailsAdapter.setItems(billingModel.oneTimeProductDetails.getValue());

        billingModel.subscriptionProductDetails.observe(this.getViewLifecycleOwner(),(items)-> {
            sponsorAdapter.setItems(items);
        });
        sponsorAdapter.setItems(billingModel.subscriptionProductDetails.getValue());

        billingModel.purchases.observe(this.getViewLifecycleOwner(),(items)-> {
            updatePurchases(items);
            oneTimeProductDetailsAdapter.onPurchasesChanged();
            sponsorAdapter.onPurchasesChanged();
        });
        billingModel.paymentDeclinedMessage.observe(this.getViewLifecycleOwner(),(message)-> {
            handlePaymentDeclinedMessage(message);
        });
        handlePaymentDeclinedMessage(billingModel.paymentDeclinedMessage.getValue());

        MaterialToolbar appBar = view.findViewById(R.id.app_bar);

        appBar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        appBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((BackListener)getActivity()).onReturnFromSponsorship();
            }
        });

        donorRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        donorRecyclerView.setAdapter(oneTimeProductDetailsAdapter);

        sponsorRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        sponsorRecyclerView.setAdapter(sponsorAdapter);

        return view;
    }



    void showBillingError(String message)
    {
        if (!message.isEmpty()) {
            BillingErrorDialogFragment.execute(this, message, getString(R.string.app_name));
        }
    }

    Handler handler = new Handler();


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    class MyViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageView;
        private final View checkmark;
        private View card;
        private TextView primaryText;
        private TextView secondaryText;
        private TextView priceText;
        private ProductDetails productDetails;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_background);
            card.setOnClickListener((view)-> {
                if (productDetails != null)
                {
                    onSkuClicked(productDetails);
                }
            });
            primaryText = itemView.findViewById(R.id.primary_text);
            imageView = itemView.findViewById(R.id.medallion);
            secondaryText = itemView.findViewById(R.id.secondary_text);
            priceText = itemView.findViewById(R.id.price_text);
            checkmark = itemView.findViewById(R.id.checkmark);
        }

        public void bind(ProductDetails productDetails) {
            this.productDetails = productDetails;
            int ridPrimary;
            int ridSecondary;
            int ridImage;
            Purchase purchase = SponsorshipFragment.this.purchaseMap.get(productDetails.getProductId());
            float checkmarkAlpha = 0.0f;
            if (purchase != null) {
                checkmark.setVisibility(View.VISIBLE);
                switch (purchase.getPurchaseState())
                {
                    case Purchase.PurchaseState.PURCHASED:
                        checkmarkAlpha = 1.0f;
                        break;
                    case Purchase.PurchaseState.PENDING:
                        checkmarkAlpha = 0.3f;
                        break;
                    default:
                        break;
                }
                checkmark.setAlpha(checkmarkAlpha);
            } else {
                checkmark.setVisibility(View.GONE);
            }

            switch (productDetails.getProductId())
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
                    throw new RuntimeException("Unexpected sku: " + productDetails.getProductId());
            }
            primaryText.setText(getString(ridPrimary));
            secondaryText.setText(getString(ridSecondary));

            String price = "#error";
            if (productDetails.getProductType().equals(BillingClient.ProductType.SUBS))
            {
                var subscriptionOffer = productDetails.getSubscriptionOfferDetails();
                if (subscriptionOffer != null && !subscriptionOffer.isEmpty()) {
                    var phases = subscriptionOffer.get(0).getPricingPhases();
                    //xxx
                    price = phases.getPricingPhaseList().get(0).getFormattedPrice();

                }

            } else {
                price = productDetails.getOneTimePurchaseOfferDetails().getFormattedPrice();
            }
            this.priceText.setText(price);
            imageView.setImageResource(ridImage);
        }
    }

    private void onSkuClicked(ProductDetails skuDetails) {
        // launch purchase flow.
        try {
            billingModel.launchPurchaseFlow(this.getActivity(), skuDetails);
        } catch (Exception e)
        {
            ErrorDialogFragment.execute(this,e.getMessage(),getString(R.string.app_name));
        }

    }

    class MyAdapter extends RecyclerView.Adapter<SponsorshipFragment.MyViewHolder> {

        List<ProductDetails> items;

        public MyAdapter(List<ProductDetails> items)
        {
            this.items = items;
        }
        @SuppressLint("NotifyDataSetChanged")
        public void setItems(List<ProductDetails> items)
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

        @SuppressLint("NotifyDataSetChanged")
        public void onPurchasesChanged() {
            this.notifyDataSetChanged();
        }
    }


}