package br.ufrn.musi

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import androidx.window.core.layout.WindowSizeClass
import br.ufrn.musi.dominio.Anotacao
import br.ufrn.musi.dominio.Faceta
import br.ufrn.musi.dominio.Filtro
import br.ufrn.musi.ui.TelaAcervo
import br.ufrn.musi.ui.TelaObra
import kotlin.time.Clock
import kotlinx.serialization.Serializable

/**
 * Rotas tipadas: cada destino é um tipo, e o argumento é uma propriedade.
 *
 * O id da obra é `String` porque é assim no domínio (`Obra.id`) e na api — `obra-03` no
 * acervo de exemplo, um UUID nas obras criadas pelo CRUD.
 */
@Serializable
object Acervo

@Serializable
data class DetalheDaObra(val id: String)

/**
 * O deep link do MUSI: `musi://obra/obra-03` abre a obra direto.
 *
 * O caminho base combina com a rota `DetalheDaObra`, cujo argumento `id` vira o último
 * segmento. No Android, o mesmo esquema está declarado no `AndroidManifest.xml`.
 */
const val BASE_DO_DEEP_LINK = "musi://obra"

/**
 * O tema, nos dois modos. `MaterialTheme` sozinho é sempre claro: quem decide é
 * `isSystemInDarkTheme()`, que no Android segue o tema do sistema e no desktop, o do
 * sistema operacional.
 *
 * As telas não escolhem cor: usam os papéis do tema (`onSurfaceVariant`, `error`), e é
 * isso que faz o modo escuro funcionar sem retoque e o contraste continuar de pé.
 */
@Composable
fun TemaMusi(escuro: Boolean = isSystemInDarkTheme(), conteudo: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (escuro) darkColorScheme() else lightColorScheme(),
        content = conteudo,
    )
}

@Composable
fun App() {
    TemaMusi {
        // Surface pinta o fundo do tema (claro e escuro). safeDrawingPadding afasta o
        // conteúdo da barra de status e do recorte da câmera.
        Surface(Modifier.fillMaxSize()) {
            val largura = currentWindowAdaptiveInfo().windowSizeClass
            Conteudo(
                largo = largura.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND),
                modifier = Modifier.safeDrawingPadding(),
            )
        }
    }
}

/**
 * O estado fica ACIMA da navegação: as duas telas leem o mesmo acervo e as mesmas
 * anotações, e nenhuma delas guarda estado próprio.
 *
 * `largo` chega por parâmetro para que o teste escolha o layout sem redimensionar janela.
 * Sprint 2: este estado sai daqui e vai para um ViewModel; Sprint 3: vem da api.
 */
@Composable
fun Conteudo(
    largo: Boolean,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val obras = acervoDeExemplo
    var filtro by remember { mutableStateOf<Filtro?>(null) }
    var anotacoes by remember { mutableStateOf(emptyList<Anotacao>()) }
    var selecionada by rememberSaveable { mutableStateOf(obras.first().id) }

    // Anotar é a única escrita do app nesta sprint. A regra de quem pode afirmar o quê
    // é do domínio (shared), não da tela.
    val anotar = { obraId: String, faceta: Faceta, curador: String ->
        anotacoes = anotacoes + Anotacao(
            id = anotacoes.size + 1L,
            obraId = obraId,
            faceta = faceta,
            curador = curador,
            criadoEm = Clock.System.now(),
        )
    }

    if (largo) {
        // Janela larga (tablet, desktop, celular deitado): lista e detalhe lado a lado,
        // sem navegar. O toque na obra troca o painel da direita.
        Row(modifier.fillMaxSize()) {
            TelaAcervo(
                obras = obras,
                filtro = filtro,
                aoTrocarFiltro = { filtro = it },
                aoAbrir = { selecionada = it },
                modifier = Modifier.width(360.dp),
            )
            VerticalDivider()
            TelaObra(
                obra = obras.find { it.id == selecionada },
                anotacoes = anotacoes.filter { it.obraId == selecionada },
                aoAnotar = { faceta, curador -> anotar(selecionada, faceta, curador) },
                aoVoltar = null,          // não há para onde voltar: a lista está à vista
                modifier = Modifier.fillMaxHeight(),
            )
        }
    } else {
        // Janela compacta (celular em pé): uma tela por vez, com pilha de retorno.
        NavHost(navController, startDestination = Acervo, modifier = modifier) {
            composable<Acervo> {
                TelaAcervo(
                    obras = obras,
                    filtro = filtro,
                    aoTrocarFiltro = { filtro = it },
                    aoAbrir = { id -> navController.navigate(DetalheDaObra(id)) },
                )
            }
            composable<DetalheDaObra>(
                deepLinks = listOf(navDeepLink<DetalheDaObra>(basePath = BASE_DO_DEEP_LINK)),
            ) { entrada ->
                val rota = entrada.toRoute<DetalheDaObra>()
                TelaObra(
                    obra = obras.find { it.id == rota.id },
                    anotacoes = anotacoes.filter { it.obraId == rota.id },
                    aoAnotar = { faceta, curador -> anotar(rota.id, faceta, curador) },
                    aoVoltar = { navController.popBackStack() },
                )
            }
        }
    }
}
