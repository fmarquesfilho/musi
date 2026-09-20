package br.ufrn.musi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.ufrn.musi.dominio.Anotacao
import br.ufrn.musi.dominio.Faceta
import br.ufrn.musi.dominio.Obra
import br.ufrn.musi.dominio.violacoes
import br.ufrn.musi.dominio.violacoesDoCurador

/**
 * Detalhe da obra, com as anotações e o formulário para anotar.
 *
 * Como a TelaAcervo, não guarda estado do acervo: recebe a obra e as anotações, e devolve
 * o evento de anotar. O único estado local é o do formulário, que não interessa a ninguém
 * de fora — e é `rememberSaveable` para sobreviver à rotação da tela no Android.
 *
 * `aoVoltar` é nulo na janela larga, onde a lista continua à vista e não há para onde voltar.
 */
@Composable
fun TelaObra(
    obra: Obra?,
    anotacoes: List<Anotacao>,
    aoAnotar: (Faceta, String) -> Unit,
    aoVoltar: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (obra == null) {
        Column(modifier.fillMaxSize().padding(16.dp)) {
            Text("Selecione uma obra", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {

        if (aoVoltar != null) {
            TextButton(onClick = aoVoltar) { Text("Voltar") }
        }

        // heading(): o leitor de tela anuncia como título e permite pular entre títulos.
        Text(
            obra.titulo,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "${obra.artista} · ${obra.ano}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (obra.mbid != null) {
            // Identidade global, quando a obra já foi conciliada — ADR-0003.
            Text(
                "MusicBrainz: ${obra.mbid}",
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(12.dp))
        Text("Facetas", style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            obra.facetas.forEach { faceta ->
                AssistChip(
                    onClick = {},
                    label = { Text("${faceta.dimensao}: ${faceta.valor}") },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text("Anotações", style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(4.dp))
        if (anotacoes.isEmpty()) {
            Text("Nenhuma anotação ainda.", style = MaterialTheme.typography.bodyMedium)
        } else {
            anotacoes.forEach { anotacao ->
                Text(
                    "${anotacao.faceta.dimensao}: ${anotacao.faceta.valor} — por ${anotacao.curador}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        FormularioDeAnotacao(aoAnotar = aoAnotar)
    }
}

/**
 * COMPONENTE PRÓPRIO: o formulário de anotação.
 *
 * A validação não é reescrita aqui: chama `violacoes()` e `violacoesDoCurador()`, do
 * módulo `shared` — as MESMAS regras que a api aplica antes de gravar. O app e a API não
 * podem discordar sobre o que é uma faceta válida.
 *
 * Enquanto houver violação, o botão fica desabilitado e a primeira mensagem aparece.
 */
@Composable
fun FormularioDeAnotacao(
    aoAnotar: (Faceta, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dimensao by rememberSaveable { mutableStateOf("") }
    var valor by rememberSaveable { mutableStateOf("") }
    var curador by rememberSaveable { mutableStateOf("") }

    val violacoes = remember(dimensao, valor, curador) {
        Faceta(dimensao.trim(), valor.trim()).violacoes() + violacoesDoCurador(curador.trim())
    }
    val podeAnotar = violacoes.isEmpty()

    Column(modifier.fillMaxWidth()) {
        Text("Anotar esta obra", style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = dimensao,
                onValueChange = { dimensao = it },
                label = { Text("Dimensão") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = valor,
                onValueChange = { valor = it },
                label = { Text("Valor") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = curador,
            onValueChange = { curador = it },
            label = { Text("Curador") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // A mensagem aparece só depois que a pessoa digitou algo: campo vazio no começo
        // não é erro, é campo vazio.
        val algoDigitado = dimensao.isNotBlank() || valor.isNotBlank() || curador.isNotBlank()
        if (algoDigitado && !podeAnotar) {
            Spacer(Modifier.height(4.dp))
            Text(
                violacoes.first(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                aoAnotar(Faceta(dimensao.trim(), valor.trim()), curador.trim())
                dimensao = ""
                valor = ""
                curador = ""
            },
            enabled = podeAnotar,
        ) {
            Text("Anotar")
        }
    }
}
