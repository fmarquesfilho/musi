package br.ufrn.musi.adaptadores.persistencia

import br.ufrn.musi.aplicacao.DadosDaAnotacao
import br.ufrn.musi.aplicacao.DadosDaObra
import br.ufrn.musi.aplicacao.FiltroDeAnotacoes
import br.ufrn.musi.aplicacao.FiltroDeObras
import br.ufrn.musi.aplicacao.PedidoDePagina
import br.ufrn.musi.aplicacao.PersistenciaIndisponivel
import br.ufrn.musi.aplicacao.RepositorioDeAnotacoes
import br.ufrn.musi.aplicacao.RepositorioDeObras
import br.ufrn.musi.dominio.Ordenacao

/**
 * As portas quando não há banco (sem `DB_URL`): o deploy no Render até a Sprint 3 — ADR-0004.
 *
 * Toda operação lança `PersistenciaIndisponivel`, que a borda responde como `503`. As rotas
 * existem e estão no OpenAPI; só não há onde gravar.
 */
object ObrasSemBanco : RepositorioDeObras {
    override suspend fun listar(filtro: FiltroDeObras, ordenacao: Ordenacao, pedido: PedidoDePagina) = indisponivel()
    override suspend fun buscar(id: String) = indisponivel()
    override suspend fun existe(id: String) = indisponivel()
    override suspend fun criar(dados: DadosDaObra) = indisponivel()
    override suspend fun atualizar(id: String, dados: DadosDaObra) = indisponivel()
    override suspend fun remover(id: String) = indisponivel()
}

object AnotacoesSemBanco : RepositorioDeAnotacoes {
    override suspend fun listar(obraId: String, filtro: FiltroDeAnotacoes, pedido: PedidoDePagina) = indisponivel()
    override suspend fun buscar(obraId: String, id: Long) = indisponivel()
    override suspend fun criar(obraId: String, dados: DadosDaAnotacao) = indisponivel()
    override suspend fun atualizar(obraId: String, id: Long, dados: DadosDaAnotacao) = indisponivel()
    override suspend fun remover(obraId: String, id: Long) = indisponivel()
}

private fun indisponivel(): Nothing = throw PersistenciaIndisponivel()
