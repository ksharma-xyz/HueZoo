package xyz.ksharma.huezoo.domain.game

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.LocalDate
import xyz.ksharma.huezoo.domain.game.model.GameRound

/**
 * Generates rounds for the Daily Challenge.
 *
 * All players on the same date see the same base colour and the same odd-swatch positions,
 * making results comparable. Stateless — callers track which round they are on.
 */
interface DailyGameEngine {

    val totalRounds: Int

    /** Fixed ΔE per round — hardest last. Index corresponds to 0-based round index. */
    val deltaECurve: List<Float>

    /**
     * Generates a deterministic round for [date] at [roundIndex] (0-based).
     * The [baseColor] must be produced by [xyz.ksharma.huezoo.domain.color.ColorEngine.seededColorForDate].
     */
    fun generateRound(date: LocalDate, roundIndex: Int, baseColor: Color): GameRound
}
