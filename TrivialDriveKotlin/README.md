# TrivialDrive (Kotlin)

Sample for the Google Play Billing 4.1 integration

![](img/screenshot.png)

This sample is provided to demonstrate using the Google Play Billing Library to
implement in-app products and subscriptions. In particular, following
functionalities are showcased:

1. Fetching and listing products and offers
1. Upgrading and downgrading subscriptions
1. Handling consumable and non-consumable products
1. Local and remote purchase validation

This app is a simple "driving" game where the player can buy gas and drive. The
car has a tank which stores gas. When the player purchases gas, the tank fills
up. When the player drives the tank diminishes.

The user can refuel by exchanging it's virtual currency for gas. There is a
standard fuel price but one of the subscriptions can be bought in order to
refuel for a better rate. More virtual currency can be bought as an in-game
purchase.

Additionally, users can purchase alternative cars as well.

## Setup

### 1. Local and remote variants
This sample showcases two approaches to implementing Google Play billing in your
game or app:

1. Local - Simpler but less safe solution that does not require having a
   separate server. The app only communicates with the Google Play Billing
   backend. Purchase validation and processing are done locally.
2. Remote - Requires independent server but is much safer. Having a remote
   backend is often the only way to prevent yourself from certain
   vulnerabilities such as replayed purchases or person-in-the-middle attack.
   We highly recommend this approach.

Simply select the desired application flavor in Android Studio.

![](img/variant.png)

### 2. Products and subscriptions
The sample offers 7 distinct products: 2 non-consumables, 3 consumables and 2
subscriptions. Add in-app products and subscriptions listed below to your
project in the Google Play Developer Console. See
[this guide to learn how to setup in-app products](https://support.google.com/googleplay/android-developer/answer/1153481?hl=en)
and
[this one about subscriptions](https://support.google.com/googleplay/android-developer/answer/140504?hl=en&ref_topic=3452890).

|SKU|Type|Description|
|-|-|-|
|car_truck|Non-consumable product|Alternative car|
|car_pickup|Non-consumable product|Alternative car|
|currency_pack_small|Consumable product|Small pack of the in-game currency|
|currency_pack_medium|Consumable product|Medium pack of the in-game currency|
|currency_pack_large|Consumable product|Large pack of the in-game currency|
|plan_silver|Subscription|Subscription for cheaper fuel prices|
|plan_gold|Subscription|Subscription for even cheaper fuel prices|

### 3. Licensing key
Copy the base64-encoded RSA public key and define `base64EncodedPublicKey` in
your `local.properties` file. The key can be found in Google Play Developer
console under Your App -> Monetization Setup -> Licensing.

![](img/licensing.png)

### 4. Package name
Change the sample's package name to your package name. To do that, update the
package name in AndroidManifest.xml and correct the references (especially the
references to the R object).

### 5. Server setup (remote variant only)

The server is implemented with [Ktor](https://ktor.io/) and
[SQLDelight](https://github.com/cashapp/sqldelight). It's purpose is to
communicate with Play Billing backend to fetch available products and process
purchases.

In order to run the server you need to specify two parameters in your
`local.properties` file:
1. `DATABASE_URL` - url to a sqlite database. For simplicity, this can be an
   in-memory database, such as `jdbc:sqlite::memory:trivial_drive`
1. `SERVICE_ACCOUNT_PATH` - full path to your service account json file.
   Service account is necessary for the server to use the
   [**Google Play Developer API**](https://developers.google.com/android-publisher)
   (aka Android Publisher). If you do not have a service account yet
   [refer to those instructions](https://developers.google.com/android-publisher/getting_started).

## Server side validation

The server handles part of the game's business logic and purchase processing.
It's endpoints are grouped into three areas:
1. `/game/...` - Anything related to general game logic. Kinda like a remote
   config.
1. `/player/...` - Player operations and data. Those endpoints always have a
   user id in their path.
1. `/billing/...` - All things related to billing.

|Url|Method|URL params|Description|
|-|-|-|-|
|`/game/cars`|GET|-|Returns list of cars available to the player|
|`/game/fuelPrices`|GET|-|Returns list of fuel price plans available to the player|
|`/player/:id`|GET|`id=[integer]` - player id|Returns player properties|
|`/player/:id/refuel`|GET|`id=[integer]` - player id<br>`plan=[integer]` - fuel price plan id<br>`amount=[integer]` - amount of fuel to acquire|Exchanges virtual currency for fuel. The value of spent virtual currency is calculated using fuel price plan and `amount` param|
|`/player/:id/drive`|GET|`id=[integer]` - player id|Simulates gameplay. Depletes a certain amount of fuel.|
|`/billing/products`|GET|`package=[alphanumeric]` - app's package name|Returns a list of available in-app products (consumable and non-consumable)|
|`/billing/subscriptions`|GET|`package=[alphanumeric]` - app's package name|Returns a list of available subscriptions|
|`/billing/:id/process`|GET|`id=[integer]` - player id<br>`package=[alphanumeric]` - app's package name<br>`product=[alphanumeric]` - product or subscription SKU<br>`token=[alphanumeric]` - purchase token|Acknowledges purchase and grants entitlements to the player (if applicable)|

## A NOTE ABOUT SECURITY
This sample app implements signature verification but does not demonstrate how
to enforce a tight security model. When releasing a production application to
the general public, we highly recommend that you implement the security best
practices described in our documentation at:

https://developer.android.com/google/play/billing/security

## Support
If you've found an error in this sample, please file an issue:
https://github.com/googlesamples/android-play-billing/issues

Patches are encouraged, and may be submitted by forking this project and
submitting a pull request through GitHub.

## Changelog

* 2012-11-29: Initial release 
* 2013-01-08: Updated to include support for subscriptions 
* 2015-03-13: Updated to new dev console and added yearly subscriptions
* 2015-08-27: Ported to gradle and prepped for transitioning to GitHub 
* 2021-04-28: Rewritten and updated to support Google Play Billing Library V3
* 2021-12-13: Refactored to uphold modern standards of Android app development,
  support Play Billing Library V4.1 and showcase server side validation
