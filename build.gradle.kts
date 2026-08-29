// Raiz do build. Os plugins sao declarados aqui e aplicados nos modulos.
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm)           apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ktor)                 apply false
    alias(libs.plugins.compose.hot.reload)   apply false
    alias(libs.plugins.versions)
}
