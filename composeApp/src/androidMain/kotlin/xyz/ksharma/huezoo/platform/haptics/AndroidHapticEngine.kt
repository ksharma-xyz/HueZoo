package xyz.ksharma.huezoo.platform.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Android implementation of [HapticEngine].
 *
 * Uses [VibrationEffect] predefined constants on API 29+ for the tightest,
 * most hardware-tuned patterns available. Falls back to explicit waveforms on
 * API 26–28. Silently no-ops on API < 26 (pre-Oreo, no VibrationEffect API).
 *
 * Pattern decisions:
 *   - [HapticType.CorrectTap] → HEAVY_CLICK: the sharpest satisfying "yes" in the predefined set.
 *   - [HapticType.WrongTap]   → DOUBLE_CLICK on 29+; two-pulse waveform on 26–28: unmistakable "no".
 *   - [HapticType.MilestoneHit] → 80 ms max-amplitude burst: emphasis without being annoying.
 *   - [HapticType.PerfectRun] → two-pulse escalating waveform: celebration without spamming.
 *   - [HapticType.GameOver]   → three-pulse fading waveform: finality.
 */
class AndroidHapticEngine(context: Context) : HapticEngine {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    override fun perform(type: HapticType) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(effectFor(type))
    }

    @Suppress("NewApi") // guarded by perform()'s SDK_INT >= O check
    private fun effectFor(type: HapticType): VibrationEffect = when (type) {

        HapticType.Selection -> predefined(VibrationEffect.EFFECT_TICK)
            ?: VibrationEffect.createOneShot(20, 40)

        HapticType.ButtonTap -> predefined(VibrationEffect.EFFECT_CLICK)
            ?: VibrationEffect.createOneShot(30, 80)

        HapticType.GemEarned -> predefined(VibrationEffect.EFFECT_TICK)
            ?: VibrationEffect.createOneShot(25, 60)

        HapticType.CorrectTap -> predefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            ?: VibrationEffect.createOneShot(50, 180)

        HapticType.WrongTap -> predefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
            ?: VibrationEffect.createWaveform(
                longArrayOf(0, 50, 100, 50),
                intArrayOf(0, 200, 0, 180),
                /* repeat= */ -1,
            )

        HapticType.MilestoneHit ->
            VibrationEffect.createOneShot(80, 255)

        HapticType.PerfectRun ->
            VibrationEffect.createWaveform(
                longArrayOf(0, 60, 150, 100),
                intArrayOf(0, 255, 0, 200),
                /* repeat= */ -1,
            )

        HapticType.GameOver ->
            VibrationEffect.createWaveform(
                longArrayOf(0, 100, 80, 60),
                intArrayOf(0, 255, 0, 150),
                /* repeat= */ -1,
            )
    }

    /**
     * Returns a predefined [VibrationEffect] on API 29+, null on older APIs.
     * Callers supply a fallback for the older path.
     */
    @Suppress("NewApi")
    private fun predefined(effectId: Int): VibrationEffect? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(effectId)
        } else {
            null
        }
}
