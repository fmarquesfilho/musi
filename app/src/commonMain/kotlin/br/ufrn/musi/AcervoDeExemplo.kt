package br.ufrn.musi

import br.ufrn.musi.dominio.Faceta
import br.ufrn.musi.dominio.Obra

/**
 * Acervo de exemplo, em memória — Sprint 0.
 *
 * A partir da Sprint 3, vem da api. Ter os dados aqui agora permite construir e
 * testar a tela sem depender de rede, o que é o ponto do estado elevado.
 *
 * Dados inventados para o curso — ver ADR-0002.
 */
val acervoDeExemplo = listOf(
    Obra("obra-01", "Ponteio", "Edu Lobo", 1967, listOf(
        Faceta("genero", "mpb"), Faceta("ritmo", "ponteio"),
        Faceta("movimento", "festivais-da-cancao"))),
    Obra("obra-02", "Beira Mar", "Gilberto Gil", 1969, listOf(
        Faceta("genero", "mpb"), Faceta("ritmo", "ijexa"),
        Faceta("movimento", "tropicalia"))),
    Obra("obra-03", "Asa Branca", "Luiz Gonzaga", 1947, listOf(
        Faceta("genero", "forro"), Faceta("ritmo", "baiao"),
        Faceta("instrumentacao", "sanfona"))),
    Obra("obra-04", "Rio Grande", "Chico Science & Nação Zumbi", 1994, listOf(
        Faceta("ritmo", "maracatu"), Faceta("movimento", "manguebeat"),
        Faceta("regiao", "recife"))),
    Obra("obra-05", "Refazenda", "Gilberto Gil", 1975, listOf(
        Faceta("genero", "mpb"), Faceta("ritmo", "baiao"))),
)
