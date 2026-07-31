package xyz.ksharma.huezoo.data.repository

import xyz.ksharma.huezoo.domain.game.model.PersonalBest

interface ColorMemoryRepository {

    /**
     * Returns the stored best for Color Memory Match, or null if never completed.
     * [PersonalBest.bestRounds] carries the best session **score** (−50..100);
     * [PersonalBest.bestDeltaE] carries the tightest ΔE answered correctly.
     */
    suspend fun getPersonalBest(): PersonalBest?

    /**
     * Saves personal best if [score] exceeds the stored best score.
     *
     * @return true if this session set a new best — callers use it to suppress the
     *   replay interstitial so a personal best isn't followed by an ad.
     */
    suspend fun savePersonalBest(score: Int, tightestDeltaE: Float): Boolean
}
