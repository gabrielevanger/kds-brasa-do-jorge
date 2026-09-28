plugins {
    id("kds.android.application")
    id("kds.android.compose")
    id("kds.android.hilt")
}

android {
    defaultConfig {
        applicationId = "io.github.gabrielevanger.kds"
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.feature.board)
    implementation(projects.feature.expedition)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.material3.adaptive)
}
