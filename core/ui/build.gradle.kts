// Interface compartilhada pelas telas da cozinha: board, Expedição e TV.
plugins {
    id("kds.android.library")
    id("kds.android.compose")
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.designsystem)
    implementation(libs.kotlinx.collections.immutable)

    testImplementation(testFixtures(projects.core.domain))
}
