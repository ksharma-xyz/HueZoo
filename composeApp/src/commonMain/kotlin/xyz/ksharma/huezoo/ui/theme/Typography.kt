package xyz.ksharma.huezoo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import huezoo.composeapp.generated.resources.Res
import huezoo.composeapp.generated.resources.antonio_bold
import huezoo.composeapp.generated.resources.antonio_medium
import huezoo.composeapp.generated.resources.antonio_regular
import huezoo.composeapp.generated.resources.fredoka_bold
import huezoo.composeapp.generated.resources.fredoka_regular
import huezoo.composeapp.generated.resources.fredoka_semibold
import huezoo.composeapp.generated.resources.space_grotesk_bold
import huezoo.composeapp.generated.resources.space_grotesk_medium
import huezoo.composeapp.generated.resources.space_grotesk_regular
import huezoo.composeapp.generated.resources.space_grotesk_semibold
import org.jetbrains.compose.resources.Font

/**
 * Hue Zoo uses three complementary typefaces:
 *
 * - **Antonio** — bold, condensed display font for hero numbers and the app name.
 *   Used for: ΔE scores, big statistics, game headings.
 *
 * - **Fredoka** — rounded, friendly font that gives the game a warm, playful feel.
 *   Used for: card titles, section headings, medium-level hierarchy.
 *
 * - **Space Grotesk** — geometric sans-serif for all body and UI text.
 *   Used for: body copy, button labels, badges, secondary labels.
 */
@Composable
fun huezooTypography(): Typography {
    val antonio = FontFamily(
        Font(Res.font.antonio_regular, FontWeight.Normal),
        Font(Res.font.antonio_medium, FontWeight.Medium),
        Font(Res.font.antonio_bold, FontWeight.Bold),
    )

    val fredoka = FontFamily(
        Font(Res.font.fredoka_regular, FontWeight.Normal),
        Font(Res.font.fredoka_semibold, FontWeight.SemiBold),
        Font(Res.font.fredoka_bold, FontWeight.Bold),
    )

    val spaceGrotesk = FontFamily(
        Font(Res.font.space_grotesk_regular, FontWeight.Normal),
        Font(Res.font.space_grotesk_medium, FontWeight.Medium),
        Font(Res.font.space_grotesk_semibold, FontWeight.SemiBold),
        Font(Res.font.space_grotesk_bold, FontWeight.Bold),
    )

    return Typography(
        // ── Hero display — Antonio ─────────────────────────────────────────────
        // ΔE scores, big count-up numbers on ResultCard
        displayLarge = TextStyle(
            fontFamily = antonio,
            fontWeight = FontWeight.Bold,
            fontSize = 56.sp,
            lineHeight = 60.sp,
        ),
        // Medium display numbers
        displayMedium = TextStyle(
            fontFamily = antonio,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            lineHeight = 44.sp,
        ),

        // ── Section headers — Antonio ──────────────────────────────────────────
        // App name "Hue Zoo", screen titles
        headlineLarge = TextStyle(
            fontFamily = antonio,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            lineHeight = 44.sp,
        ),
        // Sub-screen headings, large stats (SCORE, ROUNDS on ResultCard)
        headlineMedium = TextStyle(
            fontFamily = fredoka,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 32.sp,
        ),

        // ── Card titles — Fredoka ─────────────────────────────────────────────
        // GameCard titles, paywall section headings
        titleLarge = TextStyle(
            fontFamily = fredoka,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 26.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = fredoka,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 24.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = fredoka,
            fontWeight = FontWeight.Normal,
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
