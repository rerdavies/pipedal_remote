package com.twoplay.pipedal.model;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.twoplay.pipedal.PiPedalApplication;
import com.twoplay.pipedal.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

/**
 * Copyright (c) 2015, sRobin Davies
 * Created by Robin on 26/04/2022.
 */
public class BillingModel extends AndroidViewModel {


    private static final String TAG = "BillingModel";

    private BillingModel this_;
    public BillingModel(@NonNull Application application) {
        super(application);
        this_ = this;
        Log.i(TAG, "Billing Model started. " + this.hashCode());
        PrepareBilling(application);
    }

    private List<SkuDetails> oneTimeDonations = new ArrayList<>();
    private List<SkuDetails> subscriptions = new ArrayList<>();
    private BillingClient billingClient;

    public MutableLiveData<List<SkuDetails>> donorSkuDetails = new MutableLiveData<>(new ArrayList<SkuDetails>());
    public MutableLiveData<List<SkuDetails>> sponsorSkuDetails = new MutableLiveData<>(new ArrayList<SkuDetails>());



    public interface ErrorListener {
        void onErrorMessageAdded();
    };

    private ErrorListener errorListener;

    private ArrayList<String> errorMessages = new ArrayList<>();

    public void setErrorListener(ErrorListener listener)
    {
        this.errorListener = listener;
    }
    public boolean hasError()
    {
        return errorMessages.size() != 0;
    }

    public String  takeErrorMessage()
    {
        return errorMessages.remove(0);
    }

    private void showError(String message)
    {
        handler.post(()-> {
           errorMessages.add(message);
           if (errorListener != null)
           {
               errorListener.onErrorMessageAdded();
           }
        });
    }
    private String getString(@StringRes int ridString)
    {
        return PiPedalApplication.getContext().getString(ridString);
    }
    private PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && purchases != null) {
                for (Purchase purchase : purchases) {
                    handlePurchaseAcknowledge(purchase);
                }
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                return;
            } else {
                String message =String.format(getString(R.string.purchase_failed__reason), BillingModel.responseCodeToMessage(
                        PiPedalApplication.getContext(),
                        billingResult.getResponseCode()));

                Log.e(TAG,message);
// message has already been displayed by billing UI.
//                showError(
//                        message
//                        );
            }
        }
    };

    private HashMap<String,Purchase> purchaseMap = new HashMap<>();
    private void handlePurchaseAcknowledge(Purchase purchase) {

        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
            }
            for (String sku: purchase.getSkus())
            {
                purchaseMap.put(sku,purchase);
            }
        }
    }


    AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener = new AcknowledgePurchaseResponseListener() {
        @Override
        public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
            Log.i(TAG,"Acknowledged purchase. " + this_.hashCode());
            int responseCode = billingResult.getResponseCode();
            if (responseCode == BillingClient.BillingResponseCode.OK)
            {
                showError(getString(R.string.purchase_received));
            }
            if (responseCode != BillingClient.BillingResponseCode.OK)
            {
                showError(
                        String.format(getString(R.string.purchase_ack_failed__error),responseCodeToMessage(PiPedalApplication.getContext(),responseCode))
                );
            }
        }
    };

    private Handler handler = new Handler();

    @Override
    protected void onCleared() {

        if (billingClient != null) {
            billingClient.endConnection();
        }
        super.onCleared();
        Log.i(TAG, "Billing Model cleared. " + this_.hashCode());


    }


    private void PrepareBilling(Application activity) {

        // billing client
        billingClient = BillingClient.newBuilder(activity)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    ArrayList<String> allSkus = new ArrayList<>();
                    {
                        // The BillingClient is ready. You can query purchases here.
                        List<String> skuList = new ArrayList<>();
                        skuList.add("bronze_sponsorship");
                        skuList.add("silver_sponsorship");
                        skuList.add("gold_sponsorship");
                        allSkus.addAll(skuList);
                        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
                        billingClient.querySkuDetailsAsync(params.build(),
                                new SkuDetailsResponseListener() {
                                    @Override
                                    public void onSkuDetailsResponse(BillingResult billingResult,
                                                                     List<SkuDetails> skuDetailsList) {
                                        handler.post(() -> {
                                            donorSkuDetails.setValue(restoreOrder(skuDetailsList,skuList));
                                        });
                                    }
                                });
                    }
                    {
                        List<String> skuList = new ArrayList<>();
                        skuList.add("bronze_subscription");
                        skuList.add("silver_subscription");
                        skuList.add("gold_subscription");
                        allSkus.addAll(skuList);
                        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                        params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS);
                        billingClient.querySkuDetailsAsync(params.build(),
                                new SkuDetailsResponseListener() {
                                    @Override
                                    public void onSkuDetailsResponse(BillingResult billingResult,
                                                                     List<SkuDetails> skuDetailsList) {
                                        handler.post(() -> {
                                            sponsorSkuDetails.setValue(restoreOrder(skuDetailsList,skuList));
                                        });
                                    }
                                });

                    }
                    {
                        billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP, new PurchasesResponseListener() {
                            @Override
                            public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
                                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK)
                                {
                                    for (Purchase purchase: list)
                                    {
                                        handlePurchaseAcknowledge(purchase);
                                    }
                                }
                            }
                        });
                    }
                    {
                        billingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS, new PurchasesResponseListener() {
                            @Override
                            public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
                                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK)
                                {
                                    for (Purchase purchase: list)
                                    {
                                        handlePurchaseAcknowledge(purchase);
                                    }
                                }
                            }
                        });
                    }
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        });

    }

    private static List<SkuDetails> restoreOrder(List<SkuDetails> skuDetailsList, List<String> skuList) {
        ArrayList<SkuDetails> result = new ArrayList<>();
        for (String sku: skuList)
        {
            for (SkuDetails skuDetail: skuDetailsList)
            {
                if (sku.equals(skuDetail.getSku()))
                {
                    result.add(skuDetail);
                    break;
                }
            }
        }
        return result;
    }

    private void updateSkuDetails() {
        // concatenate the two sources.
        ArrayList<SkuDetails> result = new ArrayList<>();
        result.addAll(oneTimeDonations);
        result.addAll(subscriptions);
        this.donorSkuDetails.setValue(result); ;
    }

    private boolean consumeAndPurchaseLicense(Activity activity, SkuDetails skuDetails)
    {
        Purchase purchase = null;
        if (skuDetails.getSku().endsWith("_subscription")) return false;
        if (purchaseMap.containsKey(skuDetails.getSku()))
        {
            Purchase t = purchaseMap.get(skuDetails.getSku());
            if (t != null && t.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                purchase = t;
            }
        }
        if (purchase == null) return false;

        ConsumeParams consumeParams =
                ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();

        billingClient.consumeAsync(consumeParams, new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(@NonNull BillingResult billingResult, @NonNull String s) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                || billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_NOT_OWNED)
                {
                    try {
                        launchPurchaseFlow_(activity, skuDetails);
                    } catch (Exception e)
                    {
                        showError(e.getMessage());
                    }
                } else {
                    String message = "Consume failed. " +     responseCodeToMessage(PiPedalApplication.getContext(),billingResult.getResponseCode());
                    Log.e(TAG,message);
                    showError(message);
                }
            }
        });
        return true;

    }
    public void launchPurchaseFlow(Activity activity, SkuDetails skuDetails) throws Exception {
        if (consumeAndPurchaseLicense(activity,skuDetails))
        {
            return;
        } else {
            launchPurchaseFlow_(activity, skuDetails);
        }
    }
    public void launchPurchaseFlow_(Activity activity, SkuDetails skuDetails) throws Exception {
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build();
        int responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).getResponseCode();
        if (responseCode == BillingClient.BillingResponseCode.OK)
        {
            return;
        }
        throw new Exception(responseCodeToMessage(activity,responseCode));
    }

    private static String responseCodeToMessage(Context context, int responseCode) {
        int ridString = 0;
        switch (responseCode)
        {
            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
                ridString = R.string.billing_unavailable;
                break;
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                ridString = R.string.developer_error;
                break;
            case BillingClient.BillingResponseCode.SERVICE_TIMEOUT:
                ridString = R.string.service_timeout;
                break;
            case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
                ridString = R.string.feature_not_supported;
                break;
            case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
                ridString = R.string.service_disconnected;
                break;
            case BillingClient.BillingResponseCode.USER_CANCELED:
                ridString = R.string.user_cancelled;
                break;
            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
                ridString = R.string.service_unavailable;
                break;
            case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
                ridString = R.string.item_unavailable;
                break;
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                ridString = R.string.item_already_owned;
                break;
            case BillingClient.BillingResponseCode.ITEM_NOT_OWNED:
                ridString = R.string.item_not_owned;
                break;
            case BillingClient.BillingResponseCode.ERROR:
            default:
                ridString = R.string.unexpected_error;
                break;
        }
        return PiPedalApplication.getContext().getString(ridString);
    }
}
