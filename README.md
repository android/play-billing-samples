# Google Play Billing Samples

Sample applications for Google Play Billing. To build each sample, see the README instructions in the project directory.

* [Trivial Drive Java](https://github.com/android/play-billing-samples/tree/master/TrivialDriveJava) - Purchase items/subscriptions in your Android app (serverless).
* [Trivial Drive Kotlin](https://github.com/android/play-billing-samples/tree/master/TrivialDriveKotlin) - Purchase items/subscriptions in your Android app (local and server).
* [Classy Taxi Kotlin App](https://github.com/android/play-billing-samples/tree/master/ClassyTaxiAppKotlin) - Purchase subscriptions in your Android app and manage subscriptions on your server.
* [Classy Taxi Java App](https://github.com/android/play-billing-samples/tree/master/ClassyTaxiJava) - Purchase subscriptions in your Android app and manage subscriptions on your server.
* [Classy Taxi Server](https://github.com/android/play-billing-samples/tree/master/ClassyTaxiServer) - Manage subscriptions on your server.


## Google Play Billing

For more information about Google Play Billing, see the documentation.

* [Google Play Billing](https://developer.android.com/google/play/billing/billing_overview)
* [Google Play Billing Library](https://developer.android.com/google/play/billing/billing_library_overview)
* [Google Play Developer API](https://developer.android.com/google/play/developer-api)
* [Real-time Developer Notifications](https://developer.android.com/google/play/billing/realtime_developer_notifications)


# CHANGELOG

* **2021-05-18**
  * Updated all samples for Google Play Billing Library v4.
* **2021-04-28**
  * Publish `TrivialDriveJava` : Billing Library Java sample for purchases.
  * Rewrite `TrivialDriveKotlin` : Billing Library Java/Kotlin hybrid sample for purchases, now supports billing ktx/coroutines.
* **2020-02-28**
  * Publish `ClassyTaxiJava`: Billing Library Java sample - currently only supports subscriptions.
* **2019-12-30**
  * Restructure `ClassyTaxi` to separate Android, web, and server implementations.
    * The `ClassyTaxi` Android app now lives [here](https://github.com/android/play-billing-samples/tree/master/ClassyTaxiAppKotlin).
    * The `ClassyTaxi` read-only web app now lives [here](https://github.com/android/play-billing-samples/tree/master/ClassyTaxiAppWeb).
    * The `ClassyTaxi` server implementation now lives [here](https://github.com/android/play-billing-samples/tree/master/ClassyTaxiServer).
* **2019-10-10**
  * Remove apps that do not support Google Play Billing Library 2.0 or newer.
    The AIDL and older versions of the Google Play Billing Library are deprecated.
    The old samples are available on GitHub at the following links:
    * [`TrivialDrive` archive](https://github.com/android/play-billing-samples/tree/7a94c6905a9c125518354c216b5c3094fde47ce1/TrivialDrive)
    * [`TrivialDrive_v2` archive](https://github.com/android/play-billing-samples/tree/7a94c6905a9c125518354c216b5c3094fde47ce1/TrivialDrive_v2)
* **2019-01-16**
  * Publish `TrivialDriveKotlin`: Kotlin sample
* **2018-05-03**
  * Publish `ClassyTaxi`: Subscriptions sample
* **2017-07-12**
  * Publish `TrivialDrive_v2`: Billing Library 1.0 sample
* **2015-09-18**
  * Initial repo port to GitHub
  * `TrivialDrive`: AIDL sample
