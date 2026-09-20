package br.ufrn.musi.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.ufrn.musi.dominio.*

/**
 * Primeira tela — a entrega da Sprint 0 em DIM0524.
 *
 * A rubrica pede componente próprio e reutilizável, com estado elevado.
 *
 * Estado elevado significa que esta função não guarda estado: recebe o que mostrar e
 * devolve eventos para quem chamou. Com isso, dá para pré-visualizar passando
 * parâmetros e testar sem subir a aplicação.
 *
 * Guardar o `filtro` aqui dentro com `remember` funcionaria, mas ninguém de fora
 * conseguiria decidir o que a tela mostra.
 */
@Composable
fun TelaAcervo(
    obras: List<Obra>,
    filtro: Filtro?,
    aoTrocarFiltro: (Filtro?) -> Unit,
    aoAbrir: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visiveis = remember(obras, filtro) {
        if (filtro == null) obras else obras.filter { it.satisfaz(filtro) }
    }

    Column(modifier.fillMaxSize().padding(16.dp)) {

        // heading(): o leitor de tela anuncia como título e permite pular entre títulos.
        Text(
            "Acervo",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = filtro?.let { descrever(it) } ?: "sem filtro",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(12.dp))

        FiltrosRapidos(
            selecionado = filtro,
            aoSelecionar = aoTrocarFiltro,
        )

        Spacer(Modifier.height(12.dp))

        if (visiveis.isEmpty()) {
            // Lista vazia com explicação, e não uma tela em branco.
            Text(
                "Nenhuma obra atende a esse filtro.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visiveis, key = { it.id }) { obra ->
                    CartaoObra(obra, aoAbrir = { aoAbrir(obra.id) })
                }
            }
        }
    }
}

/**
 * COMPONENTE PRÓPRIO E REUTILIZÁVEL — o que a rubrica pede.
 *
 * Não conhece a tela em que está, não guarda estado e não decide nada sobre o
 * acervo. Recebe uma obra e avisa quando foi tocado. Serve na lista, numa tela de detalhe
 * ou numa pré-visualização.
 */
@Composable
fun CartaoObra(
    obra: Obra,
    aoAbrir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // O cartão inteiro é o alvo de toque, e não um ícone pequeno: passa dos 48 dp que a
    // acessibilidade pede, e o leitor de tela anuncia um elemento só, com o título dentro.
    Card(onClick = aoAbrir, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(obra.titulo, style = MaterialTheme.typography.titleMedium)
            Text(
                "${obra.artista} · ${obra.ano}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (obra.facetas.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    obra.facetas.joinToString(" · ") { it.valor },
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Segundo componente próprio: atalhos de filtro.
 *
 * Também sem estado interno — recebe o selecionado e avisa a mudança.
 */
@Composable
fun FiltrosRapidos(
    selecionado: Filtro?,
    aoSelecionar: (Filtro?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val atalhos = listOf(
        "todos" to null,
        "baião" to Filtro.Tem("ritmo", "baiao"),
        "ijexá" to Filtro.Tem("ritmo", "ijexa"),
        "até 1970" to Filtro.Ate(1970),
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        atalhos.forEach { (rotulo, filtro) ->
            FilterChip(
                selected = selecionado == filtro,
                onClick = { aoSelecionar(filtro) },
                label = { Text(rotulo) },
            )
        }
    }
}
