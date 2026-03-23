package xyz.ksharma.huezoo.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * Serialization config for multiplatform support (iOS).
 * Required because reflection is not available on non-JVM platforms.
 * Every NavKey subclass must be registered here explicitly.
 * https://kotlinlang.org/docs/multiplatform/compose-navigation-3.html#polymorphic-serialization-for-destination-keys
 */
val huezooNavSerializationConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Splash::class, Splash.serializer())
            subclass(EyeStrainNotice::class, EyeStrainNotice.serializer())
            subclass(Home::class, Home.serializer())
            subclass(ThresholdGame::class, ThresholdGame.serializer())
            subclass(DailyGame::class, DailyGame.serializer())
            subclass(Result::class, Result.serializer())
            subclass(Leaderboard::class, Leaderboard.serializer())
            subclass(Settings::class, Settings.serializer())
            subclass(Upgrade::class, Upgrade.serializer())
        }
    }
}
