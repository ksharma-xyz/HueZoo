# Huezoo — Billing Implementation

Technical reference for the in-app purchase system. For the monetization
*design* (free vs paid features, ad rules) see `MONETIZATION_DESIGN.md`.

---

## Product

| Field | Value |
|---|---|
| Product ID | `xyz.ksharma.huezoo.unlimited` |
| Type | Non-consumable (one-time purchase) |
| Price | $2.99 (base — App Store / Play Store adjusts per region) |
| Constant | `PRODUCT_UNLIMITED` in `BillingClient.kt` |

---

## Architecture

```
UpgradeScreen  ──►  UpgradeViewModel
                         │
                         ▼
                   BillingClient (interface)
                    /              \
        IosBillingClient     AndroidBillingClient
        (StoreKit 1)         (Play Billing Library 6)
                         │
                         ▼
                  SettingsRepository
                  (isPaid persisted to SQLDelight DB)
```

`BillingClient` is a Koin singleton injected into `UpgradeViewModel`.
The platform implementation is bound in `iosModule` / the Android DI module.
`isPaid` is read on every screen via `SettingsRepository` — ads and attempt
limits key off this flag.

---

## iOS — StoreKit 1

Uses the ObjC StoreKit 1 API (fully accessible from Kotlin/Native without a
Swift bridge). StoreKit 2 (Swift concurrency) requires additional interop
and was deferred.

### Initialisation

`MobileAds.shared.start()` is called in `AppDelegate.application(_:didFinishLaunchingWithOptions:)`
in `iOSApp.swift`. The `SKPaymentQueue` observer is registered in
`IosBillingClient.init` and stays alive for the app's lifetime (Koin singleton).

### Purchase flow

1. `UpgradeViewModel.onPurchase()` calls `IosBillingClient.purchase(productId)`
2. `fetchProduct()` calls `SKProductsRequest` with the product ID
   — the delegate (`SelfRetainingProductsDelegate`) self-retains to survive
   GC before the async callback fires (StoreKit delegate is a weak ObjC ref)
3. On success, `SKPayment.paymentWithProduct` is added to `SKPaymentQueue`
4. The transaction observer fires `paymentQueue(_:updatedTransactions:)`:
   - `SKPaymentTransactionStatePurchased` → `finishTransaction` + `PurchaseResult.Success`
   - `SKPaymentTransactionStateFailed` with `SKErrorPaymentCancelled (code 2)` → `PurchaseResult.Cancelled`
   - `SKPaymentTransactionStateFailed` other → `PurchaseResult.Error(message)`
5. ViewModel receives `PurchaseResult`, calls `settingsRepository.setPaid(true)` on success

### Restore flow

1. `UpgradeViewModel.onRestorePurchases()` calls `IosBillingClient.isOwned(productId)`
2. `SKPaymentQueue.defaultQueue().restoreCompletedTransactions()` is called
   — Apple may show a sign-in dialog; this is expected
3. Observer receives `SKPaymentTransactionStateRestored` for each owned product,
   collecting IDs into `restoredIds`
4. `paymentQueueRestoreCompletedTransactionsFinished` fires → deferred completes
   with the full set of restored product IDs
5. `isOwned` returns `productId in restoredIds`
6. On `true`: `settingsRepository.setPaid(true)` is called
7. On `false`: ViewModel sets `error = "No previous purchase found for this Apple ID."`

**Important:** Restore only returns results for purchases that have actually
been completed. Until `xyz.ksharma.huezoo.unlimited` is approved by Apple
and at least one purchase made, restore will always return empty.

### App Store Connect setup (one-time)

1. App Store Connect → your app → Features → In-App Purchases → **+**
2. Type: **Non-Consumable**
3. Product ID: `xyz.ksharma.huezoo.unlimited`
4. Set display name, description, price tier
5. Upload a review screenshot (screenshot of the Upgrade screen in-app)
6. Submit the IAP alongside your first app version for review
   — Apple reviews IAP and app version together

### Testing on iOS

| Scenario | How |
|---|---|
| Purchase flow | Create a Sandbox tester in App Store Connect → Users and Access → Sandbox Testers. On device: Settings → App Store → Sandbox Account |
| Restore | Make a sandbox purchase first, then reinstall or sign out/in |
| "Product not found" | IAP not yet created in App Store Connect, or product ID mismatch |
| Price shows "$2.99" (fallback) | `SKProductsRequest` failed — check App Store Connect setup and network |

---

## Android — Play Billing Library 6

### Purchase flow

1. `UpgradeViewModel.onPurchase()` calls `AndroidBillingClient.purchase(productId)`
2. `ensureConnected()` starts a `BillingClient` connection if not already ready
3. `queryProductDetailsInternal()` calls `queryProductDetails` with `INAPP` type
4. `googleClient.launchBillingFlow(activity, flowParams)` shows the Play Store sheet
   — requires a live `Activity` from `AndroidActivityProvider`
5. `PurchasesUpdatedListener` fires:
   - `BillingResponseCode.OK` + non-empty purchases → `acknowledgePurchase` + `PurchaseResult.Success`
   - `USER_CANCELED` → `PurchaseResult.Cancelled`
   - Any other code → `PurchaseResult.Error(debugMessage)`
6. Non-acknowledged purchases are voided by Google after 3 days — `acknowledgePurchase`
   is called immediately after each successful purchase

### Restore / ownership check

`AndroidBillingClient.isOwned(productId)` calls `queryPurchasesAsync` with
`ProductType.INAPP`. This queries the Play Store cache locally — no network
round-trip needed, and no user-visible dialog. Returns `true` if the product ID
appears in the user's purchase list.

**Note:** Google Play restores purchases automatically on reinstall for the same
Google account. A manual "Restore Purchases" button is not required by Google
Play policy (unlike Apple), which is why it's currently only in `UpgradeScreen`
(shared code) but not in `SettingsScreen`.
See `SettingsScreen.kt` ACCOUNT section TODO for adding it there later.

### Google Play setup (one-time)

1. Google Play Console → your app → Monetise → In-app products → **Create product**
2. Product ID: `xyz.ksharma.huezoo.unlimited`
3. Type: One-time product
4. Set title, description, price
5. Status: **Active** (inactive products return empty from `queryProductDetails`)
6. The app must be published to at least Internal Testing before IAP products
   can be queried on real devices

### Testing on Android

| Scenario | How |
|---|---|
| Purchase flow | Add a tester Gmail to Play Console → License Testing; purchases are free and instant |
| "Product not found" | IAP not Active in Play Console, or app not published to any track |
| Restore | Re-install the app on the same Google account — Play restores automatically |

---

## UpgradeUiState — field reference

| Field | Set when |
|---|---|
| `priceLabel` | On init, from `billingClient.queryPrice()`. Falls back to `"$2.99"` if the store call fails |
| `isPurchasing` | `true` while the purchase sheet is open / transaction pending |
| `isRestoring` | `true` while `restoreCompletedTransactions` / `queryPurchasesAsync` is in flight |
| `isPaid` | `true` after a successful purchase or restore; persisted via `SettingsRepository` |
| `error` | Set on `PurchaseResult.Error`, failed restore, or "nothing to restore". Cleared on next action |

---

## Paid state persistence

`settingsRepository.setPaid(true)` writes to SQLDelight (same DB used for
game progress). `isPaid` is read on splash via `SplashViewModel` and flows
into `PlayerState`, which every screen checks before showing ads or blocking
Threshold attempts. No network verification is done at runtime — the local
DB is the source of truth after a successful purchase/restore.
