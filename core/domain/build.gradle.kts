plugins {
    id("kds.jvm.library")
}

dependencies {
    // Expostos na API pública do domínio (Flow e ImmutableList), por isso api e não implementation.
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.collections.immutable)
}
