// MUSI - app  |  DIM0524 Sistemas para Dispositivos Móveis
//
// O domínio vem de :shared — o MESMO módulo que a api usa.
// Rodar:  ./gradlew :app:jvmTest

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())

    // O alvo JVM é onde se testa: o domínio é Kotlin puro, sem plataforma, e
    // roda em segundos, sem emulador.
    jvm {
        testRuns["test"].executionTask.configure { useJUnitPlatform() }
    }

    // Sprint 1: os alvos reais.
    // androidTarget()
    // listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
    //     it.binaries.framework { baseName = "Shared" }
    // }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))   // o domínio, compartilhado

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)

            // Sprint 2: Koin — o MESMO que a api usa
            // Sprint 3: Ktor Client — o MESMO que a api usa
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
