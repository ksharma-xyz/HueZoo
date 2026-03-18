import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "xyz.ksharma.funwithcolors.gradle"

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
}

gradlePlugin {
    plugins {
        register("composeMultiplatform") {
            id = "funwithcolors.compose.multiplatform"
            implementationClass = "xyz.ksharma.funwithcolors.gradle.ComposeMultiplatformConventionPlugin"
        }
        register("androidApplication") {
            id = "funwithcolors.android.application"
            implementationClass = "xyz.ksharma.funwithcolors.gradle.AndroidApplicationConventionPlugin"
        }
        register("androidKmpLibrary") {
            id = "funwithcolors.android.kmp.library"
            implementationClass = "xyz.ksharma.funwithcolors.gradle.AndroidKmpLibraryConventionPlugin"
        }
        register("kotlinMultiplatform") {
            id = "funwithcolors.kotlin.multiplatform"
            implementationClass = "xyz.ksharma.funwithcolors.gradle.KotlinMultiplatformConventionPlugin"
        }
    }
}
