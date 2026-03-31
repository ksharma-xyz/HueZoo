package xyz.ksharma.huezoo.ui.paywall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lexilabs.basic.ads.DependsOnGoogleMobileAds
import app.lexilabs.basic.ads.composable.RewardedAd
import xyz.ksharma.huezoo.platform.ads.AdIds
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.huezoo.ui.components.HuezooButton
import xyz.ksharma.huezoo.ui.components.HuezooButtonVariant
import xyz.ksharma.huezoo.ui.components.HuezooHeadlineMedium
import xyz.ksharma.huezoo.ui.components.HuezooLabelSmall
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing

/**
 * Out-of-tries refill sheet content.
 * Must be hosted inside [xyz.ksharma.huezoo.ui.components.HuezooBottomSheet].
 *
 * Three escape paths:
 * 1. Spend gems (disabled when balance < cost)
 * 2. Watch rewarded ad (disabled until loaded)
 * 3. Unlock Forever → navigates to UpgradeScreen via [onUnlock]
 */
@Composable
fun PaywallSheet(
    onWatchAd: () -> Unit,
    onUnlock: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaywallViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Capture the count when the sheet opens. Dismiss only when it rises — safe for
    // Activity-scoped ViewModels where the counter persists across sheet re-opens.
    val initialGrantCount = remember { state.tryGrantedCount }
    LaunchedEffect(state.tryGrantedCount) {
        if (state.tryGrantedCount > initialGrantCount) onDismiss()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HuezooSpacing.md, vertical = HuezooSpacing.lg)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
    ) {
        HuezooHeadlineMedium(
            text = "OUT OF TRIES",
            color = HuezooColors.TextPrimary,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )

        HuezooLabelSmall(
            text = "Refill with gems, watch an ad, or go unlimited.",
            color = HuezooColors.TextSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(HuezooSpacing.xs))

        val canSpendGems = state.gemBalance >= PaywallViewModel.GEM_COST
        HuezooButton(
            // Sheet auto-dismisses via LaunchedEffect(tryGranted) once DB writes complete
            text = if (state.isSpendingGems) "SPENDING…" else "SPEND ${PaywallViewModel.GEM_COST} GEMS  —  +1 TRY",
            onClick = { viewModel.onSpendGems() },
            variant = if (canSpendGems) HuezooButtonVariant.Primary else HuezooButtonVariant.Ghost,
            enabled = canSpendGems && !state.isSpendingGems,
            modifier = Modifier.fillMaxWidth(),
        )

        HuezooButton(
            // Keep sheet in composition so RewardedAd composable stays alive during the ad
            text = "WATCH AD  —  +1 TRY",
            onClick = { viewModel.onWatchAd() },
            variant = HuezooButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth(),
        )

        @OptIn(DependsOnGoogleMobileAds::class)
        if (state.showRewardedAd) {
            RewardedAd(
                adUnitId = AdIds.rewarded,
                onRewardEarned = { _ -> viewModel.onRewardEarned() },
                onDismissed = { viewModel.onAdDismissed() },
                onFailure = { _ -> viewModel.onAdDismissed() },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
        ) {
            HuezooButton(
                text = "UNLOCK FOREVER",
                onClick = onUnlock,
                variant = HuezooButtonVariant.Primary,
                modifier = Modifier.weight(1f),
            )
            HuezooButton(
                text = "LATER",
                onClick = onDismiss,
                variant = HuezooButtonVariant.Ghost,
            )
        }

        HuezooLabelSmall(
            text = "Your balance: ${state.gemBalance} 💎",
            color = HuezooColors.TextDisabled,
            textAlign = TextAlign.Center,
        )
    }
}
