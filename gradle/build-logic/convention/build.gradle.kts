import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "xyz.ksharma.huezoo.gradle"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.composeCompiler.gradlePlugin)
    compileOnly(libs.detekt.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("composeMultiplatform") {
            id = "huezoo.compose.multiplatform"
            implementationClass = "xyz.ksharma.huezoo.gradle.ComposeMultiplatformConventionPlugin"
        }
        register("androidApplication") {
            id = "huezoo.android.application"
            implementationClass = "xyz.ksharma.huezoo.gradle.AndroidApplicationConventionPlugin"
        }
        register("androidKmpLibrary") {
            id = "huezoo.android.kmp.library"
            implementationClass = "xyz.ksharma.huezoo.gradle.AndroidKmpLibraryConventionPlugin"
        }
        register("kotlinMultiplatform") {
            id = "huezoo.kotlin.multiplatform"
            implementationClass = "xyz.ksharma.huezoo.gradle.KotlinMultiplatformConventionPlugin"
        }
    }
}
