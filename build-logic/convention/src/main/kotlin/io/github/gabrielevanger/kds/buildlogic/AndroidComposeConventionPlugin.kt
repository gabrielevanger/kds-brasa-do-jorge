package io.github.gabrielevanger.kds.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/** Habilita Compose. Deve ser aplicado depois de kds.android.library ou kds.android.application. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            extensions.getByType<CommonExtension>().buildFeatures.compose = true

            dependencies {
                val bom = platform(libs.library("compose-bom"))
                add("implementation", bom)
                add("androidTestImplementation", bom)
                add("implementation", libs.library("compose-ui"))
                add("implementation", libs.library("compose-ui-graphics"))
                add("implementation", libs.library("compose-ui-tooling-preview"))
                add("implementation", libs.library("compose-material3"))
                add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
                add("debugImplementation", libs.library("compose-ui-tooling"))
                add("debugImplementation", libs.library("compose-ui-test-manifest"))
                add("androidTestImplementation", libs.library("compose-ui-test-junit4"))
            }
        }
    }
}
