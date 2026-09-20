package br.ufrn.musi

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

/**
 * Ponto de entrada da janela desktop — a superfície onde o Compose Hot Reload roda.
 *
 * Rodar com hot reload:  ./gradlew :app:hotRunJvm
 *
 * Só o `main` é específico do desktop: a interface inteira (`App`, com a navegação, as
 * telas e os componentes) vive em `commonMain` e é a mesma que o Android monta na
 * `MainActivity`.
 *
 * Redimensionar a janela troca o layout: estreita, uma tela por vez; larga, acervo e obra
 * lado a lado. É a mesma decisão que separa celular em pé de tablet.
 */
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "MUSI — Acervo") {
        App()
    }
}
