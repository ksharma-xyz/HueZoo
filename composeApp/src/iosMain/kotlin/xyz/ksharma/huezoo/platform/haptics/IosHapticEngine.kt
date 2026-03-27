package xyz.ksharma.huezoo.platform.haptics

import platform.Foundation.NSTimer
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UISelectionFeedbackGenerator

/**
 * iOS implementation of [HapticEngine].
 *
 * Maps each [HapticType] to the most semantically appropriate UIKit feedback generator.
 * All calls must arrive on the main thread — guaranteed by the ViewModel dispatcher.
 *
 * Pattern decisions:
 *   - [HapticType.CorrectTap]   → UINotificationFeedbackGenerator(.success): the universally-
 *                                  recognised "you did it" pattern on iOS hardware.
 *   - [HapticType.WrongTap]     → UINotificationFeedbackGenerator(.error): the distinctive
 *                                  double-bump "nope" — no other pattern communicates failure
 *                                  this clearly.
 *   - [HapticType.MilestoneHit] → UIImpactFeedbackGenerator(.heavy): weighty thud that lands
 *                                  200 ms after CorrectTap — layered, not simultaneous.
 *   - [HapticType.PerfectRun]   → .heavy immediately, then .success after 150 ms via NSTimer:
 *                                  two-beat celebration without blocking the call site.
 *   - [HapticType.GameOver]     → UINotificationFeedbackGenerator(.warning): solemn, distinct
 *                                  from the error pattern.
 *   - [HapticType.GemEarned]    → soft intensity impact (iOS 13+): gentle coin-collect feel.
 */
class IosHapticEngine : HapticEngine {

    override fun perform(type: HapticType) {
        when (type) {
            HapticType.Selection ->
                UISelectionFeedbackGenerator().selectionChanged()

            HapticType.ButtonTap ->
                UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
                    .impactOccurred()

            HapticType.GemEarned ->
                UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleSoft)
                    .impactOccurred()

            HapticType.CorrectTap ->
                UINotificationFeedbackGenerator()
                    .notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)

            HapticType.WrongTap ->
                UINotificationFeedbackGenerator()
                    .notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError)

            HapticType.MilestoneHit ->
                UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
                    .impactOccurred()

            HapticType.PerfectRun -> {
                // Beat 1: heavy thud — immediately
                UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
                    .impactOccurred()
                // Beat 2: success notification — 150 ms later, non-blocking via NSTimer
                NSTimer.scheduledTimerWithTimeInterval(
                    interval = 0.15,
                    repeats = false,
                    block = { _ ->
                        UINotificationFeedbackGenerator()
                            .notificationOccurred(
                                UINotificationFeedbackType.UINotificationFeedbackTypeSuccess,
                            )
                    },
                )
            }

            HapticType.GameOver ->
                UINotificationFeedbackGenerator()
                    .notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeWarning)
        }
    }
}
