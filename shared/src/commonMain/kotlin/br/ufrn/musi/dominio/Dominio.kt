package br.ufrn.musi.dominio

import kotlin.time.Instant

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
    val facetas: List<Faceta>,
    // Identidade global, preenchida na conciliação com o MusicBrainz — ADR-0003.
    // Nulo até a obra ser conciliada; a busca não depende deles.
    val mbid: String? = null,             // a gravação (recording)
    val mbidComposicao: String? = null,   // a composição (work), compartilhada por regravações
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

/**
 * Uma faceta atribuída a uma obra por um curador identificável — ver docs/DOMINIO.md.
 *
 * Anotações divergentes de curadores diferentes coexistem: o catálogo registra quem afirma
 * o quê, sem arbitrar entre as afirmações. `criadoEm` é o instante da anotação; a
 * representação (ISO-8601) é decisão de quem serializa.
 */
data class Anotacao(
    val id: Long,
    val obraId: String,
    val faceta: Faceta,
    val curador: String,
    val criadoEm: Instant,
)

/**
 * As regras de forma do domínio, as mesmas de contratos/obra.schema.json.
 *
 * Devolvem a lista de violações em vez de lançar exceção: quem valida uma entrada quer
 * todas as mensagens de uma vez, não a primeira.
 */
object Regras {
    /** Minúsculas, sem acento, com hífen — docs/GLOSSARIO.md. */
    val TERMO = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
    const val ANO_MINIMO = 1877       // o fonógrafo
    const val ANO_MAXIMO = 2100
    const val TAMANHO_MAXIMO_TEXTO = 300
    const val TAMANHO_MAXIMO_TERMO = 60
    const val TAMANHO_MAXIMO_CURADOR = 120
}

fun Faceta.violacoes(): List<String> = buildList {
    if (!Regras.TERMO.matches(dimensao) || dimensao.length > Regras.TAMANHO_MAXIMO_TERMO)
        add("dimensão `$dimensao` fora do padrão: minúsculas, sem acento, com hífen")
    if (!Regras.TERMO.matches(valor) || valor.length > Regras.TAMANHO_MAXIMO_TERMO)
        add("valor `$valor` fora do padrão: minúsculas, sem acento, com hífen")
}

/** Violações dos campos de uma obra, antes de ela ter identidade. */
fun violacoesDaObra(titulo: String, artista: String, ano: Int, facetas: List<Faceta>): List<String> = buildList {
    if (titulo.isBlank()) add("título vazio")
    if (titulo.length > Regras.TAMANHO_MAXIMO_TEXTO) add("título com mais de ${Regras.TAMANHO_MAXIMO_TEXTO} caracteres")
    if (artista.isBlank()) add("artista vazio")
    if (artista.length > Regras.TAMANHO_MAXIMO_TEXTO) add("artista com mais de ${Regras.TAMANHO_MAXIMO_TEXTO} caracteres")
    if (ano !in Regras.ANO_MINIMO..Regras.ANO_MAXIMO) add("ano $ano fora de ${Regras.ANO_MINIMO}..${Regras.ANO_MAXIMO}")
    facetas.forEach { addAll(it.violacoes()) }
    if (facetas.size != facetas.toSet().size) add("faceta repetida")
}

fun violacoesDoCurador(curador: String): List<String> = buildList {
    if (curador.isBlank()) add("curador vazio: toda anotação é assinada")
    if (curador.length > Regras.TAMANHO_MAXIMO_CURADOR) add("curador com mais de ${Regras.TAMANHO_MAXIMO_CURADOR} caracteres")
}
