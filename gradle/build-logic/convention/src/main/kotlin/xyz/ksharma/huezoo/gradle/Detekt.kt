package xyz.ksharma.huezoo.gradle

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

fun Project.configureDetekt() {
    dependencies.add("detektPlugins", libs.findLibrary("detekt-formatting").get())
    dependencies.add("detektPlugins", libs.findLibrary("detekt-compose").get())

    extensions.configure<DetektExtension>("detekt") {
        autoCorrect = true
        buildUponDefaultConfig = true
        allRules = false
        config.setFrom("${rootProject.projectDir}/config/detekt.yml")
        baseline = file("$projectDir/baseline.xml")
        source.setFrom(
            "src/commonMain/kotlin",
            "src/androidMain/kotlin",
            "src/iosMain/kotlin",
        )
    }

    tasks.withType(Detekt::class.java).configureEach {
        reports.html.required.set(true)
        reports.md.required.set(true)
        jvmTarget = JvmTarget.JVM_21.target
    }
    tasks.withType(DetektCreateBaselineTask::class.java).configureEach {
        jvmTarget = JvmTarget.JVM_21.target
    }
}
