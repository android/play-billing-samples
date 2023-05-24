/*
 * Copyright 2020 Google LLC. All rights reserved.
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

package com.sample.android.classytaxijava.data;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Local subscription data. This is stored on disk in a database.
 */
@Entity(tableName = "subscriptions")
public class SubscriptionStatus {

    // Local fields
    @PrimaryKey(autoGenerate = true)
    private int primaryKey = 0;
    @Nullable
    private String subscriptionStatusJson;
    private boolean subAlreadyOwned;
    private boolean isLocalPurchase;

    // Remote fields
    @Nullable
    private String sku;
    @Nullable
    private String purchaseToken;
    private boolean isEntitlementActive;
    private boolean willRenew;
    private Long activeUntilMillisec;
    private boolean isFreeTrial;
    private boolean isGracePeriod;
    private boolean isAccountHold;
    private boolean isPaused;
    private boolean isAcknowledged;
    private Long autoResumeTimeMillis;

    public boolean isWillRenew() {
        return willRenew;
    }

    public void setWillRenew(boolean willRenew) {
        this.willRenew = willRenew;
    }

    @Nullable
    public String getPurchaseToken() {
        return purchaseToken;
    }

    public void setPurchaseToken(@Nullable String purchaseToken) {
        this.purchaseToken = purchaseToken;
    }

    public boolean isGracePeriod() {
        return isGracePeriod;
    }

    public void setGracePeriod(boolean gracePeriod) {
        isGracePeriod = gracePeriod;
    }

    public boolean isEntitlementActive() {
        return isEntitlementActive;
    }

    public void setEntitlementActive(boolean entitlementActive) {
        isEntitlementActive = entitlementActive;
    }

    public Long getActiveUntilMillisec() {
        return activeUntilMillisec;
    }

    public void setActiveUntilMillisec(Long activeUntilMillisec) {
        this.activeUntilMillisec = activeUntilMillisec;
    }

    public boolean isFreeTrial() {
        return isFreeTrial;
    }

    public void setFreeTrial(boolean freeTrial) {
        isFreeTrial = freeTrial;
    }

    @Nullable
    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public boolean isAccountHold() {
        return isAccountHold;
    }

    public void setAccountHold(boolean accountHold) {
        isAccountHold = accountHold;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
    }

    public boolean isAcknowledged() {
        return isAcknowledged;
    }

    public void setAcknowledged(boolean acknowledged) {
        isAcknowledged = acknowledged;
    }

    public Long getAutoResumeTimeMillis() {
        return autoResumeTimeMillis;
    }

    public void setAutoResumeTimeMillis(Long autoResumeTimeMillis) {
        this.autoResumeTimeMillis = autoResumeTimeMillis;
    }

    public int getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(int primaryKey) {
        this.primaryKey = primaryKey;
    }

    @Nullable
    public String getSubscriptionStatusJson() {
        return subscriptionStatusJson;
    }

    public void setSubscriptionStatusJson(@Nullable String subscriptionStatusJson) {
        this.subscriptionStatusJson = subscriptionStatusJson;
    }

    public boolean isSubAlreadyOwned() {
        return subAlreadyOwned;
    }

    public void setSubAlreadyOwned(boolean subAlreadyOwned) {
        this.subAlreadyOwned = subAlreadyOwned;
    }

    public boolean isLocalPurchase() {
        return isLocalPurchase;
    }

    public void setLocalPurchase(boolean localPurchase) {
        isLocalPurchase = localPurchase;
    }

    /**
     * Create a record for a subscription that is already owned by a different user.
     * <p>
     * The server does not return JSON for a subscription that is already owned by
     * a different user, so we need to construct a local record with the basic fields.
     */
    public static SubscriptionStatus alreadyOwnedSubscription(String sku, String purchaseToken) {
        SubscriptionStatus subscriptionStatus = new SubscriptionStatus();
        subscriptionStatus.sku = sku;
        subscriptionStatus.purchaseToken = purchaseToken;
        subscriptionStatus.isEntitlementActive = false;
        subscriptionStatus.subAlreadyOwned = true;
        return subscriptionStatus;
    }

    @Override
    public String toString() {
        return "SubscriptionStatus{" +
                "primaryKey=" + primaryKey +
                ", subscriptionStatusJson='" + subscriptionStatusJson + '\'' +
                ", subAlreadyOwned=" + subAlreadyOwned +
                ", isLocalPurchase=" + isLocalPurchase +
                ", sku='" + sku + '\'' +
                ", purchaseToken='" + purchaseToken + '\'' +
                ", isEntitlementActive=" + isEntitlementActive +
                ", willRenew=" + willRenew +
                ", activeUntilMillisec=" + activeUntilMillisec +
                ", isFreeTrial=" + isFreeTrial +
                ", isGracePeriod=" + isGracePeriod +
                ", isAccountHold=" + isAccountHold +
                '}';
    }
}
