package br.ufrn.musi.aplicacao

import br.ufrn.musi.dominio.Anotacao
import br.ufrn.musi.dominio.violacoesDoCurador
import br.ufrn.musi.dominio.violacoes

/**
 * Casos de uso das anotações, sempre dentro de uma obra (`/obras/{id}/anotacoes`).
 *
 * Duas regras além da forma:
 * - anotar obra inexistente é `NaoEncontrado` da OBRA, não da anotação;
 * - o mesmo curador não repete a mesma faceta na mesma obra (`Conflito`). Curadores
 *   diferentes podem divergir à vontade — é o que o modelo prevê (docs/DOMINIO.md).
 */
class AnotacoesDeObras(
    private val obras: RepositorioDeObras,
    private val anotacoes: RepositorioDeAnotacoes,
) {

    suspend fun listar(obraId: String, filtro: FiltroDeAnotacoes, pedido: PedidoDePagina): Pagina<Anotacao> {
        exigirObra(obraId)
        return anotacoes.listar(obraId, filtro, pedido)
    }

    suspend fun buscar(obraId: String, id: Long): Anotacao {
        exigirObra(obraId)
        return anotacoes.buscar(obraId, id) ?: throw NaoEncontrado("anotação", id)
    }

    suspend fun criar(obraId: String, dados: DadosDaAnotacao): Anotacao {
        validar(dados)
        exigirObra(obraId)
        return anotacoes.criar(obraId, dados)
    }

    suspend fun atualizar(obraId: String, id: Long, dados: DadosDaAnotacao): Anotacao {
        validar(dados)
        exigirObra(obraId)
        return anotacoes.atualizar(obraId, id, dados) ?: throw NaoEncontrado("anotação", id)
    }

    suspend fun remover(obraId: String, id: Long) {
        exigirObra(obraId)
        if (!anotacoes.remover(obraId, id)) throw NaoEncontrado("anotação", id)
    }

    private suspend fun exigirObra(obraId: String) {
        if (!obras.existe(obraId)) throw NaoEncontrado("obra", obraId)
    }

    private fun validar(d: DadosDaAnotacao) {
        val violacoes = d.faceta.violacoes() + violacoesDoCurador(d.curador)
        if (violacoes.isNotEmpty()) throw EntradaInvalida(violacoes)
    }
}
