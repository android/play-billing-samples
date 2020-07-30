/**
 * Copyright 2018 Google LLC. All Rights Reserved.
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

import { SubscriptionPurchase, SkuType, Purchase } from "./types/purchases";
import { PurchaseQueryError } from "./types/errors";
import { DeveloperNotification, NotificationType } from "./types/notifications";

/*
 * A class that provides user-purchase linking features
 */
export default class PurchaseManager {
  /*
   * This class is intended to be initialized by the library.
   * Library consumer should not initialize this class themselves.
   */
  constructor(private playDeveloperApiClient: any) { };

  /*
   * Query a onetime product purchase by its package name, product Id (sku) and purchase token.
   * The method queries Google Play Developer API to get the latest status of the purchase,
   * then merge it with purchase ownership info stored in the library's managed Firestore database,
   * then returns the merge information as a OneTimeProductPurchase to its caller.
   */
  async queryOneTimeProductPurchase(packageName: string, sku: string, purchaseToken: string): Promise<any> {
    // STEP 1. Query Play Developer API to verify the purchase token
    await new Promise((resolve, reject) => {
      this.playDeveloperApiClient.purchases.products.get({
        packageName: packageName,
        productId: sku,
        token: purchaseToken
      }, (err: any, result: { data: any }) => {
        if (err) {
          reject(this.convertPlayAPIErrorToLibraryError(err));
        } else {
          resolve(result.data);
        }
      })
    });
  }

  /*
   * Query a subscription purchase by its package name, product Id (sku) and purchase token.
   * The method queries Google Play Developer API to get the latest status of the purchase,
   * then merge it with purchase ownership info stored in the library's managed Firestore database,
   * then returns the merge information as a SubscriptionPurchase to its caller.
   */
  querySubscriptionPurchase(packageName: string, sku: string, purchaseToken: string): Promise<SubscriptionPurchase> {
    return this.querySubscriptionPurchaseWithTrigger(packageName, sku, purchaseToken);
  }

  /*
   * Actual private information of querySubscriptionPurchase(packageName, sku, purchaseToken)
   * It's expanded to support storing extra information only available via Realtime Developer Notification,
   * such as latest notification type.
   *  - triggerNotificationType is only neccessary if the purchase query action is triggered by a Realtime Developer notification
   */
  private async querySubscriptionPurchaseWithTrigger(packageName: string, sku: string, purchaseToken: string): Promise<any> {
    // STEP 1. Query Play Developer API to verify the purchase token
     await new Promise((resolve, reject) => {
      this.playDeveloperApiClient.purchases.subscriptions.get({
        packageName: packageName,
        subscriptionId: sku,
        token: purchaseToken
      }, (err: any, result: { data: any }) => {
        if (err) {
          reject(this.convertPlayAPIErrorToLibraryError(err));
        } else {
          resolve(result.data);
        }
      })
    });
  }

  /*
   * Another method to query latest status of a Purchase.
   * Internally it just calls queryOneTimeProductPurchase / querySubscriptionPurchase accordingly
   */
  async queryPurchase(packageName: string, sku: string, purchaseToken: string, skuType: SkuType): Promise<Purchase> {
    if (skuType === SkuType.ONE_TIME) {
      return await this.queryOneTimeProductPurchase(packageName, sku, purchaseToken);
    } else if (skuType === SkuType.SUBS) {
      return await this.querySubscriptionPurchase(packageName, sku, purchaseToken);
    } else {
      throw new Error('Invalid skuType.');
    }
  }

  async processDeveloperNotification(packageName: string, notification: DeveloperNotification): Promise<SubscriptionPurchase | null> {
    if (notification.testNotification) {
      console.log('Received a test Realtime Developer Notification. ', notification.testNotification);
      return null;
    }

    // Received a real-time developer notification.
    const subscriptionNotification = notification.subscriptionNotification;
    if (subscriptionNotification?.notificationType !== NotificationType.SUBSCRIPTION_PURCHASED) {
      // We can safely ignoreSUBSCRIPTION_PURCHASED because with new subscription, our Android app will send the same token to server for verification
      // For other type of notification, we query Play Developer API to update our purchase record cache in Firestore
      return await this.querySubscriptionPurchaseWithTrigger(packageName,
        subscriptionNotification?.subscriptionId ?? "",
        subscriptionNotification?.purchaseToken ?? ""
      );
    }

    return null;
  }

  private convertPlayAPIErrorToLibraryError(playError: any): Error {
    const libraryError = new Error(playError.message);
    if (playError.code === 404) {
      libraryError.name = PurchaseQueryError.INVALID_TOKEN;
    } else {
      // Unexpected error occurred. It's likely an issue with Service Account
      libraryError.name = PurchaseQueryError.OTHER_ERROR;
      console.error('Unexpected error when querying Google Play Developer API. Please check if you use a correct service account');
    }
    return libraryError;
  }
}