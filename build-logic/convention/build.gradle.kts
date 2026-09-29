plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

ktlint {
    version.set(libs.versions.ktlint)
}

group = "io.github.gabrielevanger.kds.buildlogic"

kotlin {
    jvmToolchain(
        libs.versions.jvmToolchain
            .get()
            .toInt(),
    )
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("jvmLibrary") {
            id = "kds.jvm.library"
            implementationClass = "io.github.gabrielevanger.kds.buildlogic.JvmLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "kds.android.library"
            implementationClass = "io.github.gabrielevanger.kds.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "kds.android.application"
            implementationClass = "io.github.gabrielevanger.kds.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidCompose") {
            id = "kds.android.compose"
            implementationClass = "io.github.gabrielevanger.kds.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "kds.android.hilt"
            implementationClass = "io.github.gabrielevanger.kds.buildlogic.AndroidHiltConventionPlugin"
        }
        register("androidFeature") {
            id = "kds.android.feature"
            implementationClass = "io.github.gabrielevanger.kds.buildlogic.AndroidFeatureConventionPlugin"
        }
    }
}
