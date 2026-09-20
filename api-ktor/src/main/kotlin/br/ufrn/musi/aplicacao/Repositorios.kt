package br.ufrn.musi.aplicacao

import br.ufrn.musi.dominio.Anotacao
import br.ufrn.musi.dominio.Faceta
import br.ufrn.musi.dominio.Obra
import br.ufrn.musi.dominio.Ordenacao

/**
 * As portas de persistência. Como `FonteDeObras`, pertencem a quem as usa: nenhuma
 * menção a SQL, Exposed ou transação. As operações têm o nome do que o domínio precisa,
 * não de uma tabela (`salvarLinha`, `executarQuery`).
 *
 * `suspend`: a implementação com banco faz E/S e não pode travar a thread do servidor.
 */
interface RepositorioDeObras {
    suspend fun listar(filtro: FiltroDeObras, ordenacao: Ordenacao, pedido: PedidoDePagina): Pagina<Obra>
    suspend fun buscar(id: String): Obra?
    suspend fun existe(id: String): Boolean
    suspend fun criar(dados: DadosDaObra): Obra
    /** Devolve `null` se a obra não existe. */
    suspend fun atualizar(id: String, dados: DadosDaObra): Obra?
    /** Devolve `false` se a obra não existe. Remove também as anotações dela. */
    suspend fun remover(id: String): Boolean
}

interface RepositorioDeAnotacoes {
    suspend fun listar(obraId: String, filtro: FiltroDeAnotacoes, pedido: PedidoDePagina): Pagina<Anotacao>
    suspend fun buscar(obraId: String, id: Long): Anotacao?
    suspend fun criar(obraId: String, dados: DadosDaAnotacao): Anotacao
    suspend fun atualizar(obraId: String, id: Long, dados: DadosDaAnotacao): Anotacao?
    suspend fun remover(obraId: String, id: Long): Boolean
}

/** O que se informa para criar ou substituir uma obra. A identidade é do servidor. */
data class DadosDaObra(
    val titulo: String,
    val artista: String,
    val ano: Int,
    val facetas: List<Faceta>,
    val mbid: String? = null,
    val mbidComposicao: String? = null,
)

/** O que se informa numa anotação. A data é do servidor. */
data class DadosDaAnotacao(val faceta: Faceta, val curador: String)

/**
 * Filtros da listagem de obras. Todos opcionais e combinados com E.
 *
 * Não confundir com `Filtro`, a árvore da busca (ADR-0002), que o serviço Go avalia.
 * Aqui são critérios de listagem, traduzidos para `WHERE`.
 */
data class FiltroDeObras(
    val artista: String? = null,     // contém, sem diferenciar maiúsculas
    val anoDe: Int? = null,
    val anoAte: Int? = null,
    val faceta: Faceta? = null,      // tem esta faceta
)

data class FiltroDeAnotacoes(
    val curador: String? = null,
    val dimensao: String? = null,
)
