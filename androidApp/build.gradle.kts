plugins {
    alias(libs.plugins.huezoo.android.application)
    alias(libs.plugins.composeCompiler)
}
android {
    namespace = "xyz.ksharma.huezoo"

    defaultConfig {
        applicationId = "xyz.ksharma.huezoo"
        minSdk = 28
        versionCode = findProperty("versionCode")?.toString()?.toInt() ?: 1
        versionName = "0.1.0"
    }

    signingConfigs {
        create("release") {
            // CI: env vars injected from GitHub Secrets.
            // Local: passwords read from macOS Keychain via `security find-generic-password`.
            //   Store once:
            //     security add-generic-password -a "huezoo-keystore" -s "huezoo-signing" -w "<store_pass>"
            //     security add-generic-password -a "huezoo-key"      -s "huezoo-signing" -w "<key_pass>"
            fun keychain(account: String): String? = runCatching {
                val proc = ProcessBuilder(
                    "security", "find-generic-password",
                    "-a", account, "-s", "huezoo-signing", "-w",
                ).start()
                proc.inputStream.bufferedReader().readLine()?.trim()
            }.getOrNull()

            val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
                ?: "/Users/ksharma/ksharma-xyz/krail_key.jks"
            storeFile = file(keystorePath)
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD") ?: keychain("huezoo-keystore")
            keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: "huezoo-key"
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD") ?: keychain("huezoo-key")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.core.ktx)
    implementation(libs.di.koinAndroid)
    debugImplementation(libs.compose.ui.tooling)
}
