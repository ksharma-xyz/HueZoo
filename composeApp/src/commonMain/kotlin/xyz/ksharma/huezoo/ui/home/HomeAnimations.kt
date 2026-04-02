package xyz.ksharma.huezoo.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.ksharma.huezoo.ui.components.HuezooHeadlineSmall

internal const val STAGGER_DELAY_MS = 120L
internal const val CARD_ANIM_DURATION_MS = 220
internal const val GREETING_TYPEWRITER_MS = 28L
internal const val INITIAL_DELAY_MS = 175L // breathing room after splash→home fade (halved from 350)
internal const val GREETING_START_DELAY_MS = 1100L
internal const val CALLSIGN_LETTER_STAGGER_MS = 65L
internal const val CALLSIGN_DELAY_MS = 1300L
internal const val SESSION_ANIM_DONE_MS = 1900L

/**
 * In-memory session flag. Set to `true` once the cold-open entrance animation finishes.
 * On back-navigation the composable remounts and reads `hasPlayed = true` → all
 * entrance animations are skipped immediately, keeping re-entry instant and clean.
 * Resets when the process is killed = a new app session always gets the full show.
 */
internal object HomeScreenAnimationState {
    var hasPlayed: Boolean = false
}

/**
 * Cold-open entrance: cards slide up from below with a spring bounce.
 * [INITIAL_DELAY_MS] breathing room before first card, then [STAGGER_DELAY_MS] per index
 * so each card's arrival is clearly visible before the next one begins.
 *
 * Re-entry: [HomeScreenAnimationState.hasPlayed] = true → instant display, no replay.
 */
@Composable
internal fun StaggeredCard(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val alreadyPlayed = remember { HomeScreenAnimationState.hasPlayed }
    val alpha = remember { Animatable(if (alreadyPlayed) 1f else 0f) }
    val translateY = remember { Animatable(if (alreadyPlayed) 0f else 36f) } // dp, slides up
    val scale = remember { Animatable(if (alreadyPlayed) 1f else 0.88f) }

    LaunchedEffect(Unit) {
        if (!alreadyPlayed) {
            delay(INITIAL_DELAY_MS + index * STAGGER_DELAY_MS)
            launch { alpha.animateTo(1f, tween(CARD_ANIM_DURATION_MS)) }
            launch {
                translateY.animateTo(
                    0f,
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
            }
            launch {
                scale.animateTo(
                    1f,
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
            }
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = translateY.value * density
            scaleX = scale.value
            scaleY = scale.value
        },
    ) {
        content()
    }
}

/**
 * Simple per-character fade-in reveal — each letter fades in staggered left-to-right.
 *
 * - [animate] = true  → letters fade in after [startDelay] ms (cold open only)
 * - [animate] = false → all characters immediately visible (re-entry, zero cost)
 */
@Composable
internal fun AnimatedCallsign(
    text: String,
    color: Color,
    animate: Boolean,
    startDelay: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alphas = remember(text) { List(text.length) { Animatable(if (animate) 0f else 1f) } }

    LaunchedEffect(text) {
        if (animate) {
            text.indices.forEach { i ->
                launch {
                    delay(startDelay + i * CALLSIGN_LETTER_STAGGER_MS)
                    alphas[i].animateTo(1f, tween(160))
                }
            }
        }
    }

    Row(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    ) {
        text.forEachIndexed { i, char ->
            HuezooHeadlineSmall(
                text = char.toString(),
                color = color,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.graphicsLayer {
                    alpha = alphas[i].value
                },
            )
        }
    }
}
