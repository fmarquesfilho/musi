// MUSI — exercício da aula de Kotlin
// DIM0524 · Sprint 0 · cole em play.kotlinlang.org
//
// Ao final, você terá escrito um mecanismo de busca por facetas com combinação
// livre de critérios, definidos por quem consulta.

data class Faceta(val dimensao: String, val valor: String)

data class Obra(
    val id: String,
    val titulo: String,
    val artista: String,
    val ano: Int,
    val facetas: List<Faceta>
)

sealed interface Filtro {
    data class Tem(val dimensao: String, val valor: String) : Filtro
    data class Ou(val opcoes: List<Filtro>) : Filtro
    data class E(val exigencias: List<Filtro>) : Filtro
    data class Exceto(val filtro: Filtro) : Filtro
    // EXERCÍCIO 2 — descomente e faça compilar:
    // data class Ate(val ano: Int) : Filtro
}

fun Obra.satisfaz(f: Filtro): Boolean = when (f) {
    is Filtro.Tem    -> facetas.any { it.dimensao == f.dimensao && it.valor == f.valor }
    is Filtro.Ou     -> f.opcoes.any { satisfaz(it) }
    is Filtro.E      -> f.exigencias.all { satisfaz(it) }
    is Filtro.Exceto -> !satisfaz(f.filtro)
}

// EXERCÍCIO 1 — escreva esta função.
// Deve produzir, para o filtro do main:
//   ((ritmo=ijexa ou ritmo=baiao) e não genero=forro)
fun descrever(f: Filtro): String = TODO("exercício 1")

val acervo = listOf(
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

fun main() {
    val busca = Filtro.E(listOf(
        Filtro.Ou(listOf(Filtro.Tem("ritmo", "ijexa"), Filtro.Tem("ritmo", "baiao"))),
        Filtro.Exceto(Filtro.Tem("genero", "forro"))
    ))

    println("Busca: ${descrever(busca)}")
    acervo.filter { it.satisfaz(busca) }.forEach {
        println("  ${it.titulo} — ${it.artista} (${it.ano})")
    }

    // EXERCÍCIO 3 — monte uma busca com três níveis, usando Ate.
    // Sugestão: tropicália ou festivais, até 1968, exceto ijexá.
    // Resultado esperado: só "Ponteio".
}
