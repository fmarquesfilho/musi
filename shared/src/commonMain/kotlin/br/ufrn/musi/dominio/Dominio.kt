package br.ufrn.musi.dominio

/**
 * O domínio do MUSI em Kotlin.
 *
 * DIM0524 — Sistemas para Dispositivos Móveis.
 *
 * Este arquivo fica em `commonMain`: compila para Android, iOS e desktop, e não importa
 * Compose nem Ktor. A camada de domínio não conhece a interface nem a rede.
 * A regra está no `docs/STACK.md` da disciplina e é verificada pelo compilador: um import
 * de Compose aqui faz a compilação falhar.
 *
 * Ver docs/decisoes/0002 e 0003.
 */

/** Um par (dimensão, valor). A lista de dimensões não é fixa — ADR-0002. */
data class Faceta(val dimensao: String, val valor: String)

data class Obra(
    val id: String,
    val titulo: String,
    val artista: String,
    val ano: Int,
    val facetas: List<Faceta>
)

/**
 * A busca é uma árvore — ADR-0002.
 *
 * `sealed` indica que o compilador conhece todas as implementações, então um `when`
 * sobre Filtro é exaustivo: acrescentar um construtor faz a compilação falhar em todos
 * os pontos que precisam ser atualizados.
 *
 * Go não tem união etiquetada, e por isso não oferece essa verificação.
 */
sealed interface Filtro {
    data class Tem(val dimensao: String, val valor: String) : Filtro
    data class Ou(val opcoes: List<Filtro>) : Filtro
    data class E(val exigencias: List<Filtro>) : Filtro
    data class Exceto(val filtro: Filtro) : Filtro
    data class Ate(val ano: Int) : Filtro
}

/** Avalia o filtro contra uma obra. A recursão percorre a árvore. */
fun Obra.satisfaz(f: Filtro): Boolean = when (f) {
    is Filtro.Tem    -> facetas.any { it.dimensao == f.dimensao && it.valor == f.valor }
    is Filtro.Ou     -> f.opcoes.any { satisfaz(it) }
    is Filtro.E      -> f.exigencias.all { satisfaz(it) }
    is Filtro.Exceto -> !satisfaz(f.filtro)
    is Filtro.Ate    -> ano <= f.ano
}

/** Descrição legível do filtro, para depuração e para a interface. */
fun descrever(f: Filtro): String = when (f) {
    is Filtro.Tem    -> "${f.dimensao}=${f.valor}"
    is Filtro.Ou     -> f.opcoes.joinToString(" ou ", "(", ")") { descrever(it) }
    is Filtro.E      -> f.exigencias.joinToString(" e ", "(", ")") { descrever(it) }
    is Filtro.Exceto -> "não ${descrever(f.filtro)}"
    is Filtro.Ate    -> "ano<=${f.ano}"
}

/**
 * A ordenação é sempre declarada por quem consulta — ADR-0002.
 * Não há um valor `POR_RELEVANCIA`, e essa ausência é deliberada.
 */
enum class Ordenacao { TITULO, ARTISTA, ANO_CRESCENTE, ANO_DECRESCENTE }

fun List<Obra>.buscar(filtro: Filtro, ordenacao: Ordenacao = Ordenacao.TITULO): List<Obra> =
    filter { it.satisfaz(filtro) }.sortedWith(
        when (ordenacao) {
            Ordenacao.TITULO          -> compareBy { it.titulo }
            Ordenacao.ARTISTA         -> compareBy { it.artista }
            Ordenacao.ANO_CRESCENTE   -> compareBy { it.ano }
            Ordenacao.ANO_DECRESCENTE -> compareByDescending { it.ano }
        }
    )
