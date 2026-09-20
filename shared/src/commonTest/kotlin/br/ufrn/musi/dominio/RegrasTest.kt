package br.ufrn.musi.dominio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** As regras de forma, sem banco nem HTTP: rodam em todos os alvos. */
class RegrasTest {

    @Test
    fun facetaNoPadraoNaoTemViolacao() {
        assertTrue(Faceta("instrumentacao", "viola-caipira").violacoes().isEmpty())
    }

    @Test
    fun facetaComMaiusculaOuAcentoEhRecusada() {
        assertEquals(2, Faceta("Ritmo", "baião").violacoes().size)
    }

    @Test
    fun obraValida() {
        assertTrue(violacoesDaObra("Asa Branca", "Luiz Gonzaga", 1947, listOf(Faceta("ritmo", "baiao"))).isEmpty())
    }

    @Test
    fun obraAcumulaTodasAsViolacoes() {
        val violacoes = violacoesDaObra(" ", "", 1500, listOf(Faceta("ritmo", "baiao"), Faceta("ritmo", "baiao")))
        assertEquals(listOf("título vazio", "artista vazio", "ano 1500 fora de 1877..2100", "faceta repetida"), violacoes)
    }

    @Test
    fun anotacaoSemCuradorEhRecusada() {
        assertEquals(1, violacoesDoCurador("  ").size)
    }
}
