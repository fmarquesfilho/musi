// MUSI - dominio compartilhado.
//
// Este modulo e a razao de `api/` e `app/` viverem no mesmo build: o dominio
// deixou de ser duplicado entre Java e Kotlin e passou a ser UM arquivo,
// importado pelos dois. Ver docs/decisoes/0001-stacks-e-estrutura.md.
//
// commonMain nao pode importar Ktor, Koin nem Compose. E Kotlin puro.

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())

    jvm()          // usado por :api e pelos testes rapidos de :app

    // Sprint 1: os alvos moveis.
    // androidTarget()
    // listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
    //     it.binaries.framework { baseName = "Shared" }
    // }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
