// MUSI - app  |  DIM0524 Sistemas para Dispositivos Móveis
//
// O domínio vem de :shared — o MESMO módulo que a api usa.
// Testar:               ./gradlew :app:jvmTest
// Rodar a tela (desktop): ./gradlew :app:run
//   Em Codespaces/noVNC (sem GPU), acrescente -Pheadless para render por software:
//   DISPLAY=:1 ./gradlew :app:run -Pheadless
// Hot reload (já embutido no Compose Multiplatform): ./gradlew :app:hotRunJvm
//   A JetBrains Runtime que o hot reload exige é provisionada pela
//   foojay-resolver-convention (ver settings.gradle.kts).

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
        // A janela desktop, onde se roda o hot reload. O código de UI fica em
        // commonMain; só o ponto de entrada (main.kt) é específico do desktop.
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// Ponto de entrada do app desktop (tasks `:app:run` e `:app:hotRunJvm`).
compose.desktop {
    application {
        mainClass = "br.ufrn.musi.MainKt"

        // Codespaces/noVNC não têm GPU. Com -Pheadless, o app renderiza por
        // software (o flag chega à JVM do app por jvmArgs, não pelo daemon).
        if (project.hasProperty("headless")) {
            jvmArgs("-Dskiko.renderApi=SOFTWARE")
        }
    }
}
