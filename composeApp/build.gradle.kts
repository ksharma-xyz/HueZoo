import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.huezoo.kotlin.multiplatform)
    alias(libs.plugins.huezoo.compose.multiplatform)
    alias(libs.plugins.huezoo.android.kmp.library)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.detekt)
}

kotlin {
    androidLibrary {
        namespace = "xyz.ksharma.huezoo.composeapp"
        compileSdk = 36
        minSdk = 28

        // MANDATORY for AGP 9 to include assets
        androidResources {
            enable = true
        }

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
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
            implementation(libs.compose.ui.tooling)           // ComposeViewAdapter — required for AS preview rendering
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.sqldelight.android.driver)
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

            // DI
            implementation(libs.di.koinCore)
            implementation(libs.di.koinCompose)
            api(libs.di.koinComposeViewmodel)
            implementation(libs.di.koinComposeViewmodelNav)

            // Navigation 3
            implementation(libs.navigation3.ui)
            implementation(libs.navigation3.viewmodel)

            // Coroutines + Serialization + DateTime
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            // SQLDelight
            implementation(libs.sqldelight.coroutines)
        }

        iosMain.dependencies {
            api(libs.sqldelight.native.driver)
        }

        commonTest.dependencies {
            implementation(libs.test.kotlin)
            implementation(libs.test.coroutines)
        }

        // NativeSqliteDriver is needed in iosTest for the in-memory test database helper.
        // Declared explicitly because test source sets don't automatically inherit
        // main source set dependencies for dependency resolution.
        iosTest.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}

sqldelight {
    databases {
        create("HuezooDatabase") {
            packageName.set("xyz.ksharma.huezoo.data.db")
            verifyMigrations.set(true)
        }
    }
}

detekt {
    autoCorrect = true
    buildUponDefaultConfig = true
    config.setFrom("${rootProject.projectDir}/config/detekt.yml")
    baseline = file("$projectDir/baseline.xml")
    source.setFrom(
        "src/commonMain/kotlin",
        "src/androidMain/kotlin",
        "src/iosMain/kotlin",
    )
}

dependencies {
    detektPlugins(libs.detekt.formatting)
    detektPlugins(libs.detekt.compose)
}
