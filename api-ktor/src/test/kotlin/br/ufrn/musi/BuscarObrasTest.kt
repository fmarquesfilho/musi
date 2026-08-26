package br.ufrn.musi

import br.ufrn.musi.aplicacao.BuscarObras
import br.ufrn.musi.aplicacao.FonteDeObras
import br.ufrn.musi.dominio.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * O ganho concreto da porta: este teste não sobe servidor, não abre porta e não usa
 * mock de biblioteca HTTP. Substitui a INTERFACE por seis linhas.
 */
class BuscarObrasTest {

    private val acervo = listOf(
        Obra("obra-01", "Ponteio", "Edu Lobo", 1967,
            listOf(Faceta("genero", "mpb"), Faceta("ritmo", "ponteio"))),
        Obra("obra-03", "Asa Branca", "Luiz Gonzaga", 1947,
            listOf(Faceta("genero", "forro"), Faceta("ritmo", "baiao"))),
    )

    private val emMemoria = object : FonteDeObras {
        override suspend fun buscar(filtro: Filtro) = acervo.filter { it.satisfaz(filtro) }
    }

    @Test
    fun buscaPorRitmo() = runTest {
        val obras = BuscarObras(emMemoria)(Filtro.Tem("ritmo", "baiao"))
        assertEquals(listOf("obra-03"), obras.map { it.id })
    }

    @Test
    fun buscaComposta() = runTest {
        val filtro = Filtro.E(listOf(Filtro.Tem("genero", "mpb"), Filtro.Ate(1970)))
        assertEquals(1, BuscarObras(emMemoria)(filtro).size)
    }
}
