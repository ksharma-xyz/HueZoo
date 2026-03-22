package xyz.ksharma.huezoo.ui.upgrade

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import xyz.ksharma.huezoo.ui.components.AmbientGlowBackground
import xyz.ksharma.huezoo.ui.components.HuezooBodyMedium
import xyz.ksharma.huezoo.ui.components.HuezooLabelSmall
import xyz.ksharma.huezoo.ui.components.HuezooTitleLarge
import xyz.ksharma.huezoo.ui.components.HuezooTopBar
import xyz.ksharma.huezoo.ui.components.PriceButton
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewScreen
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing

/**
 * Paywall screen — the single entry point for all purchase flows.
 *
 * Navigation entrypoints:
 *  • Home screen (GET FULL ACCESS button below Threshold card, shown when blocked + free)
 *  • Result screen (future: upsell after out-of-tries game)
 *  • Any future placement that needs to drive upgrade
 *
 * Always use this screen for purchase CTAs — never inline a [PriceButton] in another screen.
 *
 * TODO: Wire [onPurchase] to real in-app purchase (Google Play Billing / StoreKit 2).
 *       Price string should come from the store, not hardcoded.
 */
@Composable
fun UpgradeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AmbientGlowBackground(
        modifier = modifier,
        primaryColor = HuezooColors.PriceGreen,
        secondaryColor = HuezooColors.AccentCyan,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            HuezooTopBar(
                onBackClick = onBack,
                currencyAmount = null,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = HuezooSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(HuezooSpacing.xxl))

                HuezooLabelSmall(
                    text = "FULL ACCESS",
                    color = HuezooColors.PriceGreen,
                    fontWeight = FontWeight.ExtraBold,
                )

                Spacer(Modifier.height(HuezooSpacing.sm))

                HuezooTitleLarge(
                    text = "Train Without Limits",
                    color = HuezooColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(HuezooSpacing.sm))

                HuezooBodyMedium(
                    text = "Free players get 5 Threshold tries per day. Full access removes that limit — play as many sessions as you want.",
                    color = HuezooColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(HuezooSpacing.xxl))

                FeatureList()

                Spacer(Modifier.height(HuezooSpacing.xxl))
            }

            // Purchase CTA — pinned at bottom above nav bar
            Column(
                modifier = Modifier.padding(horizontal = HuezooSpacing.lg),
            ) {
                PriceButton(
                    price = "GET FULL ACCESS — $2.99", // TODO: fetch price from store
                    onClick = { /* TODO: trigger IAP purchase */ },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(HuezooSpacing.sm))

                HuezooLabelSmall(
                    text = "One-time purchase · No subscription · Supports indie dev",
                    color = HuezooColors.TextDisabled,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(HuezooSpacing.lg))
            }
        }
    }
}

// ── Feature list ─────────────────────────────────────────────────────────────

private val FEATURES = listOf(
    "Unlimited Threshold sessions per day" to "Free tier: 5 tries. Full access: unlimited.",
    "Daily Challenge" to "Always free for everyone.",
    "Leaderboard & personal bests" to "Track your ΔE improvement over time.",
    "All future game modes" to "New modes added — no extra charge.",
)

@Composable
private fun FeatureList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(HuezooColors.SurfaceL2, RoundedCornerShape(16.dp))
            .border(1.dp, HuezooColors.SurfaceL4, RoundedCornerShape(16.dp))
            .padding(HuezooSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(HuezooSpacing.lg),
    ) {
        FEATURES.forEach { (title, subtitle) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.md),
                verticalAlignment = Alignment.Top,
            ) {
                HuezooLabelSmall(
                    text = "✓",
                    color = HuezooColors.PriceGreen,
                    fontWeight = FontWeight.ExtraBold,
                )
                Column {
                    HuezooLabelSmall(
                        text = title,
                        color = HuezooColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    HuezooLabelSmall(
                        text = subtitle,
                        color = HuezooColors.TextSecondary,
                    )
                }
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@PreviewScreen
@Composable
private fun UpgradeScreenPreview() {
    HuezooPreviewTheme {
        UpgradeScreen(onBack = {})
    }
}
