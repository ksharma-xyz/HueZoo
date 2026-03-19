package xyz.ksharma.huezoo.domain.color

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.LocalDate

/**
 * Deterministic [ColorEngine] stub for use in ViewModel and integration tests.
 *
 * Every method is overridable via constructor parameters so tests can supply
 * any behaviour they need without subclassing.
 *
 * **Usage example:**
 * ```kotlin
 * val fake = FakeColorEngine(
 *     vividColor = Color.Red,
 *     oddSwatch  = Color.Blue,
 * )
 * val viewModel = ThresholdViewModel(colorEngine = fake, ...)
 * ```
 */
class FakeColorEngine(
    private val vividColor: Color = Color(0xFF00E5FF),
    private val oddSwatch: Color = Color(0xFFFF2D78),
    private val dailyColor: Color = Color(0xFFFFE600),
    private val score: Int = 500,
) : ColorEngine {

    override fun randomVividColor(): Color = vividColor
    override fun generateOddSwatch(base: Color, targetDeltaE: Float): Color = oddSwatch
    override fun seededColorForDate(date: LocalDate): Color = dailyColor
    override fun scoreFromDeltaE(de: Float): Int = score
}
