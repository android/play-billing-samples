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

package com.example.subscriptions.data.network.retrofit

import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.subscriptions.data.ContentResource
import com.example.subscriptions.data.SubscriptionStatus
import com.example.subscriptions.data.network.firebase.ServerFunctions
import com.example.subscriptions.data.network.retrofit.authentication.RetrofitClient
import java.net.HttpURLConnection
import com.example.subscriptions.BuildConfig.SERVER_URL


/**
 * Implementation of [ServerFunctions] using Retrofit.
 */
class RemoteServerFunctionsImpl : ServerFunctions {

    private val retrofitClient = RetrofitClient(SERVER_URL, SubscriptionStatusApiCall::class.java)

    /**
     * Track the number of pending server requests.
     */
    private val pendingRequestCounter = PendingRequestCounter()

    /**
     * Live data is true when there are pending network requests.
     */
    override val loading: LiveData<Boolean> = pendingRequestCounter.getLoading()


    /**
     * The latest subscription data from the server.
     *
     * Must be observed and active in order to receive updates from the server.
     */
    override val subscriptions = MutableLiveData<List<SubscriptionStatus>>()


    /**
     * The basic content URL.
     */
    override val basicContent = MutableLiveData<ContentResource>()

    /**
     * The premium content URL.
     */
    override val premiumContent = MutableLiveData<ContentResource>()

    /**
     * Fetch basic content and post results to [basicContent].
     * This will fail if the user does not have a basic subscription.
     */
    override fun updateBasicContent() {
        val method = "updateBasicContent"
        pendingRequestCounter.incrementRequestCount()
        retrofitClient.getService().fetchBasicContent().enqueue(object :
            RetrofitResponseHandlerCallback<ContentResource?>(method, pendingRequestCounter) {
            override fun onSuccess(response: ContentResource?) {
                basicContent.postValue(response)
            }
        })
    }

    /**
     * Fetch premium content and post results to [premiumContent].
     * This will fail if the user does not have a premium subscription.
     */
    override fun updatePremiumContent() {
        val method = "updatePremiumContent"
        pendingRequestCounter.incrementRequestCount()
        retrofitClient.getService().fetchPremiumContent().enqueue(object :
            RetrofitResponseHandlerCallback<ContentResource?>(method, pendingRequestCounter) {
            override fun onSuccess(response: ContentResource?) {
                premiumContent.postValue(response)
            }
        })
    }

    /**
     * Fetches subscription data from the server and posts successful results to [subscriptions].
     */
    override fun updateSubscriptionStatus() {
        val method = "updateSubscriptionStatus"
        pendingRequestCounter.incrementRequestCount()
        retrofitClient.getService().fetchSubscriptionStatus().enqueue(object :
            RetrofitResponseHandlerCallback<Map<String, Any>>(method, pendingRequestCounter) {
            override fun onSuccess(response: Map<String, Any>) {
                onSuccessfulSubscriptionCall(response, subscriptions)
            }
        })
    }

    /**
     * Register a subscription with the server and posts successful results to [subscriptions].
     */
    override fun registerSubscription(sku: String, purchaseToken: String) {
        val method = "registerSubscription"
        val data = SubscriptionStatus()
        data.sku = sku
        data.purchaseToken = purchaseToken
        pendingRequestCounter.incrementRequestCount()
        retrofitClient.getService().registerSubscription(data).enqueue(object :
            RetrofitResponseHandlerCallback<Map<String, Any>>(method, pendingRequestCounter) {
            override fun onSuccess(response: Map<String, Any>) {
                onSuccessfulSubscriptionCall(response, subscriptions)
            }

            override fun onError(errorCode: Int, errorMessage: String) {
                if (errorCode == HttpURLConnection.HTTP_CONFLICT) {
                    Log.w(TAG, "Subscription already exists")
                    val oldSubscriptions = subscriptions.value
                    val newSubscription = SubscriptionStatus
                        .alreadyOwnedSubscription(sku, purchaseToken)
                    val newSubscriptions = insertOrUpdateSubscription(
                        oldSubscriptions,
                        newSubscription
                    )
                    subscriptions.postValue(newSubscriptions)
                }
                super.logError(errorCode, errorMessage)
            }
        })
    }

    /**
     * Transfer subscription to this account posts successful results to [subscriptions].
     */
    override fun transferSubscription(sku: String, purchaseToken: String) {
        val method = "transferSubscription"
        val data = SubscriptionStatus()
        data.sku = sku
        data.purchaseToken = purchaseToken
        pendingRequestCounter.incrementRequestCount()
        retrofitClient.getService().transferSubscription(data).enqueue(object :
            RetrofitResponseHandlerCallback<Map<String, Any>>(method, pendingRequestCounter) {
            override fun onSuccess(response: Map<String, Any>) {
                onSuccessfulSubscriptionCall(response, subscriptions)
            }
        })
    }

    /**
     * Register Instance ID when the user signs in or the token is refreshed.
     */
    override fun registerInstanceId(instanceId: String) {
        val method = "registerInstanceId"
        val data = mapOf("instanceId" to instanceId)
        pendingRequestCounter.incrementRequestCount()
        retrofitClient.getService().registerInstanceID(data).enqueue(object :
            RetrofitResponseHandlerCallback<String>(method, pendingRequestCounter) {
            override fun onSuccess(response: String) {
                // A production app may want to track whether registration has been successful to allow for retrying.
            }
        })
    }

    /**
     * Unregister when the user signs out.
     */
    override fun unregisterInstanceId(instanceId: String) {
        val method = "unregisterInstanceId"
        val data = mapOf("instanceId" to instanceId)
        pendingRequestCounter.incrementRequestCount()
        retrofitClient.getService().unregisterInstanceID(data).enqueue(object :
            RetrofitResponseHandlerCallback<String>(method, pendingRequestCounter) {
            override fun onSuccess(response: String) {
                // A production app may want to track whether un-registration has been successful to allow for retrying.
            }
        })
    }


    // Helper functions
    /**
     * Inserts or updates the subscription to the list of existing com.example.subscriptions.
     *
     *
     * If none of the existing com.example.subscriptions have a SKU that matches, insert this SKU.
     * If a subscription exists with the matching SKU, the output list will contain the new
     * subscription instead of the old subscription.
     */
    private fun insertOrUpdateSubscription(
        oldSubscriptions: List<SubscriptionStatus>?,
        newSubscription: SubscriptionStatus
    ): List<SubscriptionStatus> {
        if (oldSubscriptions == null || oldSubscriptions.isEmpty()) return listOf(newSubscription)
        val subscriptionStatuses = mutableListOf<SubscriptionStatus>()
        var subscriptionAdded = false
        for (subscription in oldSubscriptions) {
            if (TextUtils.equals(subscription.sku, newSubscription.sku)) {
                subscriptionStatuses.add(newSubscription)
                subscriptionAdded = true
            } else {
                subscriptionStatuses.add(subscription)
            }
        }
        if (!subscriptionAdded) {
            subscriptionStatuses.add(newSubscription)
        }
        return subscriptionStatuses
    }

    /**
     * Called when a successful response returns from the server
     * for a [SubscriptionStatus] HTTPS call
     *
     * @param responseBody  Successful subscription statuses response object
     * @param subscriptions LiveData subscription list
     */
    private fun onSuccessfulSubscriptionCall(
        responseBody: Map<String, Any>,
        subscriptions: MutableLiveData<List<SubscriptionStatus>>
    ) {
        val subs = responseBody["subscriptions"] as List<*>?
        if (subs == null) {
            Log.w(TAG, "Invalid subscription data")
            return
        }
        Log.i(TAG, "Valid subscription data")
        val subList = SubscriptionStatus.listFromMap(responseBody)
        subscriptions.postValue(subList)
    }

    companion object {
        private const val TAG = "RemoteServerFunction"
    }

}