package xyz.ksharma.huezoo.domain.game.model

/** Best recorded result for a given game, loaded from local DB. */
data class PersonalBest(
    val gameId: String,
    val bestDeltaE: Float?,
    val bestScore: Int?,
)
