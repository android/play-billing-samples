/*
 * Copyright 2018 Google LLC. All rights reserved.
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

package com.example.subscriptions.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.example.subscriptions.Constants
import com.example.subscriptions.SubApp
import com.example.subscriptions.billing.deviceHasGooglePlaySubscription
import com.example.subscriptions.billing.serverHasSubscription
import com.example.subscriptions.data.SubscriptionStatus
import kotlinx.coroutines.flow.StateFlow



class BillingViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Local billing purchase data.
     */
    private val purchases = (application as SubApp).billingClientLifecycle.purchases

    /**
     * ProductDetails for all known Products.
     */
    private val productsWithProductDetails =
        (application as SubApp).billingClientLifecycle.productsWithProductDetails


    /**
     * Subscriptions record according to the server.
     */
    private val subscriptions: StateFlow<List<SubscriptionStatus>> = (application as SubApp).repository.subscriptions

    /**
     * Send an event when the Activity needs to buy something.
     */
    val buyEvent = SingleLiveEvent<BillingFlowParams>()

    /**
     * Send an event when the UI should open the Google Play
     * Store for the user to manage their subscriptions.
     */
    val openPlayStoreSubscriptionsEvent = SingleLiveEvent<String>()

    /**
     * Open the Play Store subscription center. If the user has exactly one product,
     * then open the deeplink to the specific product.
     */
    fun openPlayStoreSubscriptions() {
        val hasBasic = deviceHasGooglePlaySubscription(purchases.value, Constants.BASIC_PRODUCT)
        val hasPremium = deviceHasGooglePlaySubscription(purchases.value, Constants.PREMIUM_PRODUCT)
        Log.d(TAG, "hasBasic: $hasBasic, hasPremium: $hasPremium")
        when {
            hasBasic && !hasPremium -> {
                // If we just have a basic subscription, open the basic Product.
                openPlayStoreSubscriptionsEvent.postValue(Constants.BASIC_PRODUCT)
            }
            !hasBasic && hasPremium -> {
                // If we just have a premium subscription, open the premium Product.
                openPlayStoreSubscriptionsEvent.postValue(Constants.PREMIUM_PRODUCT)
            }
            else -> {
                // If we do not have an active subscription,
                // or if we have multiple subscriptions, open the default subscription center.
                openPlayStoreSubscriptionsEvent.call()
            }
        }
    }

    /**
     * Open the subscription page on Google Play.
     *
     * Since the purchase tokens will not be returned during account hold or pause,
     * we use the server data to determine the deeplink to Google Play.
     */
    fun openSubscriptionPageOnGooglePlay() {
        subscriptions.value.let { subscriptionStatusList ->
            when {
                serverHasSubscription(subscriptionStatusList, Constants.PREMIUM_PRODUCT) ->
                    openProductPlayStoreSubscriptions(Constants.PREMIUM_PRODUCT)
                serverHasSubscription(subscriptionStatusList, Constants.BASIC_PRODUCT) ->
                    openProductPlayStoreSubscriptions(Constants.BASIC_PRODUCT)
            }
        }
    }

    /**
     * Open the Play Store for the product subscription.
     *
     * @param product Product Id of the subscription product.
     */
    private fun openProductPlayStoreSubscriptions(product: String) {
        openPlayStoreSubscriptionsEvent.postValue(product)
    }

    /**
     * BillingFlowParams Builder for normal purchases.
     *
     * @param productDetails ProductDetails object returned by the library.
     * @param offerToken the least priced offer's offer id token returned by
     * [leastPricedOfferToken].
     *
     * @return [BillingFlowParams] builder.
     */
    private fun billingFlowParamsBuilder(productDetails: ProductDetails, offerToken: String):
            BillingFlowParams {
        return BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )
        ).build()
    }

    /**
     * BillingFlowParams Builder for upgrades and downgrades.
     *
     * @param productDetails ProductDetails object returned by the library.
     * @param offerToken the least priced offer's offer id token returned by
     * [leastPricedOfferToken].
     * @param oldToken the purchase token of the subscription purchase being upgraded or downgraded.
     *
     * @return [BillingFlowParams] builder.
     */
    private fun upDowngradeBillingFlowParamsBuilder(
        productDetails: ProductDetails, offerToken: String, oldToken: String
    ): BillingFlowParams {
        return BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )
        ).setSubscriptionUpdateParams(
            BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                .setOldPurchaseToken(oldToken)
                .setReplaceProrationMode(BillingFlowParams.ProrationMode.IMMEDIATE_AND_CHARGE_FULL_PRICE)
                .build()
        ).build()
    }


    /**
     * Calculates the lowest priced offer amongst all eligible offers.
     * In this implementation the lowest price of all offers' pricing phases is returned.
     * It's possible the logic can be implemented differently.
     * For example, the lowest average price in terms of month could be returned instead.
     *
     * @param offerDetails List of of eligible offers and base plans.
     *
     * @return the offer id token of the lowest priced offer.
     *
     */
    private fun leastPricedOfferToken(
        offerDetails: List<ProductDetails.SubscriptionOfferDetails>
    ): String {
        var offerToken = String()
        var leastPricedOffer: ProductDetails.SubscriptionOfferDetails
        var lowestPrice = Int.MAX_VALUE

        if (!offerDetails.isNullOrEmpty()) {
            for (offer in offerDetails) {
                for (price in offer.pricingPhases.pricingPhaseList) {
                    if (price.priceAmountMicros < lowestPrice) {
                        lowestPrice = price.priceAmountMicros.toInt()
                        leastPricedOffer = offer
                        offerToken = leastPricedOffer.offerToken
                    }
                }
            }
        }
        return offerToken

        TODO("Replace this with least average priced offer implementation")
    }

    /**
     * Retrieves all eligible base plans and offers using tags from ProductDetails.
     *
     * @param offerDetails offerDetails from a ProductDetails returned by the library.
     * @param tag string representing tags associated with offers and base plans.
     *
     * @return the eligible offers and base plans in a list.
     *
     */
    private fun retrieveEligibleOffers(
        offerDetails: MutableList<ProductDetails.SubscriptionOfferDetails>, tag: String
    ):
            List<ProductDetails.SubscriptionOfferDetails> {
        val eligibleOffers = emptyList<ProductDetails.SubscriptionOfferDetails>().toMutableList()
        offerDetails.forEach { offerDetail ->
            if (offerDetail.offerTags.contains(tag)) {
                eligibleOffers.add(offerDetail)
            }
        }
        return eligibleOffers
    }

    /**
     * Use the Google Play Billing Library to make a purchase.
     *
     * @param tag String representing tags associated with offers and base plans.
     * @param product Product being purchased.
     * @param upDowngrade Boolean indicating wether the purchase is an upgrade or downgrade and
     * when converting from one base plan to another.
     *
     */
    fun buyBasePlans(tag: String, product: String, upDowngrade: Boolean) {
        // First, determine whether the Product can be purchased.
        val isProductOnServer = serverHasSubscription(subscriptions.value, product)
        val isProductOnDevice = deviceHasGooglePlaySubscription(purchases.value, product)
        Log.d(
            "Billing", "$product - isProductOnServer: $isProductOnServer," +
                    " isProductOnDevice: $isProductOnDevice"
        )
        when {
            isProductOnDevice && isProductOnServer -> {
                Log.d(
                    "Billing", "User is trying to top up: $product. "
                )
            }
            isProductOnDevice && !isProductOnServer -> {
                Log.e(
                    "Billing", "The Google Play Billing Library APIs indicate that" +
                            "this Product is already owned, but the purchase token is not registered " +
                            "with the server. There might be an issue registering the " +
                            "purchase token."
                )
                return
            }
            !isProductOnDevice && isProductOnServer -> {
                Log.w(
                    "Billing", "WHOA! The server says that the user already owns " +
                            "this item: $product. This could be from another Google account. " +
                            "You should warn the user that they are trying to buy something " +
                            "from Google Play that they might already have access to from " +
                            "another purchase, possibly from a different Google account " +
                            "on another device.\n" +
                            "You can choose to block this purchase.\n" +
                            "If you are able to cancel the existing subscription on the server, " +
                            "you should allow the user to subscribe with Google Play, and then " +
                            "cancel the subscription after this new subscription is complete. " +
                            "This will allow the user to seamlessly transition their payment " +
                            "method from an existing payment method to this Google Play account."
                )
                return
            }
        }

        // Then get the ProductDetails of the product being purchased.
        val productDetails = productsWithProductDetails.value?.get(product) ?: run {
            Log.e(TAG, "Could not find ProductDetails to make purchase.")
            return
        }

        // Retrieve all offers the user is eligible for.
        val offers =
            productDetails.subscriptionOfferDetails?.let { offerDetailsList ->
                retrieveEligibleOffers(
                    offerDetails = offerDetailsList,
                    tag = tag
                )
            }

        //  Get the offer id token of the lowest priced offer.
        val offerToken = offers?.let { leastPricedOfferToken(it) }

        var oldToken = ""
        if (upDowngrade) {
            // The purchase is for an upgrade or downgrade, therefore the existing purchase's
            // token is retrieved.
            for (purchase in purchases.value) {
                oldToken = purchase.purchaseToken
            }

            // Use [upDowngradeBillingFlowParamsBuilder] to build the Params that describe the
            // product to be purchased and the offer to purchase with.
            val billingParams = offerToken?.let { token ->
                upDowngradeBillingFlowParamsBuilder(
                    productDetails = productDetails,
                    offerToken = token,
                    oldToken = oldToken
                )
            }

            // Finally, Launch billing flow.
            buyEvent.postValue(billingParams)
        } else {
            // This is a normal purchase for auto-renewing base plans and/or top-up for prepaid
            // base plans.

            // Use [billingFlowParamsBuilder] to build the Params that describe the product to be
            // purchased and the offer to purchase with.
            val billingParams = offerToken?.let { token ->
                billingFlowParamsBuilder(
                    productDetails = productDetails,
                    offerToken = token
                )
            }

            // Finally, Launch billing flow.
            buyEvent.postValue(billingParams)
        }
    }

    companion object {
        const val TAG = "BillingViewModel"
    }
}
