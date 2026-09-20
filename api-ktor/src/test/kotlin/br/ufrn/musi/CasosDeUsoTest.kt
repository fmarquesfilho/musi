package br.ufrn.musi

import br.ufrn.musi.aplicacao.AnotacoesDeObras
import br.ufrn.musi.aplicacao.CatalogoDeObras
import br.ufrn.musi.aplicacao.Conflito
import br.ufrn.musi.aplicacao.DadosDaAnotacao
import br.ufrn.musi.aplicacao.DadosDaObra
import br.ufrn.musi.aplicacao.EntradaInvalida
import br.ufrn.musi.aplicacao.FiltroDeObras
import br.ufrn.musi.aplicacao.NaoEncontrado
import br.ufrn.musi.aplicacao.PedidoDePagina
import br.ufrn.musi.dominio.Faceta
import br.ufrn.musi.dominio.Ordenacao
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Os casos de uso com as portas em memória: sem servidor, sem banco. É onde as regras
 * (validação, obra inexistente, anotação repetida) se testam mais barato.
 */
class CasosDeUsoTest {

    private val obras = ObrasEmMemoria(ASA_BRANCA, PONTEIO)
    private val catalogo = CatalogoDeObras(obras)
    private val anotacoes = AnotacoesDeObras(obras, AnotacoesEmMemoria())

    @Test
    fun criarValidaTodasAsRegrasDeUmaVez() = runTest {
        val erro = assertFailsWith<EntradaInvalida> {
            catalogo.criar(DadosDaObra("", "Gil", 1500, listOf(Faceta("Ritmo", "ijexa")), mbid = "nao-e-uuid"))
        }
        assertEquals(4, erro.violacoes.size, erro.violacoes.toString())
    }

    @Test
    fun obraInexistenteEhNaoEncontrado() = runTest {
        assertFailsWith<NaoEncontrado> { catalogo.buscar("obra-99") }
        assertFailsWith<NaoEncontrado> { catalogo.remover("obra-99") }
    }

    @Test
    fun faixaDeAnosInvertidaEhRecusada() = runTest {
        assertFailsWith<EntradaInvalida> {
            catalogo.listar(FiltroDeObras(anoDe = 1970, anoAte = 1960), Ordenacao.TITULO, PedidoDePagina())
        }
    }

    @Test
    fun tamanhoDePaginaTemTeto() {
        assertFailsWith<EntradaInvalida> { PedidoDePagina(tamanho = PedidoDePagina.MAXIMO + 1) }
    }

    @Test
    fun anotarObraInexistenteEhNaoEncontradoDaObra() = runTest {
        val erro = assertFailsWith<NaoEncontrado> {
            anotacoes.criar("obra-99", DadosDaAnotacao(Faceta("ritmo", "xote"), "ana"))
        }
        assertEquals("obra `obra-99` não existe", erro.message)
    }

    @Test
    fun anotacaoSemCuradorEhRecusada() = runTest {
        assertFailsWith<EntradaInvalida> { anotacoes.criar("obra-03", DadosDaAnotacao(Faceta("ritmo", "xote"), " ")) }
    }

    @Test
    fun curadoresDiferentesDivergemMasOMesmoNaoRepete() = runTest {
        anotacoes.criar("obra-03", DadosDaAnotacao(Faceta("ritmo", "xote"), "ana"))
        anotacoes.criar("obra-03", DadosDaAnotacao(Faceta("ritmo", "baiao"), "bia"))
        assertFailsWith<Conflito> { anotacoes.criar("obra-03", DadosDaAnotacao(Faceta("ritmo", "xote"), "ana")) }
    }
}
