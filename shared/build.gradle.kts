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
    alias(libs.plugins.android.kmp.library)   // o dominio tambem compila para Android
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())

    jvm()          // usado por :api-ktor e pelos testes rapidos de :app

    // Sprint 1: o alvo Android. O MESMO Dominio.kt que a api usa vai no APK.
    // `com.android.kotlin.multiplatform.library` e o plugin de biblioteca KMP do AGP:
    // nao ha AndroidManifest nem recursos aqui, so codigo.
    androidLibrary {
        namespace = "br.ufrn.musi.dominio"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    // Sprint 3: iOS.
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
