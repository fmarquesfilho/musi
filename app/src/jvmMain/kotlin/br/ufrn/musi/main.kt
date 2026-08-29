package br.ufrn.musi

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import br.ufrn.musi.dominio.Filtro
import br.ufrn.musi.ui.TelaAcervo

/**
 * Ponto de entrada da janela desktop — a superfície onde o Compose Hot Reload roda.
 *
 * Rodar com hot reload:  ./gradlew :app:hotRunJvm
 *
 * Só o `main` é específico do desktop. A tela (`TelaAcervo`) e os componentes
 * (`CartaoObra`, `FiltrosRapidos`) vivem em `commonMain` e valem para Android e iOS.
 * O estado do filtro é elevado até aqui — a tela recebe o valor e devolve o evento.
 */
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "MUSI — Acervo") {
        MaterialTheme {
            Surface(Modifier.fillMaxSize()) {
                var filtro by remember { mutableStateOf<Filtro?>(null) }
                TelaAcervo(
                    obras = acervoDeExemplo,
                    filtro = filtro,
                    aoTrocarFiltro = { filtro = it },
                )
            }
        }
    }
}
