// MUSI - app  |  DIM0524 Sistemas para Dispositivos Móveis
//
// O domínio vem de :shared — o MESMO módulo que a api usa.
// Testar (interface, no alvo desktop):  ./gradlew :app:jvmTest
// APK de debug (precisa do SDK do Android em ANDROID_HOME):
//                                       ./gradlew :app:assembleDebug
// Rodar a tela (desktop): ./gradlew :app:run
//   Em Codespaces/noVNC (sem GPU), acrescente -Pheadless para render por software:
//   DISPLAY=:1 ./gradlew :app:run -Pheadless
// Hot reload (já embutido no Compose Multiplatform): ./gradlew :app:hotRunJvm
//   A JetBrains Runtime que o hot reload exige é provisionada pela
//   foojay-resolver-convention (ver settings.gradle.kts).

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)   // rotas tipadas da navegação
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())

    // O alvo JVM é onde se testa: o domínio é Kotlin puro, sem plataforma, e
    // roda em segundos, sem emulador.
    jvm {
        testRuns["test"].executionTask.configure { useJUnitPlatform() }
    }

    // Sprint 1: o alvo Android, a plataforma declarada na proposta (seção 4.2).
    // A interface inteira vive em commonMain: o Android só traz a Activity.
    androidTarget()

    // Sprint 3: iOS.
    // listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
    //     it.binaries.framework { baseName = "Shared" }
    // }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))   // o domínio, compartilhado

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation("org.jetbrains.compose.ui:ui-tooling-preview:${libs.versions.compose.get()}")

            implementation(libs.navigation.compose)   // rotas tipadas e deep link
            implementation(libs.material3.adaptive)   // classes de tamanho de janela

            // Sprint 2: Koin — o MESMO que a api usa
            // Sprint 3: Ktor Client — o MESMO que a api usa
        }
        // A janela desktop, onde se roda o hot reload. O código de UI fica em
        // commonMain; só o ponto de entrada (main.kt) é específico do desktop.
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
        // A Activity e o render do @Preview no Android Studio.
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation("org.jetbrains.compose.ui:ui-tooling:${libs.versions.compose.get()}")
        }
        // Os testes de interface ficam em commonTest e rodam no alvo JVM (desktop),
        // sem emulador: ./gradlew :app:jvmTest
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.compose.ui.test)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

// O alvo Android. O que a rubrica chama de plataforma-alvo; o desktop existe para o
// ciclo rápido (hot reload) e para rodar os testes de interface sem emulador.
android {
    namespace = "br.ufrn.musi"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "br.ufrn.musi"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.2.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Os testes de interface vivem em `commonTest` e rodam no alvo JVM (desktop), onde o Skia
// desenha de verdade. A variante de teste UNITÁRIO do Android roda numa JVM sem Android
// (`android.os.Build.FINGERPRINT` vem nulo) e falharia neles, então fica desligada: os
// mesmos testes já rodaram em `:app:jvmTest`. Testes que precisem de aparelho iriam para
// `androidInstrumentedTest`, com emulador — assunto da Sprint 2.
tasks.matching { it.name.endsWith("UnitTest") }.configureEach { enabled = false }

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
