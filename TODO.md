# Huezoo TODO

## IAP Bugs — fix using kmp-iap-ios + kmp-iap-android skills

### iOS — Critical: orphaned transaction drops entitlement

**File:** `composeApp/src/iosMain/kotlin/xyz/ksharma/huezoo/platform/billing/IosBillingClient.kt`

**Bug:** When the app is killed after Apple processes payment but before `finishTransaction`
runs, StoreKit queues a pending PURCHASED transaction. On next launch the observer receives
it, correctly calls `finishTransaction`, but then tries `pendingPurchase?.complete(Success)` —
which is `null` in the new session. The transaction is finished but entitlement is silently dropped.

**Fix:** Add an `onSuccess: () -> Unit` callback (injected at construction) that ALWAYS
persists entitlement regardless of whether `pendingPurchase` is live. Call it on every
PURCHASED and RESTORED transaction state — before (or instead of) completing the deferred.
Pattern: see Sumi's `StoreKitProRepository` / `kmp-iap-ios` skill.

---

### Android — Fixed: startup purchase reconciliation + listener entitlement grant

**File:** `composeApp/src/androidMain/kotlin/xyz/ksharma/huezoo/platform/billing/AndroidBillingClient.kt`

**Was:** `queryPurchasesAsync()` was never called at startup. If the app crashed after
`acknowledgePurchase` succeeded but before `settingsRepository.setPaid(true)` committed,
the user lost entitlement permanently unless they manually tapped "Restore Purchases".
The listener also early-returned when `pendingPurchase` was null, silently dropping
orphaned PURCHASED updates delivered without a live deferred.

**Now:** `AndroidBillingClient` connects eagerly in `init { }` and, on
`onBillingSetupFinished()`, calls `queryExistingPurchases()` which grants entitlement
(via `onPurchaseSuccess`) when Play's local cache shows the product as PURCHASED. The
listener also grants entitlement on every PURCHASED purchase independent of whether a
deferred is live. DI wires `onPurchaseSuccess` to `settingsRepository.setPaid(true)`,
matching the iOS shape. Pattern follows Sumi's `PlayBillingProRepository`.
