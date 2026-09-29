import java.util.Properties

plugins {
    id("kds.android.application")
    id("kds.android.compose")
    id("kds.android.hilt")
}

// Endereço do mock: -Pkds.serverUrl=... ou kds.serverUrl no local.properties.
// O padrão 10.0.2.2 é o computador visto de dentro do emulador.
val localProperties = Properties().apply {
    providers.fileContents(rootProject.layout.projectDirectory.file("local.properties"))
        .asText.orNull?.let { load(it.reader()) }
}
val serverUrl: String = providers.gradleProperty("kds.serverUrl").orNull
    ?: localProperties.getProperty("kds.serverUrl")
    ?: "http://10.0.2.2:4000/"

android {
    defaultConfig {
        applicationId = "io.github.gabrielevanger.kds"
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "KDS_SERVER_URL", "\"$serverUrl\"")
    }
    buildFeatures {
        buildConfig = true
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
