/*
 * Copyright 2021 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sample.android.classytaxijava.data.network.retrofit;

import android.text.TextUtils;
import android.util.Log;

import com.sample.android.classytaxijava.data.ContentResource;
import com.sample.android.classytaxijava.data.SubscriptionStatus;
import com.sample.android.classytaxijava.data.network.firebase.ServerFunctions;
import com.sample.android.classytaxijava.data.network.retrofit.authentication.RetrofitClient;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.lifecycle.MutableLiveData;

import static com.sample.android.classytaxijava.BuildConfig.SERVER_URL;


/**
 * Implementation of Interfaces with Retrofit.
 */
public class ServerFunctionImpl implements ServerFunctions {

    private static final String TAG = "RemoteServerFunction";
    private static volatile ServerFunctions INSTANCE = null;
    private final MutableLiveData<List<SubscriptionStatus>> subscriptions = new MutableLiveData<>();
    private final MutableLiveData<ContentResource> basicContent = new MutableLiveData<>();
    private final MutableLiveData<ContentResource> premiumContent = new MutableLiveData<>();
    private MutableLiveData<Boolean> loading = new MutableLiveData<>();
    private final RetrofitClient<SubscriptionStatusApiCall> retrofitClient = new RetrofitClient<>(SERVER_URL, SubscriptionStatusApiCall.class);
    private final PendingRequestCounter pendingRequestCounter = new PendingRequestCounter();

    private ServerFunctionImpl() {
    }

    public static ServerFunctions getInstance() {
        if (INSTANCE == null) {
            synchronized (ServerFunctionImpl.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ServerFunctionImpl();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Live data is true when there are pending network requests.
     */
    @Override
    public LiveData<Boolean> getLoading() {
        loading = pendingRequestCounter.getLoading();
        return loading;
    }

    /**
     * The latest subscription data from the Firebase server.
     * <p>
     * Use this class by observing the subscriptions LiveData.
     * Any server updates will be communicated through this LiveData.
     */
    @Override
    public LiveData<List<SubscriptionStatus>> getSubscriptions() {
        return subscriptions;
    }

    /**
     * The basic content URL.
     */
    @Override
    public LiveData<ContentResource> getBasicContent() {
        return basicContent;
    }

    /**
     * The premium content URL.
     */
    @Override
    public LiveData<ContentResource> getPremiumContent() {
        return premiumContent;
    }


    /**
     * Fetches basic content and posts results to {@link #basicContent}.
     * This will fail if the user does not have a basic subscription.
     */
    @Override
    public void updateBasicContent() {
        final String method = "updateBasicContent";
        pendingRequestCounter.incrementRequestCount();
        retrofitClient.getService().fetchBasicContent().enqueue(new RetrofitResponseHandlerCallback<ContentResource>(method, pendingRequestCounter) {
            protected void onSuccess(ContentResource response) {
                basicContent.postValue(response);
            }
        });
    }

    /**
     * Fetches premium content and posts results to {@link #premiumContent}.
     * This will fail if the user does not have a premium subscription.
     */
    public void updatePremiumContent() {
        final String method = "updatePremiumContent";
        pendingRequestCounter.incrementRequestCount();
        retrofitClient.getService().fetchPremiumContent().enqueue(new RetrofitResponseHandlerCallback<ContentResource>(method, pendingRequestCounter) {
            @Override
            protected void onSuccess(ContentResource response) {
                premiumContent.postValue(response);
            }
        });
    }

    /**
     * Fetches the Subscription Status from the server.
     */
    public void updateSubscriptionStatus() {
        final String method = "updateSubscriptionStatus";
        pendingRequestCounter.incrementRequestCount();
        retrofitClient.getService().fetchSubscriptionStatus().enqueue(new RetrofitResponseHandlerCallback<Map<String, Object>>(method, pendingRequestCounter) {
            @Override
            protected void onSuccess(Map<String, Object> response) {
                onSuccessfulSubscriptionCall(response, subscriptions);
            }
        });
    }

    /**
     * Registers a subscription with the server and posts successful results to
     * {@link #subscriptions}.
     *
     * @param sku           the ID of a specific product type
     * @param purchaseToken string that represents a buyer's entitlement to a product on Google Play
     */
    public void registerSubscription(String sku, String purchaseToken) {
        final String method = "registerSubscription";
        SubscriptionStatus data = new SubscriptionStatus();
        data.sku = sku;
        data.purchaseToken = purchaseToken;
        pendingRequestCounter.incrementRequestCount();
        retrofitClient.getService().registerSubscription(data).enqueue(new RetrofitResponseHandlerCallback<Map<String, Object>>(method, pendingRequestCounter) {
            @Override
            protected void onSuccess(Map<String, Object> response) {
                onSuccessfulSubscriptionCall(response, subscriptions);
            }

            @Override
            protected void onError(int errorCode, @Nullable String errorMessage) {
                if (errorCode == HttpURLConnection.HTTP_CONFLICT) {
                    Log.w(TAG, "Subscription already exists");
                    List<SubscriptionStatus> oldSubscriptions =
                            subscriptions.getValue();
                    SubscriptionStatus newSubscription =
                            SubscriptionStatus
                                    .alreadyOwnedSubscription(sku, purchaseToken);
                    List<SubscriptionStatus> newSubscriptions =
                            insertOrUpdateSubscription(oldSubscriptions,
                                    newSubscription);
                    subscriptions.postValue(newSubscriptions);
                }
                super.logError(errorCode, errorMessage);
            }
        });
    }

    /**
     * Transfers subscription to this account posts successful results to {@link #subscriptions}.
     *
     * @param sku           the ID of a specific product type
     * @param purchaseToken string that represents a buyer's entitlement to a product on Google Play
     */
    public void transferSubscription(String sku, String purchaseToken) {
        final String method = "transferSubscription";
        SubscriptionStatus data = new SubscriptionStatus();
        data.sku = sku;
        data.purchaseToken = purchaseToken;
        pendingRequestCounter.incrementRequestCount();
        retrofitClient.getService().transferSubscription(data).enqueue(new RetrofitResponseHandlerCallback<Map<String, Object>>(method, pendingRequestCounter) {
            @Override
            protected void onSuccess(Map<String, Object> response) {
                onSuccessfulSubscriptionCall(response, subscriptions);
            }
        });
    }

    /**
     * Registers Instance ID for Firebase Cloud Messaging.
     *
     * @param instanceId an FCM registration/instance token returned by
     *                   {@link com.sample.android.classytaxijava.FcmRegistrationTokenService}
     */
    @Override
    public void registerInstanceId(String instanceId) {
        final String method = "registerInstanceId";
        Map<String, String> data = new HashMap<>();
        data.put("instanceId", instanceId);
        pendingRequestCounter.incrementRequestCount();
        retrofitClient.getService().registerInstanceID(data).enqueue(new RetrofitResponseHandlerCallback<String>(method, pendingRequestCounter) {
            @Override
            protected void onSuccess(String response) {
                // A production app may want to track whether registration has been successful to allow for retrying.
            }
        });
    }

    /**
     * Unregisters Instance ID for Firebase Cloud Messaging.
     *
     * @param instanceId an FCM registration/instance token returned by
     *                   {@link com.sample.android.classytaxijava.FcmRegistrationTokenService}
     */
    @Override
    public void unregisterInstanceId(String instanceId) {
        final String method = "unregisterInstanceId";
        Map<String, String> data = new HashMap<>();
        data.put("instanceId", instanceId);
        pendingRequestCounter.incrementRequestCount();
        retrofitClient.getService().unregisterInstanceID(data).enqueue(new RetrofitResponseHandlerCallback<String>(method, pendingRequestCounter) {
            @Override
            protected void onSuccess(String response) {
                // A production app may want to track whether un-registration has been successful to allow for retrying.
            }
        });
    }

    // Helper functions

    /**
     * Inserts or updates the subscription to the list of existing subscriptions.
     * <p>
     * If none of the existing subscriptions have a SKU that matches, insert this SKU.
     * If a subscription exists with the matching SKU, the output list will contain the new
     * subscription instead of the old subscription.
     */
    private List<SubscriptionStatus> insertOrUpdateSubscription(
            List<SubscriptionStatus> oldSubscriptions,
            SubscriptionStatus newSubscription) {
        List<SubscriptionStatus> subscriptionStatuses = new ArrayList<>();
        if (oldSubscriptions == null || oldSubscriptions.isEmpty()) {
            subscriptionStatuses.add(newSubscription);
            return subscriptionStatuses;
        }

        boolean subscriptionAdded = false;
        for (SubscriptionStatus subscription : oldSubscriptions) {
            if (TextUtils.equals(subscription.sku, newSubscription.sku)) {
                subscriptionStatuses.add(newSubscription);
                subscriptionAdded = true;
            } else {
                subscriptionStatuses.add(subscription);
            }
        }

        if (!subscriptionAdded) {
            subscriptionStatuses.add(newSubscription);
        }

        return subscriptionStatuses;
    }

    /**
     * Called when a successful response returns from the server
     * for a {@link SubscriptionStatus} HTTPS call
     *
     * @param responseBody  Successful subscription statuses response object
     * @param subscriptions LiveData subscription list
     */
    protected void onSuccessfulSubscriptionCall(Map<String, Object> responseBody, @Nullable MutableLiveData<List<SubscriptionStatus>> subscriptions) {
        List subs = (List) responseBody.get("subscriptions");
        if (subs == null) {
            Log.w(TAG, "Invalid subscription data");
            return;
        }
        Log.i(TAG, "Valid subscription data");
        List<SubscriptionStatus> subList = SubscriptionStatus.listFromMap(responseBody);
        subscriptions.postValue(subList);
    }
}




