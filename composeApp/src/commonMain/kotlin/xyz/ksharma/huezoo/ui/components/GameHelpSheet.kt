package xyz.ksharma.huezoo.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing

// ── Threshold ─────────────────────────────────────────────────────────────────

/**
 * Bottom sheet explaining The Threshold game rules in plain language.
 * Triggered by the `?` button in the top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThresholdHelpSheet(onDismiss: () -> Unit) {
    HuezooBottomSheet(onDismissRequest = onDismiss) {
        HelpSheetBody(
            title = "HOW THRESHOLD WORKS",
            accentColor = HuezooColors.GameThreshold,
            sections = thresholdHelpSections,
        )
    }
}

private val thresholdHelpSections = listOf(
    HelpSection(
        heading = "THE GOAL",
        body = "Six swatches appear — one has a slightly different hue. Tap the outlier.",
    ),
    HelpSection(
        heading = "TAPS",
        body = "Each correct tap makes the colour difference smaller by ΔE 0.3. " +
            "The longer your streak, the harder it gets. How far can you push it?",
    ),
    HelpSection(
        heading = "TRIES",
        body = "One wrong tap ends the current try — your ΔE resets to 5.0 and a new run begins. " +
            "You get 10 tries per session. Your best ΔE across all tries is your result.",
    ),
    HelpSection(
        heading = "WHAT IS ΔE?",
        body = "ΔE (delta-E) measures colour difference. " +
            "Higher = more obvious. Lower = near-invisible.\n\n" +
            "ΔE 5.0  →  Easy — clearly different\n" +
            "ΔE 2.0  →  Hard — trained eye needed\n" +
            "ΔE 1.0  →  Expert / colorist level\n" +
            "ΔE 0.5  →  Near human limits",
    ),
    HelpSection(
        heading = "GEMS",
        body = "+2 gems per correct tap. Bonus gems for milestones:\n\n" +
            "ΔE < 2.0  →  +5 SHARP bonus\n" +
            "ΔE < 1.0  →  +10 EXPERT bonus\n" +
            "ΔE < 0.5  →  +25 ELITE bonus\n\n" +
            "Milestones reset each try — earn them again on every run.",
    ),
)

// ── Daily ─────────────────────────────────────────────────────────────────────

/**
 * Bottom sheet explaining the Daily Challenge rules.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyHelpSheet(onDismiss: () -> Unit) {
    HuezooBottomSheet(onDismissRequest = onDismiss) {
        HelpSheetBody(
            title = "HOW DAILY WORKS",
            accentColor = HuezooColors.GameDaily,
            sections = dailyHelpSections,
        )
    }
}

private val dailyHelpSections = listOf(
    HelpSection(
        heading = "THE GOAL",
        body = "Six swatches appear — one has a slightly different hue. Tap the outlier.",
    ),
    HelpSection(
        heading = "6 ROUNDS, ALWAYS",
        body = "Every day there are exactly 6 rounds with a fixed ΔE curve: " +
            "4.0 → 3.0 → 2.0 → 1.5 → 1.0 → 0.7.\n\n" +
            "A wrong tap doesn't end the game — the correct swatch is revealed and " +
            "you move on. All 6 rounds are always played.",
    ),
    HelpSection(
        heading = "SCORING",
        body = "Only correct rounds score. Wrong rounds score 0.\n\n" +
            "Score per round = 1000 ÷ ΔE\n\n" +
            "e.g. correct at ΔE 1.0 → 1,000 pts\n" +
            "e.g. correct at ΔE 0.7 → ~1,428 pts",
    ),
    HelpSection(
        heading = "SAME PUZZLE WORLDWIDE",
        body = "Today's colours are the same for every player. " +
            "One attempt per day — come back tomorrow for a new challenge.",
    ),
    HelpSection(
        heading = "GEMS",
        body = "+5 gems per correct round.\n" +
            "+3 participation gems for finishing all 6 rounds.\n" +
            "+20 bonus gems for a perfect run (all 6 correct).",
    ),
)

// ── Shared internals ──────────────────────────────────────────────────────────

private data class HelpSection(val heading: String, val body: String)

@Composable
private fun HelpSheetBody(
    title: String,
    accentColor: androidx.compose.ui.graphics.Color,
    sections: List<HelpSection>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HuezooSpacing.lg)
            .navigationBarsPadding()
            .padding(bottom = HuezooSpacing.xl),
    ) {
        // Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .background(accentColor, RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.width(HuezooSpacing.sm))
            HuezooTitleLarge(
                text = title,
                color = accentColor,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        sections.forEach { section ->
            Spacer(Modifier.height(HuezooSpacing.lg))
            HelpSectionBlock(section = section, accentColor = accentColor)
        }
    }
}

@Composable
private fun HelpSectionBlock(
    section: HelpSection,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(HuezooColors.SurfaceL3, RoundedCornerShape(12.dp))
            .padding(HuezooSpacing.md),
        verticalArrangement = Arrangement.spacedBy(HuezooSpacing.xs),
    ) {
        HuezooLabelSmall(
            text = section.heading,
            color = accentColor,
            fontWeight = FontWeight.ExtraBold,
        )
        HuezooBodyMedium(
            text = section.body,
            color = HuezooColors.TextSecondary,
        )
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun ThresholdHelpSheetPreview() {
    HuezooPreviewTheme {
        HelpSheetBody(
            title = "HOW THRESHOLD WORKS",
            accentColor = HuezooColors.GameThreshold,
            sections = thresholdHelpSections,
        )
    }
}

@PreviewComponent
@Composable
private fun DailyHelpSheetPreview() {
    HuezooPreviewTheme {
        HelpSheetBody(
            title = "HOW DAILY WORKS",
            accentColor = HuezooColors.GameDaily,
            sections = dailyHelpSections,
        )
    }
}
