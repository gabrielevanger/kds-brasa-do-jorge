plugins {
    id("kds.android.feature")
}

android {
    testOptions {
        // Robolectric precisa dos recursos do módulo (strings, ícones) para renderizar o Compose na JVM.
        unitTests.isIncludeAndroidResources = true
    }
}

// O Robolectric acessa internos do JDK ao simular arquivos do Android; o Java 21 exige liberação explícita.
tasks.withType<Test>().configureEach {
    jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
}

dependencies {
    testImplementation(testFixtures(projects.core.domain))

    // Testes de UI do Compose na JVM, sem emulador, rodando no CI junto com os unitários.
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testRuntimeOnly(libs.junit.vintage.engine)
}
