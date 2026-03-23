package xyz.ksharma.huezoo.ui.eyestrain

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.huezoo.ui.components.AmbientGlowBackground
import xyz.ksharma.huezoo.ui.components.HuezooButton
import xyz.ksharma.huezoo.ui.components.HuezooButtonVariant
import xyz.ksharma.huezoo.ui.preview.HuezooPreviewTheme
import xyz.ksharma.huezoo.ui.preview.PreviewScreen
import xyz.ksharma.huezoo.ui.theme.HuezooColors

/**
 * Eye-strain / health notice screen — UX.5.3.
 *
 * Shown exactly once on first launch (before Home) gated by
 * [SettingsRepository.hasSeenHealthNotice]. After the player taps "GOT IT",
 * the flag is persisted and this screen is never shown again automatically.
 *
 * The same content is also accessible at any time from About / Settings (UX.15).
 *
 * Copy follows App Store + Play Store health content guidelines:
 *   - factual, not alarmist
 *   - no fear-based language
 *   - player agency ("play at your own pace", "take breaks")
 */
@Composable
fun EyeStrainNoticeScreen(
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EyeStrainViewModel = koinViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.navEvent.collect { onNavigateToHome() }
    }

    AmbientGlowBackground(
        modifier = modifier,
        primaryColor = HuezooColors.AccentCyan,
        secondaryColor = HuezooColors.AccentPurple,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            // ── Icon ──────────────────────────────────────────────────────────
            EyeIcon(
                tint = HuezooColors.AccentCyan,
                modifier = Modifier.size(72.dp),
            )

            Spacer(Modifier.height(32.dp))

            // ── Title ─────────────────────────────────────────────────────────
            Text(
                text = "QUICK NOTE",
                style = MaterialTheme.typography.headlineMedium,
                color = HuezooColors.AccentCyan,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = 4.sp,
            )

            Spacer(Modifier.height(24.dp))

            // ── Notice copy ───────────────────────────────────────────────────
            // Broken into two tonal blocks so each idea has room to land.
            NoticeBlock(
                headline = "It's a game for your eyes.",
                body = "Each round tests how clearly you can see and differentiate colors. " +
                    "The goal: spot the one swatch that looks even slightly different. " +
                    "The better your eyes, the smaller the difference you'll catch.",
            )

            Spacer(Modifier.height(20.dp))

            NoticeBlock(
                headline = "Your color vision can improve.",
                body = "Like any skill, the more you practice, the sharper you get. " +
                    "Players who train regularly reach ΔE scores below 1.0 — " +
                    "a level most people can't even perceive.",
            )

            Spacer(Modifier.height(20.dp))

            NoticeBlock(
                headline = "Take breaks when you need to.",
                body = "Extended sessions can cause eye fatigue. " +
                    "Stop any time you feel discomfort, rest, and come back sharper. " +
                    "There's no penalty for pausing.",
            )

            Spacer(Modifier.weight(1f))

            // ── Dismiss ───────────────────────────────────────────────────────
            HuezooButton(
                text = "GOT IT — LET'S PLAY",
                onClick = { viewModel.onGotIt() },
                variant = HuezooButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "This notice is always available in Settings.",
                style = MaterialTheme.typography.labelSmall,
                color = HuezooColors.TextSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun NoticeBlock(
    headline: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = headline,
            style = MaterialTheme.typography.titleSmall,
            color = HuezooColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = HuezooColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Simple eye icon drawn with Canvas — an almond-shaped outline with a filled iris circle
 * and a small highlight dot.  No image assets or Material Icons required.
 */
@Composable
private fun EyeIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val rx = w * 0.48f // horizontal radius of the almond
        val ry = h * 0.28f // vertical radius
        val stroke = w * 0.055f

        // Outer almond (eye outline) — two cubic arcs
        val eyePath = Path().apply {
            moveTo(cx - rx, cy)
            cubicTo(cx - rx * 0.5f, cy - ry * 2f, cx + rx * 0.5f, cy - ry * 2f, cx + rx, cy)
            cubicTo(cx + rx * 0.5f, cy + ry * 2f, cx - rx * 0.5f, cy + ry * 2f, cx - rx, cy)
            close()
        }
        drawPath(eyePath, color = tint.copy(alpha = 0.25f), style = androidx.compose.ui.graphics.drawscope.Fill)
        drawPath(eyePath, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))

        // Iris ring
        val irisR = h * 0.22f
        drawCircle(color = tint.copy(alpha = 0.20f), radius = irisR, center = Offset(cx, cy))
        drawCircle(
            color = tint,
            radius = irisR,
            center = Offset(cx, cy),
            style = Stroke(width = stroke * 0.85f),
        )

        // Pupil fill
        drawCircle(color = tint, radius = irisR * 0.45f, center = Offset(cx, cy))

        // Specular highlight dot
        drawCircle(
            color = Color.White.copy(alpha = 0.7f),
            radius = irisR * 0.16f,
            center = Offset(cx + irisR * 0.35f, cy - irisR * 0.35f),
        )

        // Lash tick marks at the top curve
        val lashCount = 5
        for (i in 0 until lashCount) {
            val t = (i + 1f) / (lashCount + 1f)
            val lx = (cx - rx) + 2 * rx * t
            // point on top arc
            val topY = cy - ry * 2f * 4f * t * (1f - t)
            drawLine(
                color = tint.copy(alpha = 0.55f),
                start = Offset(lx, topY),
                end = Offset(lx, topY - h * 0.07f),
                strokeWidth = stroke * 0.6f,
                cap = StrokeCap.Round,
            )
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@PreviewScreen
@Composable
private fun EyeStrainNoticePreview() {
    HuezooPreviewTheme {
        EyeStrainNoticeScreen(onNavigateToHome = {})
    }
}
