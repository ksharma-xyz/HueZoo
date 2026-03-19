package xyz.ksharma.huezoo.domain.color.di

import org.koin.dsl.module
import xyz.ksharma.huezoo.domain.color.ColorEngine
import xyz.ksharma.huezoo.domain.color.DefaultColorEngine

/**
 * Koin module that provides the [ColorEngine] interface binding.
 *
 * Registered as a singleton — there is no state inside [DefaultColorEngine] that
 * requires a new instance per screen or ViewModel; the Random default is stateless
 * from the caller's perspective.
 *
 * **Testing:** Do NOT use this module in unit tests. Instead, construct
 * [DefaultColorEngine] directly with a seeded [kotlin.random.Random], or pass a
 * [xyz.ksharma.huezoo.domain.color.FakeColorEngine].
 */
val colorModule = module {
    single<ColorEngine> { DefaultColorEngine() }
}
