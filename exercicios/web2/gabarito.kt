// MUSI — GABARITO do exercício de Kotlin
// DIM0547 · cole em play.kotlinlang.org
//
// O mesmo domínio que a api Ktor usa. Sem Gradle, sem instalação.

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
    data class Ate(val ano: Int) : Filtro
}

fun Obra.satisfaz(f: Filtro): Boolean = when (f) {
    is Filtro.Tem    -> facetas.any { it.dimensao == f.dimensao && it.valor == f.valor }
    is Filtro.Ou     -> f.opcoes.any { satisfaz(it) }
    is Filtro.E      -> f.exigencias.all { satisfaz(it) }
    is Filtro.Exceto -> !satisfaz(f.filtro)
    is Filtro.Ate    -> ano <= f.ano
}

fun descrever(f: Filtro): String = when (f) {
    is Filtro.Tem    -> "${f.dimensao}=${f.valor}"
    is Filtro.Ou     -> f.opcoes.joinToString(" ou ", "(", ")") { descrever(it) }
    is Filtro.E      -> f.exigencias.joinToString(" e ", "(", ")") { descrever(it) }
    is Filtro.Exceto -> "não ${descrever(f.filtro)}"
    is Filtro.Ate    -> "ano<=${f.ano}"
}

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

    val tresNiveis = Filtro.E(listOf(
        Filtro.Tem("genero", "mpb"),
        Filtro.Ate(1970),
        Filtro.Exceto(Filtro.Tem("ritmo", "ijexa"))
    ))

    println("\nBusca: ${descrever(tresNiveis)}")
    acervo.filter { it.satisfaz(tresNiveis) }.forEach {
        println("  ${it.titulo} — ${it.artista} (${it.ano})")
    }
}
