package io.github.gabrielevanger.kds.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension

internal fun Project.configureKotlinToolchain() {
    extensions.configure<KotlinBaseExtension> {
        jvmToolchain(libs.intVersionOf("jvmToolchain"))
    }
}

internal fun Project.configureAndroid(extension: CommonExtension) {
    val javaVersion = JavaVersion.toVersion(libs.intVersionOf("jvmToolchain"))
    extension.apply {
        namespace = namespaceFromPath()
        compileSdk = libs.intVersionOf("compileSdk")
        defaultConfig.minSdk = libs.intVersionOf("minSdk")
        compileOptions.sourceCompatibility = javaVersion
        compileOptions.targetCompatibility = javaVersion
    }
    configureKotlinToolchain()
}

internal fun Project.configureUnitTests() {
    dependencies {
        add("testImplementation", platform(libs.library("junit-bom")))
        add("testImplementation", libs.library("junit-jupiter"))
        add("testRuntimeOnly", libs.library("junit-platform-launcher"))
        add("testImplementation", libs.library("kotlinx-coroutines-test"))
        add("testImplementation", libs.library("turbine"))
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
