import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.funwithcolors.kotlin.multiplatform)
    alias(libs.plugins.funwithcolors.compose.multiplatform)
    alias(libs.plugins.funwithcolors.android.kmp.library)
}

kotlin {
    android {
        namespace = "xyz.ksharma.huezoo.composeapp"
        compileSdk = 36
        minSdk = 28

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.kotlinx.coroutines.android)
        }

        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.di.koinCore)
            implementation(libs.di.koinCompose)
            implementation(libs.di.koinComposeViewmodel)
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(libs.test.kotlin)
        }
    }
}
