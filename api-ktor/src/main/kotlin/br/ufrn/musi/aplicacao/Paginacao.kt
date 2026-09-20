package br.ufrn.musi.aplicacao

/**
 * Qual fatia da listagem se quer. `pagina` começa em 0.
 *
 * O tamanho tem teto: sem ele, `?tamanho=1000000` traria o acervo inteiro numa resposta.
 * A fatia vai para o SQL (`LIMIT`/`OFFSET`); nada é paginado em memória.
 */
data class PedidoDePagina(val pagina: Int = 0, val tamanho: Int = PADRAO) {
    init {
        if (pagina < 0) throw EntradaInvalida(listOf("pagina deve ser >= 0"))
        if (tamanho !in 1..MAXIMO) throw EntradaInvalida(listOf("tamanho deve estar entre 1 e $MAXIMO"))
    }

    val deslocamento: Long get() = pagina.toLong() * tamanho

    companion object {
        const val PADRAO = 20
        const val MAXIMO = 100
    }
}

/** Uma fatia da listagem e o total, para quem consome saber quantas páginas há. */
data class Pagina<T>(val itens: List<T>, val pedido: PedidoDePagina, val total: Long)
