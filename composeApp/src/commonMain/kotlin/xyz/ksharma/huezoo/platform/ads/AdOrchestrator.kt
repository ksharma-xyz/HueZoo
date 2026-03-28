package xyz.ksharma.huezoo.platform.ads

import kotlinx.datetime.LocalDate

/**
 * In-memory singleton that gates interstitial ad display for Threshold sessions.
 *
 * Rules (from MONETIZATION_DESIGN.md):
 * - Show at most once every 2 Threshold sessions.
 * - Never after a new all-time personal best.
 * - Never more than [MAX_PER_DAY] times per calendar day.
 *
 * All state is in-memory and resets with the process (daily cap resets on date change).
 * Call [shouldShowInterstitial] *after* the session result is known, then [onInterstitialShown]
 * after the ad is actually presented.
 */
class AdOrchestrator {

    private var thresholdSessionCount = 0
    private var todayKey = ""
    private var shownTodayCount = 0

    /**
     * Returns true if an interstitial should be shown for this Threshold session end.
     * Increments the session counter regardless of the return value.
     *
     * @param isNewPersonalBest  true if the player set a new all-time best this session.
     * @param today              today's date — used to enforce the daily cap.
     */
    fun shouldShowInterstitial(isNewPersonalBest: Boolean, today: LocalDate): Boolean {
        val dateStr = today.toString()
        if (dateStr != todayKey) {
            todayKey = dateStr
            shownTodayCount = 0
        }
        thresholdSessionCount++
        return thresholdSessionCount % 2 == 0 &&
            !isNewPersonalBest &&
            shownTodayCount < MAX_PER_DAY
    }

    /** Call after an interstitial was successfully presented to track the daily cap. */
    fun onInterstitialShown() {
        shownTodayCount++
    }

    private companion object {
        const val MAX_PER_DAY = 3
    }
}
