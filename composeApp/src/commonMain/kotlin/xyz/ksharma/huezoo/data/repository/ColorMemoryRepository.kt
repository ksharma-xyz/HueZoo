package xyz.ksharma.huezoo.data.repository

import xyz.ksharma.huezoo.domain.game.model.PersonalBest

interface ColorMemoryRepository {

    /**
     * Returns the stored best for Color Memory Match, or null if never completed.
     * [PersonalBest.bestRounds] carries the best session **score** (−50..100);
     * [PersonalBest.bestDeltaE] carries the tightest ΔE answered correctly.
     */
    suspend fun getPersonalBest(): PersonalBest?

    /** Saves personal best if [score] exceeds the stored best score. */
    suspend fun savePersonalBest(score: Int, tightestDeltaE: Float)
}
