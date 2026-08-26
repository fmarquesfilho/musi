package br.ufrn.musi.dominio

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Os mesmos casos de contratos/exemplos/casos-de-busca.json.
 * api (Java) e services (Go) rodam estes mesmos casos e devem concordar.
 */
class FiltroTest {

    private val acervo = listOf(
        Obra("obra-01", "Ponteio", "Edu Lobo", 1967, listOf(
            Faceta("genero", "mpb"), Faceta("ritmo", "ponteio"),
            Faceta("movimento", "festivais-da-cancao"))),
        Obra("obra-02", "Beira Mar", "Gilberto Gil", 1969, listOf(
            Faceta("genero", "mpb"), Faceta("ritmo", "ijexa"),
            Faceta("movimento", "tropicalia"))),
        Obra("obra-03", "Asa Branca", "Luiz Gonzaga", 1947, listOf(
            Faceta("genero", "forro"), Faceta("ritmo", "baiao"),
            Faceta("instrumentacao", "sanfona"))),
        Obra("obra-04", "Rio Grande", "Chico Science & Nacao Zumbi", 1994, listOf(
            Faceta("ritmo", "maracatu"), Faceta("movimento", "manguebeat"),
            Faceta("regiao", "recife"))),
        Obra("obra-05", "Refazenda", "Gilberto Gil", 1975, listOf(
            Faceta("genero", "mpb"), Faceta("ritmo", "baiao")))
    )

    private fun ids(f: Filtro) = acervo.filter { it.satisfaz(f) }.map { it.id }

    @Test fun facetaSimples() {
        assertEquals(listOf("obra-03", "obra-05"), ids(Filtro.Tem("ritmo", "baiao")))
    }

    @Test fun disjuncaoDeRitmos() {
        val f = Filtro.Ou(listOf(Filtro.Tem("ritmo", "ijexa"), Filtro.Tem("ritmo", "maracatu")))
        assertEquals(listOf("obra-02", "obra-04"), ids(f))
    }

    @Test fun conjuncaoComExclusao() {
        val f = Filtro.E(listOf(
            Filtro.Tem("genero", "mpb"),
            Filtro.Exceto(Filtro.Tem("ritmo", "ijexa"))
        ))
        assertEquals(listOf("obra-01", "obra-05"), ids(f))
    }

    @Test fun tresNiveisComCortePorAno() {
        val f = Filtro.E(listOf(
            Filtro.Ou(listOf(
                Filtro.Tem("movimento", "tropicalia"),
                Filtro.Tem("movimento", "festivais-da-cancao")
            )),
            Filtro.Ate(1968),
            Filtro.Exceto(Filtro.Tem("ritmo", "ijexa"))
        ))
        assertEquals(listOf("obra-01"), ids(f))
    }

    /** Casos de borda que costumam surpreender. Valem discussão em aula. */
    @Test fun disjuncaoVaziaNaoAceitaNada() {
        assertEquals(emptyList(), ids(Filtro.Ou(emptyList())))
    }

    @Test fun conjuncaoVaziaAceitaTudo() {
        assertEquals(5, ids(Filtro.E(emptyList())).size)
    }

    @Test fun descreverAninhado() {
        val f = Filtro.E(listOf(
            Filtro.Ou(listOf(Filtro.Tem("ritmo", "ijexa"), Filtro.Tem("ritmo", "baiao"))),
            Filtro.Exceto(Filtro.Tem("genero", "forro"))
        ))
        assertEquals("((ritmo=ijexa ou ritmo=baiao) e não genero=forro)", descrever(f))
    }
}
