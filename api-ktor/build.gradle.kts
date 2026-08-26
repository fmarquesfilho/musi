// MUSI - api  |  DIM0547 Desenvolvimento Web II
//
// Ktor + Koin sobre Kotlin/JVM. Ver docs/decisoes/0001-stacks-e-estrutura.md.

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
}

application {
    mainClass.set("br.ufrn.musi.AplicacaoKt")
}

kotlin { jvmToolchain(libs.versions.java.get().toInt()) }

dependencies {
    // O dominio vem do modulo compartilhado, o MESMO que o app usa.
    implementation(project(":shared"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.negotiation)
    implementation(libs.ktor.server.statuspages)
    implementation(libs.ktor.server.calllogging)
    implementation(libs.ktor.server.caching)      // Cache-Control
    implementation(libs.ktor.server.conditional)  // ETag e 304
    implementation(libs.ktor.json)

    // Ktor Client - o MESMO que o app usa para falar com esta api
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.negotiation)

    implementation(libs.koin.ktor)
    implementation(libs.logback)

    // Sprint 2: Postgres no Neon. Descomente junto com BancoNeon.kt.
    // implementation("com.zaxxer:HikariCP:6.3.0")
    // implementation("org.postgresql:postgresql:42.7.7")

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.testhost)
    testImplementation(libs.koin.test)
}
