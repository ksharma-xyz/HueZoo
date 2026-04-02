package xyz.ksharma.huezoo.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val GEM_PAD_WIDTH = 3
private const val DECIMAL_PRECISION = 10

internal fun formatGems(gems: Int): String = when {
    gems >= 1_000 -> "${gems / 1_000},${(gems % 1_000).toString().padStart(GEM_PAD_WIDTH, '0')}"
    else -> "$gems"
}

internal fun Float.fmtHome(): String {
    val i = toInt()
    val d = ((this - i) * DECIMAL_PRECISION).toInt()
    return "$i.$d"
}

@Suppress("MagicNumber")
internal fun estimatedRankLabel(deltaE: Float): String = when {
    deltaE < 0.5f -> "TOP 1%"
    deltaE < 1.0f -> "TOP 5%"
    deltaE < 1.5f -> "TOP 10%"
    deltaE < 2.0f -> "TOP 20%"
    deltaE < 3.0f -> "TOP 40%"
    deltaE < 4.0f -> "TOP 60%"
    else -> "TOP 80%"
}

@Suppress("MagicNumber")
internal fun estimatedRankDescription(deltaE: Float): String = when {
    deltaE < 0.5f -> "Near human limits"
    deltaE < 1.0f -> "Professional colorist"
    deltaE < 1.5f -> "Trained eye"
    deltaE < 2.0f -> "Designer / photographer"
    deltaE < 3.0f -> "Above average"
    deltaE < 4.0f -> "Average untrained"
    else -> "Just starting out"
}

@Suppress("MagicNumber")
internal fun DrawScope.drawMedalStar(color: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val outer = size.minDimension * 0.48f
    val inner = outer * 0.40f
    val path = Path()
    for (i in 0..9) {
        val angle = (i * 36.0 - 90.0) * PI / 180.0
        val r = if (i % 2 == 0) outer else inner
        val x = cx + (r * cos(angle)).toFloat()
        val y = cy + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = color)
}

@Suppress("MagicNumber")
internal fun DrawScope.drawLeaderboardBars(color: Color) {
    val barW = size.width * 0.24f
    val gap = size.width * 0.08f
    val totalW = barW * 3 + gap * 2
    val startX = (size.width - totalW) / 2f
    listOf(0.45f, 0.70f, 1.0f).forEachIndexed { i, h ->
        val barH = size.height * h
        drawRect(
            color = color,
            topLeft = Offset(startX + i * (barW + gap), size.height - barH),
            size = Size(barW, barH),
        )
    }
}

@OptIn(ExperimentalTime::class)
@Composable
internal fun countdownUntil(until: Instant, prefix: String): String {
    val text by produceState(initialValue = "") {
        while (true) {
            val remaining = until - Clock.System.now()
            val totalSeconds = remaining.inWholeSeconds.coerceAtLeast(0)
            value = if (totalSeconds <= 0) {
                ""
            } else {
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                if (hours > 0) "$prefix${hours}h ${minutes}m" else "$prefix${minutes}m"
            }
            delay(60_000L)
        }
    }
    return text
}
