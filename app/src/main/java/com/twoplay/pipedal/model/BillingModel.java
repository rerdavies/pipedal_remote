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
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.twoplay.pipedal.R;

import java.util.ArrayList;
import java.util.HashSet;
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
    private Context context;

    private Context getContext() {
        return context;
    }
    public BillingModel(@NonNull Application application) {
        super(application);
        this.context = application.getApplicationContext();
        this_ = this;
        PrepareBilling(application);
    }

    private BillingClient billingClient;


    public MutableLiveData<List<ProductDetails>> oneTimeProductDetails = new MutableLiveData<>(new ArrayList<>());
    public MutableLiveData<List<ProductDetails>> subscriptionProductDetails = new MutableLiveData<>(new ArrayList<>());

    private MutableLiveData<List<Purchase>> oneTimeDonations = new MutableLiveData<>(new ArrayList<>());
    private MutableLiveData<List<Purchase>> subscriptions = new MutableLiveData<>(new ArrayList<>());
    public MutableLiveData<List<Purchase>> purchases = new MutableLiveData<>(new ArrayList<>());

    public MutableLiveData<String> billingError = new MutableLiveData<>("");

    public MutableLiveData<Boolean> paymentReceived = new MutableLiveData<>(true);


    public MutableLiveData<Integer> billingResponseCode
            = new MutableLiveData<>(BillingClient.BillingResponseCode.OK);

    public MutableLiveData<Boolean> isConnected = new MutableLiveData<>(false);
    public MutableLiveData<String> paymentDeclinedMessage = new MutableLiveData<>("");
    public interface ErrorListener {
        void onErrorMessageAdded();
    }



    void setBillingResponseCode(int responseCode)
    {
        handler.post(()->{
            this.billingError.setValue(responseCodeToMessage(getContext(),responseCode));
            this.billingResponseCode.setValue(responseCode);
        });
    }


    private String getString(@StringRes int ridString)
    {
        return getContext().getString(ridString);
    }
    private PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> purchases) {
            if (checkBillingResult("purchasesUpdated", billingResult)
                    && purchases != null) {
                final ArrayList<Purchase> validPurchases = new ArrayList<>();
                for (Purchase purchase : purchases) {
                    handlePurchaseAcknowledge(purchase);

                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED
                            || purchase.getPurchaseState() == Purchase.PurchaseState.PENDING
                    ) {
                        validPurchases.add(purchase);
                    }
                }
                handler.post(()-> {
                    HashSet<String> existingProducts = new HashSet<>();
                    ArrayList<Purchase> updatedPurchases = new ArrayList<>(purchases);
                    for (var purchase: purchases)
                    {
                        existingProducts.addAll(purchase.getProducts());
                    }

                    for (var existingPurchase: BillingModel.this.purchases.getValue())
                    {
                        boolean found = false;
                        for (var purchasedProductId: existingPurchase.getProducts())
                        {
                            if (existingProducts.contains(purchasedProductId))
                            {
                                found = true;
                                break;
                            }
                        }
                        if (!found)  {
                            updatedPurchases.add(existingPurchase);
                        }
                    }
                    BillingModel.this.purchases.setValue(updatedPurchases);
                    schedulePendingPurchaseUpdate();
                });
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                return;
            } else {
                String message =String.format(getString(R.string.purchase_failed__reason), responseCodeToMessage(
                        getContext(),
                        billingResult.getResponseCode()));
                ArrayList<Purchase> updatedPurchases = new ArrayList<>(BillingModel.this.purchases.getValue());

                ArrayList<Purchase> removedPurchases = new ArrayList<>();

                if (purchases != null) {
                    for (var existingPurchase : updatedPurchases) {
                        for (var p : purchases) {
                            if (existingPurchase.getPurchaseToken().equals(p.getPurchaseToken())) {
                                removedPurchases.add(existingPurchase);
                                break;
                            }
                        }
                    }
                    if (!removedPurchases.isEmpty()) {
                        updatedPurchases.removeAll(removedPurchases);
                        BillingModel.this.purchases.setValue(updatedPurchases);
                    }
                }

                paymentDeclinedMessage.setValue(message);

                Log.e(TAG,message);
            }
        }
    };

    private void handlePurchaseAcknowledge(Purchase purchase) {

        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
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
                paymentReceived.setValue(true);
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

    private boolean checkBillingResult(String method, BillingResult billingResult) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK)
        {
            return true;
        }
        if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.NETWORK_ERROR)
        {
            Log.i(TAG, method + ": Response failed. (" + responseCodeToMessage(getContext(),billingResult.getResponseCode()) + ")");
        }
        setBillingResponseCode(billingResult.getResponseCode());
        return false;
    }
    private void PrepareBilling(Application activity) {

        // billing client
        this.purchases.setValue(new ArrayList<>());
        this.oneTimeDonations.setValue(new ArrayList<>());
        this.subscriptions.setValue(new ArrayList<>());


        PendingPurchasesParams purchaseParms =
                PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .enablePrepaidPlans()
                        .build();

        billingClient = BillingClient.newBuilder(activity)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases(purchaseParms)
                .build();
        isConnected.setValue(false);

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {

                isConnected.postValue(true);

                if (checkBillingResult("Billing.startConnection", billingResult)) {
                    List<String> oneTimeList = new ArrayList<>();
                    oneTimeList.add("gold_sponsorship");
                    oneTimeList.add("silver_sponsorship");
                    oneTimeList.add("bronze_sponsorship");
                    List<String> subscriptionList = new ArrayList<>();
                    subscriptionList.add("gold_subscription");
                    subscriptionList.add("silver_subscription");
                    subscriptionList.add("bronze_subscription");
                    // The BillingClient is ready. You can query purchases here.
                    List<QueryProductDetailsParams.Product> oneTimeProducts = new ArrayList<>();
                    List<QueryProductDetailsParams.Product> subscriptionProducts = new ArrayList<>();
                    for (String sponsorshipId: oneTimeList)
                    {
                        var product = QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(sponsorshipId)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build();
                        oneTimeProducts.add(product);
                    }
                    for (String subscriptionId: subscriptionList)
                    {
                        var product = QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(subscriptionId)
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build();
                        subscriptionProducts.add(product);
                    }
                    billingClient.queryProductDetailsAsync(
                            QueryProductDetailsParams.newBuilder()
                                    .setProductList(oneTimeProducts)
                                    .build(),
                            (
                                billingResult1,
                                oneTimeDetails
                            ) -> {
                                if (checkBillingResult("queryProductDetails", billingResult1)) {
                                    var sortedItems = restoreOrder(oneTimeDetails.getProductDetailsList(),oneTimeList);
                                    BillingModel.this.oneTimeProductDetails.postValue(sortedItems);
                                    for (var unfetchedProduct : oneTimeDetails.getUnfetchedProductList()) {
                                        Log.i(TAG, "Unfetched billing product: " + unfetchedProduct.getProductId() + " " + unfetchedProduct.getProductType());
                                    }
                                }
                            }
                    );
                    billingClient.queryProductDetailsAsync(
                            QueryProductDetailsParams.newBuilder()
                                    .setProductList(subscriptionProducts)
                                    .build(),
                            (
                                    billingResult1,
                                    subscriptionDetails
                            ) -> {
                                if (checkBillingResult("queryProductDetails", billingResult1)) {
                                    var sortedItems = restoreOrder(subscriptionDetails.getProductDetailsList(),subscriptionList);

                                    BillingModel.this.subscriptionProductDetails.postValue(sortedItems);
                                    for (var unfetchedProduct : subscriptionDetails.getUnfetchedProductList()) {
                                        Log.i(TAG, "Unfetched billing product: " + unfetchedProduct.getProductId() + " " + unfetchedProduct.getProductType());
                                    }
                                }
                            }
                    );

                    refreshPurchases();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                isConnected.postValue(false);
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        });

    }


    public void refreshPurchases()
    {
        handler.post(()-> {

            if (!isConnected.getValue()) {
                return;
            }
            QueryPurchasesParams queryPurchasesParamsInApp =
                    QueryPurchasesParams
                            .newBuilder()
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build();
            billingClient.queryPurchasesAsync(
                    queryPurchasesParamsInApp,
                    (billingResult1, list) -> {
                        if (checkBillingResult("queryPurchases(INAPP)", billingResult1)) {
                            ArrayList<Purchase> validPurchases = new ArrayList<>();
                            for (Purchase purchase : list) {
                                handlePurchaseAcknowledge(purchase);
                                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED
                                        || purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                                    validPurchases.add(purchase);
                                }
                            }
                            handler.post(() -> {
                                BillingModel.this.oneTimeDonations.setValue(validPurchases);
                                updatePurchases();
                            });

                        }
                    }
            );
            QueryPurchasesParams queryPurchasesParamsSubs =
                    QueryPurchasesParams
                            .newBuilder()
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build();
            billingClient.queryPurchasesAsync(
                    queryPurchasesParamsSubs,
                    (billingResult1, list) -> {
                        if (checkBillingResult("queryPurchases(SUBS)", billingResult1)) {
                            ArrayList<Purchase> validPurchases = new ArrayList<>();
                            for (Purchase purchase : list) {
                                handlePurchaseAcknowledge(purchase);
                                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED
                                        || purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                                    validPurchases.add(purchase);
                                }
                            }
                            handler.post(() -> {
                                BillingModel.this.subscriptions.setValue(validPurchases);
                                updatePurchases();
                            });
                        }
                    }
            );
        });

    }
    private static List<ProductDetails> restoreOrder(
            List<ProductDetails> productDetailsList    ,
            List<String> productIdList
    ) {
        ArrayList<ProductDetails> result = new ArrayList<>();
        for (String productId: productIdList)
        {
            for (ProductDetails product: productDetailsList)
            {
                if (productId.equals(product.getProductId()))
                {
                    result.add(product);
                    break;
                }
            }
        }
        return result;
    }

    private boolean hasPendingPurchaseUpdate = false;
    private void updatePurchases() {
        // concatenate the two sources.
        ArrayList<Purchase> result = new ArrayList<>();
        result.addAll(oneTimeDonations.getValue());
        result.addAll(subscriptions.getValue());
        this.purchases.setValue(result);
        schedulePendingPurchaseUpdate();
    }
    void schedulePendingPurchaseUpdate() {
        boolean hasPendingPayment = false;
        for (var purchase: this.purchases.getValue())
        {
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING)
            {
                hasPendingPayment = true;
                break;
            }
        }
        if (hasPendingPayment && isConnected.getValue())
        {
            if (!hasPendingPurchaseUpdate) {
                hasPendingPurchaseUpdate = true;
                handler.postDelayed(
                        () -> {
                            hasPendingPurchaseUpdate = false;
                            refreshPurchases();
                        },
                        15 * 1000
                );
            }
        }

    }

    void showError(String message)
    {
        this.billingError.postValue(message);
    }
    private boolean consumeAndPurchaseLicense(Activity activity, final ProductDetails product)
    {
        if (product.getProductType().equals(BillingClient.ProductType.SUBS))
        {
            return false;
        }
        Purchase existingPurchase = null;
        for (var activePurchase: this.purchases.getValue())
        {
            boolean found = false;
            for (var purchasedProductId: activePurchase.getProducts())
            {
                if (product.getProductId().equals(purchasedProductId))
                {
                    existingPurchase = activePurchase;
                    break;
                }
            }
            if (existingPurchase != null) break;
        }
        if (existingPurchase == null) {
            return false;
        }
        ConsumeParams consumeParams =
                ConsumeParams.newBuilder()
                        .setPurchaseToken(existingPurchase.getPurchaseToken())
                        .build();

        billingClient.consumeAsync(consumeParams, new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(@NonNull BillingResult billingResult, @NonNull String s) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                || billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_NOT_OWNED)
                {
                    try {
                        launchPurchaseFlow_(activity, product);
                    } catch (Exception e)
                    {
                        showError(e.getMessage());
                    }
                } else {
                    String message = "Consume failed. " +     responseCodeToMessage(getContext(),billingResult.getResponseCode());
                    Log.e(TAG,message);
                    showError(message);
                }
            }
        });
        return true;

    }
    public void launchPurchaseFlow(Activity activity, ProductDetails product) throws Exception {
        paymentDeclinedMessage.setValue("");
        if (consumeAndPurchaseLicense(activity,product))
        {
            return;
        } else {
            launchPurchaseFlow_(activity, product);
        }
    }
    public void launchPurchaseFlow_(Activity activity, ProductDetails product) throws Exception {
        ArrayList<BillingFlowParams.ProductDetailsParams> productDetailsParamList = new ArrayList<>();
        if (product.getProductType().equals(BillingClient.ProductType.SUBS))
        {
            String offerToken = product.getSubscriptionOfferDetails().get(0).getOfferToken();
            productDetailsParamList.add(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(product)
                            .setOfferToken(offerToken)
                            .build()
            );
        } else{
            productDetailsParamList.add(
                    BillingFlowParams.ProductDetailsParams.newBuilder().
                            setProductDetails(product)
                            .build()
            );
        }
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamList)
                .build();
        int responseCode = billingClient.launchBillingFlow(
                activity, billingFlowParams
        ).getResponseCode();
        if (responseCode == BillingClient.BillingResponseCode.OK)
        {
            return;
        }
        throw new Exception(responseCodeToMessage(activity,responseCode));
    }

    private String responseCodeToMessage(Context context, int responseCode) {
        int ridString;
        switch (responseCode)
        {
            case BillingClient.BillingResponseCode.OK:
                return "";
            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
                ridString = R.string.billing_unavailable;
                break;
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                ridString = R.string.developer_error;
                break;
            //noinspection deprecation
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
        return getContext().getString(ridString);
    }
}
