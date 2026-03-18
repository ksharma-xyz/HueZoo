package xyz.ksharma.funwithcolors.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin for KMP Android libraries (AGP 9+).
 * Uses `com.android.kotlin.multiplatform.library` — NOT `com.android.library`.
 *
 * Module usage:
 * ```
 * plugins { alias(libs.plugins.funwithcolors.android.kmp.library) }
 * kotlin {
 *     android {
 *         namespace = "xyz.ksharma.funwithcolors.module"
 *         compileSdk = 36
 *         minSdk = 28
 *     }
 * }
 * ```
 */
class AndroidKmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.kotlin.multiplatform.library")
        }
    }
}
