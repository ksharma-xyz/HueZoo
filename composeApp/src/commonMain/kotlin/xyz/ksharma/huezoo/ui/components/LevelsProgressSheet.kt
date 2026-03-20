package xyz.ksharma.huezoo.ui.components

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.ksharma.huezoo.ui.model.PlayerLevel
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewComponent
import xyz.ksharma.huezoo.ui.theme.HuezooColors
import xyz.ksharma.huezoo.ui.theme.HuezooSpacing
import xyz.ksharma.huezoo.ui.theme.LocalPlayerAccentColor
import xyz.ksharma.huezoo.ui.theme.shapedShadow

private val LevelShelfOffset = 4.dp
private const val LOCKED_ALPHA = 0.38f

/**
 * Bottom sheet showing the full 5-tier player progression system.
 *
 * Layout:
 * - Header: "LEVELS & PROGRESS" + tricolor gradient divider
 * - 5 [PlayerLevel] tier cards, color-coded
 *   - Current level: full opacity, progress bar toward next tier
 *   - Future tiers: dimmed to [LOCKED_ALPHA]
 * - Close button
 *
 * Trigger from HomeScreen: tap the gem inventory area (stats section).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelsProgressSheet(
    currentGems: Int,
    onDismiss: () -> Unit,
) {
    val currentLevel = PlayerLevel.fromGems(currentGems)

    HuezooBottomSheet(onDismissRequest = onDismiss) {
        // ── Sticky header — never scrolls away ────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HuezooSpacing.lg)
                .padding(bottom = HuezooSpacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(24.dp)
                        .background(LocalPlayerAccentColor.current, RoundedCornerShape(2.dp)),
                )
                Spacer(Modifier.width(HuezooSpacing.md))
                HuezooTitleLarge(
                    text = "LEVELS & PROGRESS",
                    color = HuezooColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            Spacer(Modifier.height(HuezooSpacing.xs))

            // Tricolor gradient divider: Cyan → Magenta → Gold
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .graphicsLayer { alpha = 0.5f }
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                HuezooColors.AccentCyan,
                                HuezooColors.AccentMagenta,
                                HuezooColors.AccentYellow,
                            ),
                        ),
                    ),
            )
        }

        // ── Scrollable cards + close button ───────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = HuezooSpacing.lg)
                .navigationBarsPadding()
                .padding(top = HuezooSpacing.md)
                .padding(bottom = HuezooSpacing.xl),
        ) {
            PlayerLevel.entries.forEachIndexed { index, level ->
                val isCurrentLevel = level == currentLevel
                val isLocked = level.ordinal > currentLevel.ordinal

                LevelCard(
                    level = level,
                    levelNumber = index + 1,
                    isCurrentLevel = isCurrentLevel,
                    isLocked = isLocked,
                    currentGems = currentGems,
                )

                if (index < PlayerLevel.entries.lastIndex) {
                    Spacer(Modifier.height(HuezooSpacing.md))
                }
            }

            Spacer(Modifier.height(HuezooSpacing.xl))

            HuezooButton(
                text = "CLOSE",
                onClick = onDismiss,
                variant = HuezooButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ── Level card ─────────────────────────────────────────────────────────────────

@Composable
private fun LevelCard(
    level: PlayerLevel,
    levelNumber: Int,
    isCurrentLevel: Boolean,
    isLocked: Boolean,
    currentGems: Int,
    modifier: Modifier = Modifier,
) {
    val levelColor = level.levelColor
    val shelfColor = levelColor.copy(alpha = 0.45f)
    val gemRange = gemRangeLabel(level)
    val perks = levelPerks(level)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (isLocked) LOCKED_ALPHA else 1f },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shapedShadow(RectangleShape, shelfColor, LevelShelfOffset, LevelShelfOffset)
                .background(HuezooColors.SurfaceL3)
                .drawBehind {
                    // Neon top-left rim (neo-brutalist inset highlight)
                    val stroke = 1.5.dp.toPx()
                    drawLine(
                        levelColor.copy(alpha = 0.7f),
                        Offset(0f, stroke / 2f),
                        Offset(size.width, stroke / 2f),
                        stroke,
                    )
                    drawLine(
                        levelColor.copy(alpha = 0.7f),
                        Offset(stroke / 2f, 0f),
                        Offset(stroke / 2f, size.height),
                        stroke,
                    )
                    // Subtle accent bar along the entire top
                    drawRect(
                        color = levelColor.copy(alpha = 0.35f),
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width, 2.dp.toPx()),
                    )
                }
                .padding(HuezooSpacing.md),
            verticalArrangement = Arrangement.spacedBy(HuezooSpacing.sm),
        ) {
            // ── Card header ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    HuezooLabelSmall(
                        text = "LEVEL ${levelNumber.toString().padStart(2, '0')}",
                        color = levelColor,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    HuezooHeadlineMedium(
                        text = level.displayName,
                        color = HuezooColors.TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }

                // Level badge box
                Box(
                    modifier = Modifier
                        .background(levelColor.copy(alpha = 0.18f))
                        .padding(horizontal = HuezooSpacing.sm, vertical = HuezooSpacing.xs),
                    contentAlignment = Alignment.Center,
                ) {
                    HuezooLabelSmall(
                        text = if (isCurrentLevel) "CURRENT" else if (isLocked) "LOCKED" else "CLEARED",
                        color = levelColor,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }

            // ── Gem threshold row ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HuezooColors.SurfaceL0)
                    .padding(horizontal = HuezooSpacing.sm, vertical = HuezooSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(HuezooSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HuezooLabelSmall(
                    text = "THRESHOLD",
                    color = HuezooColors.TextDisabled,
                    fontWeight = FontWeight.SemiBold,
                )
                HuezooTitleSmall(
                    text = gemRange,
                    color = levelColor,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            // ── What Changes section ──────────────────────────────────────────
            HuezooLabelSmall(
                text = "WHAT CHANGES",
                color = HuezooColors.TextDisabled,
                fontWeight = FontWeight.SemiBold,
            )

            perks.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HuezooColors.SurfaceL0)
                        .padding(horizontal = HuezooSpacing.sm, vertical = HuezooSpacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HuezooBodyMedium(
                        text = label,
                        color = HuezooColors.TextSecondary,
                    )
                    HuezooBodyMedium(
                        text = value,
                        color = levelColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // ── Progress bar (current level only) ────────────────────────────
            if (isCurrentLevel) {
                Spacer(Modifier.height(HuezooSpacing.xs))
                LevelProgressInCard(
                    level = level,
                    currentGems = currentGems,
                    levelColor = levelColor,
                )
            }
        }
    }
}

@Composable
private fun LevelProgressInCard(
    level: PlayerLevel,
    currentGems: Int,
    levelColor: Color,
    modifier: Modifier = Modifier,
) {
    val nextLevel = PlayerLevel.entries.getOrNull(level.ordinal + 1)
    val fraction = if (nextLevel != null) {
        val range = (nextLevel.minGems - level.minGems).toFloat()
        val progress = (currentGems - level.minGems).toFloat()
        (progress / range).coerceIn(0f, 1f)
    } else {
        1f
    }
    Column(modifier = modifier.fillMaxWidth()) {
        // Progress bar track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(HuezooColors.SurfaceL4),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .background(levelColor),
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HuezooLabelSmall(
                text = "$currentGems GEMS",
                color = levelColor,
            )
            if (nextLevel != null) {
                val gemsToNext = nextLevel.minGems - currentGems
                HuezooLabelSmall(
                    text = "$gemsToNext to ${nextLevel.displayName}",
                    color = HuezooColors.TextDisabled,
                )
            } else {
                HuezooLabelSmall(
                    text = "MAX LEVEL",
                    color = levelColor,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

// ── Data helpers ───────────────────────────────────────────────────────────────

private fun gemRangeLabel(level: PlayerLevel): String {
    val next = PlayerLevel.entries.getOrNull(level.ordinal + 1)
    return if (next != null) {
        "${formatGems(level.minGems)} – ${formatGems(next.minGems - 1)} gems"
    } else {
        "${formatGems(level.minGems)}+ gems"
    }
}

private fun formatGems(n: Int): String = when {
    n >= 1_000 -> "${n / 1_000},${(n % 1_000).toString().padStart(3, '0')}"
    else -> "$n"
}

/**
 * Placeholder "what changes" per level — values TBD once monetization is finalized.
 * See GAME_DESIGN.md Open Design Questions.
 */
private fun levelPerks(level: PlayerLevel): List<Pair<String, String>> = when (level) {
    PlayerLevel.Rookie -> listOf(
        "Difficulty" to "Standard",
        "Rewards" to "1.0× gems",
        "Badge" to "Default",
    )
    PlayerLevel.Trained -> listOf(
        "Difficulty" to "Wider gamut",
        "Rewards" to "1.0× gems",
        "Badge" to "Glow frame",
    )
    PlayerLevel.Sharp -> listOf(
        "Difficulty" to "Subtle shifts",
        "Rewards" to "1.2× gems",
        "Badge" to "Neon aura",
    )
    PlayerLevel.Elite -> listOf(
        "Difficulty" to "Near-invisible",
        "Rewards" to "1.5× gems",
        "Badge" to "Elite insignia",
    )
    PlayerLevel.Master -> listOf(
        "Difficulty" to "Chromatic limits",
        "Rewards" to "2.0× gems",
        "Badge" to "Master crown",
    )
}

// ── Previews ───────────────────────────────────────────────────────────────────

@PreviewComponent
@Composable
private fun LevelsProgressSheetRookiePreview() {
    HuezooPreviewTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(HuezooColors.SurfaceL2)
                .padding(HuezooSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(HuezooSpacing.md),
        ) {
            PlayerLevel.entries.forEachIndexed { index, level ->
                LevelCard(
                    level = level,
                    levelNumber = index + 1,
                    isCurrentLevel = level == PlayerLevel.Rookie,
                    isLocked = level.ordinal > PlayerLevel.Rookie.ordinal,
                    currentGems = 87,
                )
            }
        }
    }
}

@PreviewComponent
@Composable
private fun LevelsProgressSheetSharpPreview() {
    HuezooPreviewTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(HuezooColors.SurfaceL2)
                .padding(HuezooSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(HuezooSpacing.md),
        ) {
            PlayerLevel.entries.forEachIndexed { index, level ->
                LevelCard(
                    level = level,
                    levelNumber = index + 1,
                    isCurrentLevel = level == PlayerLevel.Sharp,
                    isLocked = level.ordinal > PlayerLevel.Sharp.ordinal,
                    currentGems = 2100,
                )
            }
        }
    }
}
