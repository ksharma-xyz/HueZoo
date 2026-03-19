package xyz.ksharma.huezoo.domain.game.model

import kotlinx.datetime.LocalDate

/** Local record of a played (or in-progress) daily challenge. */
data class DailyChallenge(
    val date: LocalDate,
    val score: Float,
    val completed: Boolean,
)
