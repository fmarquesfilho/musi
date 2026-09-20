package br.ufrn.musi.aplicacao

import br.ufrn.musi.dominio.Obra
import br.ufrn.musi.dominio.Ordenacao
import br.ufrn.musi.dominio.violacoesDaObra

/**
 * Casos de uso do CRUD de obras.
 *
 * Não é uma camada que só repassa: valida a entrada com as regras do domínio antes de
 * chegar ao banco, e transforma "não achei" em erro com nome. O banco continua com as
 * restrições dele (`CHECK`, `UNIQUE`, chave estrangeira) como última barreira.
 */
class CatalogoDeObras(private val obras: RepositorioDeObras) {

    suspend fun listar(filtro: FiltroDeObras, ordenacao: Ordenacao, pedido: PedidoDePagina): Pagina<Obra> {
        if (filtro.anoDe != null && filtro.anoAte != null && filtro.anoDe > filtro.anoAte)
            throw EntradaInvalida(listOf("anoDe maior que anoAte"))
        return obras.listar(filtro, ordenacao, pedido)
    }

    suspend fun buscar(id: String): Obra = obras.buscar(id) ?: throw NaoEncontrado("obra", id)

    suspend fun criar(dados: DadosDaObra): Obra {
        validar(dados)
        return obras.criar(dados)
    }

    suspend fun atualizar(id: String, dados: DadosDaObra): Obra {
        validar(dados)
        return obras.atualizar(id, dados) ?: throw NaoEncontrado("obra", id)
    }

    suspend fun remover(id: String) {
        if (!obras.remover(id)) throw NaoEncontrado("obra", id)
    }

    private fun validar(d: DadosDaObra) {
        val violacoes = violacoesDaObra(d.titulo, d.artista, d.ano, d.facetas) +
            listOfNotNull(d.mbid, d.mbidComposicao).filterNot { MBID.matches(it) }.map { "MBID `$it` não é um UUID" }
        if (violacoes.isNotEmpty()) throw EntradaInvalida(violacoes)
    }

    private companion object {
        val MBID = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
    }
}
