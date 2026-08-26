// MUSI - build Gradle unico para os componentes Kotlin.
//
// Ficam de fora, com ciclo proprio:
//   services/     Go
//   api-quarkus/  Java, com Maven

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

plugins {
    // Baixa o JDK declarado pelo toolchain, se ele nao estiver na maquina.
    // Sem isto, `jvmToolchain(25)` falha em quem tem outro JDK instalado.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// De onde vem cada dependencia. Sem este bloco, o build falha com
// "no repositories are defined" no primeiro `compileKotlin`.
//
// PREFER_SETTINGS: os modulos nao podem declarar repositorio proprio, o que
// mantem a resolucao previsivel e num lugar so.
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.PREFER_SETTINGS
    repositories {
        mavenCentral()
        google()            // artefatos do Compose Multiplatform
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "musi"

include(":shared")      // dominio compartilhado (KMP)
include(":api-ktor")    // Ktor + Koin           -> DIM0547
include(":app")         // Compose Multiplatform -> DIM0524
