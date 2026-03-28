package xyz.ksharma.huezoo.platform.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Adaptive banner ad — fills its width, height determined by the platform SDK.
 * Shown only when the player has not purchased the Unlimited upgrade.
 *
 * Android: wraps [com.google.android.gms.ads.AdView] via AndroidView.
 * iOS: no-op stub; AdMob iOS integration deferred.
 */
@Composable
expect fun BannerAd(modifier: Modifier = Modifier)
