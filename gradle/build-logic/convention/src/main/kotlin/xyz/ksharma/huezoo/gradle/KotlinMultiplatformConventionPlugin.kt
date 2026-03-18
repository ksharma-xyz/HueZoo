package xyz.ksharma.huezoo.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

class KotlinMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("org.jetbrains.kotlin.multiplatform")
        }

        extensions.configure<KotlinMultiplatformExtension> {
            applyDefaultHierarchyTemplate()
            iosArm64()
            iosSimulatorArm64()

            // Required for SQLDelight on iOS — links the system SQLite library
            targets.withType<KotlinNativeTarget>().configureEach {
                binaries.configureEach {
                    linkerOpts("-lsqlite3")
                }
            }

            configureJava()
        }
    }
}
