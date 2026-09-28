package io.github.gabrielevanger.kds.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/** Tela de feature: library com Compose, Hilt, ViewModel e os módulos core que toda feature usa. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("kds.android.library")
            pluginManager.apply("kds.android.compose")
            pluginManager.apply("kds.android.hilt")

            dependencies {
                add("implementation", project(":core:domain"))
                add("implementation", project(":core:designsystem"))
                add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
                add("implementation", libs.library("kotlinx-collections-immutable"))
            }
        }
    }
}
