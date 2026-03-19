package xyz.ksharma.huezoo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import huezoo.composeapp.generated.resources.Res
import huezoo.composeapp.generated.resources.bebas_neue_regular
import huezoo.composeapp.generated.resources.clash_display_bold
import huezoo.composeapp.generated.resources.clash_display_medium
import huezoo.composeapp.generated.resources.clash_display_regular
import huezoo.composeapp.generated.resources.clash_display_semibold
import huezoo.composeapp.generated.resources.space_grotesk_bold
import huezoo.composeapp.generated.resources.space_grotesk_medium
import huezoo.composeapp.generated.resources.space_grotesk_regular
import huezoo.composeapp.generated.resources.space_grotesk_semibold
import org.jetbrains.compose.resources.Font

/**
 * Hue Zoo uses three complementary typefaces:
 *
 * - **Bebas Neue** — ultra-bold, tall, condensed display font for hero numbers and the app name.
 *   Used for: ΔE scores, big statistics, game headings. Single weight — inherently bold.
 *   License: SIL Open Font License 1.1 (free for all uses including commercial bundling).
 *
 * - **Clash Display** — geometric, premium display font. Distinct alternates at large sizes.
 *   Used for: card titles, section headings, medium-level hierarchy.
 *   License: Fontshare FF EULA — free for commercial use in apps.
 *   ⚠️  See MVP.md for license review TODO before App Store submission.
 *
 * - **Space Grotesk** — geometric sans-serif for all body and UI text.
 *   Used for: body copy, button labels, badges, secondary labels.
 *   License: SIL Open Font License 1.1 (free for all uses including commercial bundling).
 */
@Composable
fun huezooTypography(): Typography {
    // Bebas Neue has only one weight — it is inherently heavy/condensed.
    // Register it under all weights so Compose picks it up regardless of what weight is requested.
    val bebasNeue = FontFamily(
        Font(Res.font.bebas_neue_regular, FontWeight.Normal),
        Font(Res.font.bebas_neue_regular, FontWeight.Medium),
        Font(Res.font.bebas_neue_regular, FontWeight.SemiBold),
        Font(Res.font.bebas_neue_regular, FontWeight.Bold),
        Font(Res.font.bebas_neue_regular, FontWeight.ExtraBold),
    )

    val clashDisplay = FontFamily(
        Font(Res.font.clash_display_regular, FontWeight.Normal),
        Font(Res.font.clash_display_medium, FontWeight.Medium),
        Font(Res.font.clash_display_semibold, FontWeight.SemiBold),
        Font(Res.font.clash_display_bold, FontWeight.Bold),
        Font(Res.font.clash_display_bold, FontWeight.ExtraBold),
    )

    val spaceGrotesk = FontFamily(
        Font(Res.font.space_grotesk_regular, FontWeight.Normal),
        Font(Res.font.space_grotesk_medium, FontWeight.Medium),
        Font(Res.font.space_grotesk_semibold, FontWeight.SemiBold),
        Font(Res.font.space_grotesk_bold, FontWeight.Bold),
        Font(Res.font.space_grotesk_bold, FontWeight.ExtraBold),
    )

    return Typography(
        // ── Hero display — Bebas Neue ─────────────────────────────────────────
        // ΔE scores, big count-up numbers on ResultCard
        displayLarge = TextStyle(
            fontFamily = bebasNeue,
            fontWeight = FontWeight.Normal,
            fontSize = 56.sp,
            lineHeight = 60.sp,
        ),
        // Medium display numbers — SCORE / ROUNDS stats on ResultCard
        displayMedium = TextStyle(
            fontFamily = bebasNeue,
            fontWeight = FontWeight.Normal,
            fontSize = 40.sp,
            lineHeight = 44.sp,
        ),
        // Small display numbers — DeltaEBadge, compact numeric readouts
        displaySmall = TextStyle(
            fontFamily = bebasNeue,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            lineHeight = 32.sp,
        ),

        // ── Section headers — Bebas Neue / Clash Display ──────────────────────
        // App name "Hue Zoo", screen titles
        headlineLarge = TextStyle(
            fontFamily = bebasNeue,
            fontWeight = FontWeight.Normal,
            fontSize = 40.sp,
            lineHeight = 44.sp,
        ),
        // Sub-screen headings, dialog titles
        headlineMedium = TextStyle(
            fontFamily = clashDisplay,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 32.sp,
        ),
        // Compact Bebas — currency pill, inline numeric labels
        headlineSmall = TextStyle(
            fontFamily = bebasNeue,
            fontWeight = FontWeight.Normal,
            fontSize = 26.sp,
            lineHeight = 30.sp,
        ),

        // ── Card titles — Clash Display ───────────────────────────────────────
        // GameCard titles, paywall section headings
        titleLarge = TextStyle(
            fontFamily = clashDisplay,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 26.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = clashDisplay,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 24.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = clashDisplay,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 20.sp,
        ),

        // ── Body — Space Grotesk ──────────────────────────────────────────────
        // Subtitles, descriptive text
        bodyLarge = TextStyle(
            fontFamily = spaceGrotesk,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            lineHeight = 26.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = spaceGrotesk,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = spaceGrotesk,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),

        // ── Labels — Space Grotesk ────────────────────────────────────────────
        // Button labels (ExtraBold → set at call site)
        labelLarge = TextStyle(
            fontFamily = spaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 20.sp,
        ),
        // Badges, game labels, secondary actions
        labelMedium = TextStyle(
            fontFamily = spaceGrotesk,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 16.sp,
        ),
        // Captions, disabled text, small metadata
        labelSmall = TextStyle(
            fontFamily = spaceGrotesk,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 14.sp,
        ),
    )
}
