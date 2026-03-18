package xyz.ksharma.huezoo.gradle

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project

fun Project.configureAndroid() {
    extensions.configure(CommonExtension::class.java) {
        compileSdk = AndroidVersion.COMPILE_SDK
    }

    extensions.findByName("android")?.let { ext ->
        when (ext) {
            is ApplicationExtension -> ext.defaultConfig.targetSdk = AndroidVersion.TARGET_SDK
            is LibraryExtension -> ext.testOptions.targetSdk = AndroidVersion.TARGET_SDK
        }
    }
}

object AndroidVersion {
    const val COMPILE_SDK = 36
    const val MIN_SDK = 28
    const val TARGET_SDK = 36
}
