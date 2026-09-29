plugins {
    id("kds.android.feature")
}

dependencies {
    testImplementation(testFixtures(projects.core.domain))
}
