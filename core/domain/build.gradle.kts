plugins {
    id("kds.jvm.library")
    // Fixtures de pedido compartilhadas com os testes de outros módulos, fora do código de produção.
    `java-test-fixtures`
}

dependencies {
    // Expostos na API pública do domínio (Flow e ImmutableList), por isso api e não implementation.
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.collections.immutable)
}
