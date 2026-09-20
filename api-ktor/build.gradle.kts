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
    // Engine CIO (corrotinas puras) em vez do Netty: menor footprint de memória
    // e único engine com suporte a GraalVM native. Ver docs/BENCHMARK.md e docs/FOOTPRINT-KOTLIN.md.
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.negotiation)
    implementation(libs.ktor.server.statuspages)
    implementation(libs.ktor.server.calllogging)
    implementation(libs.ktor.server.caching)      // Cache-Control
    implementation(libs.ktor.server.conditional)  // ETag e 304
    implementation(libs.ktor.server.cors)         // CORS: acesso do navegador (Hoppscotch/Swagger em outra origem)
    implementation(libs.ktor.json)

    // OpenAPI gerado das rotas (`describe`), + Swagger UI. Os dois são do próprio Ktor.
    // O spec sai do código, como no lado Quarkus — não de um arquivo à mão.
    implementation(libs.ktor.server.openapi)
    implementation(libs.ktor.server.swagger)

    // Ktor Client - o MESMO que o app usa para falar com esta api
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.negotiation)

    implementation(libs.koin.ktor)
    implementation(libs.logback)

    // Persistência: Exposed (SQL em Kotlin), driver JDBC, pool e migrações.
    // O PostgreSQL é local (docker compose) e do CI até a Sprint 3 — ADR-0004.
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.javatime)
    implementation(libs.postgresql)
    implementation(libs.hikari)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.testhost)
    testImplementation(libs.koin.test)
    testImplementation(libs.testcontainers.postgres)   // PostgreSQL descartável, num container
    testImplementation(libs.archunit)                  // regra de dependência, como teste
}

// Os testes de integração (tag `integracao`) sobem um PostgreSQL com Testcontainers e
// precisam de Docker. `-PsemDocker` os deixa de fora — é o que o verificar.sh faz quando
// não encontra Docker. O CI roda todos.
tasks.test {
    useJUnitPlatform {
        if (project.hasProperty("semDocker")) excludeTags("integracao")
    }
}
