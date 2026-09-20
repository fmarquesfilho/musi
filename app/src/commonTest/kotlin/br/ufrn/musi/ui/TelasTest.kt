package br.ufrn.musi.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.navigation.NavHostController
import androidx.navigation.NavUri
import androidx.navigation.compose.rememberNavController
import br.ufrn.musi.Conteudo
import br.ufrn.musi.dominio.Filtro
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Testes de interface: montam a tela, agem como usuário e leem a árvore de semântica — a
 * mesma que o leitor de tela usa. Por isso encontram os elementos pelo texto visível.
 *
 * Ficam em `commonTest` e rodam no alvo JVM (desktop), sem emulador:
 * `./gradlew :app:jvmTest`. O CI roda esta suíte no job `kotlin`.
 */
@OptIn(ExperimentalTestApi::class)
class TelasTest {

    @Test
    fun acervoListaAsObrasComOTituloComoCabecalho() = runComposeUiTest {
        setContent { Conteudo(largo = false) }

        onNodeWithText("Asa Branca").assertIsDisplayed()
        onNodeWithText("Ponteio").assertIsDisplayed()
        assertEquals(1, onAllNodes(isHeading()).fetchSemanticsNodes().size)
    }

    @Test
    fun filtroRapidoDeixaSoAsObrasQueOSatisfazem() = runComposeUiTest {
        setContent { Conteudo(largo = false) }

        onNodeWithText("ijexá").performClick()
        onNodeWithText("Beira Mar").assertIsDisplayed()
        assertTrue(onAllNodesWithText("Asa Branca").fetchSemanticsNodes().isEmpty())
        onNodeWithText("ritmo=ijexa").assertIsDisplayed()
    }

    @Test
    fun filtroSemResultadoExplicaAListaVazia() = runComposeUiTest {
        // A tela sozinha, sem navegação: é o que o estado elevado permite.
        setContent {
            TelaAcervo(
                obras = br.ufrn.musi.acervoDeExemplo,
                filtro = Filtro.Tem("ritmo", "coco"),
                aoTrocarFiltro = {},
                aoAbrir = {},
            )
        }
        onNodeWithText("Nenhuma obra atende a esse filtro.").assertIsDisplayed()
    }

    @Test
    fun janelaCompactaAbreODetalheEVolta() = runComposeUiTest {
        setContent { Conteudo(largo = false) }

        onNodeWithText("Asa Branca").performClick()
        onNodeWithText("Luiz Gonzaga · 1947").assertIsDisplayed()
        onNodeWithText("ritmo: baiao").assertIsDisplayed()
        onNodeWithText("Nenhuma anotação ainda.").assertIsDisplayed()

        onNodeWithText("Voltar").performClick()
        onNodeWithText("Ponteio").assertIsDisplayed()   // voltou à lista
    }

    @Test
    fun janelaLargaMostraAcervoEObraLadoALado() = runComposeUiTest {
        setContent { Conteudo(largo = true) }

        // Dois painéis: a primeira obra já vem aberta à direita, com as facetas e o
        // formulário, e o acervo continua à esquerda.
        onNodeWithText("Facetas").assertIsDisplayed()
        onNodeWithText("Anotar esta obra").assertIsDisplayed()
        onNodeWithText("Acervo").assertIsDisplayed()
        // Obra conciliada mostra a identidade global (ADR-0003).
        onNodeWithText("MusicBrainz: 11111111-1111-4111-8111-111111111111").assertIsDisplayed()

        // Tocar na obra troca o painel da direita, sem tirar a lista da tela: por isso o
        // mesmo texto passa a aparecer duas vezes, uma em cada painel.
        onNodeWithText("Asa Branca").performClick()
        assertEquals(2, onAllNodesWithText("Luiz Gonzaga · 1947").fetchSemanticsNodes().size)
        onNodeWithText("Ponteio").assertIsDisplayed()

        // Sem navegação, não há para onde voltar.
        assertTrue(onAllNodesWithText("Voltar").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun formularioSoAnotaComFacetaECuradorValidos() = runComposeUiTest {
        setContent { Conteudo(largo = false) }
        onNodeWithText("Asa Branca").performClick()

        onNodeWithText("Anotar").assertIsNotEnabled()

        // Maiúscula na dimensão viola a regra do domínio (shared), e a tela diz por quê.
        onNodeWithText("Dimensão").performTextInput("Ritmo")
        onNodeWithText("Valor").performTextInput("xote")
        onNodeWithText("Curador").performTextInput("ana")
        onNodeWithText("Anotar").assertIsNotEnabled()
        onNodeWithText("dimensão `Ritmo` fora do padrão: minúsculas, sem acento, com hífen")
            .assertIsDisplayed()
    }

    @Test
    fun anotacaoValidaApareceNaObra() = runComposeUiTest {
        setContent { Conteudo(largo = false) }
        onNodeWithText("Asa Branca").performClick()

        onNodeWithText("Dimensão").performTextInput("ritmo")
        onNodeWithText("Valor").performTextInput("xote")
        onNodeWithText("Curador").performTextInput("ana")
        onNodeWithText("Anotar").assertIsEnabled()
        onNodeWithText("Anotar").performClick()

        onNodeWithText("ritmo: xote — por ana").assertIsDisplayed()
        onNodeWithText("Anotar").assertIsNotEnabled()   // o formulário foi limpo
    }

    @Test
    fun deepLinkAbreAObraDireto() = runComposeUiTest {
        // musi://obra/obra-03 — o mesmo endereço declarado no AndroidManifest.xml.
        lateinit var navegacao: NavHostController
        setContent {
            navegacao = rememberNavController()
            Conteudo(largo = false, navController = navegacao)
        }

        runOnIdle { navegacao.navigate(NavUri("musi://obra/obra-03")) }
        waitForIdle()

        onNodeWithText("Luiz Gonzaga · 1947").assertIsDisplayed()
        onNodeWithText("Voltar").performClick()
        onNodeWithText("Acervo").assertIsDisplayed()
    }
}
