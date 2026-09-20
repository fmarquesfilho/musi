package br.ufrn.musi.adaptadores.persistencia

import br.ufrn.musi.aplicacao.DadosDaAnotacao
import br.ufrn.musi.aplicacao.FiltroDeAnotacoes
import br.ufrn.musi.aplicacao.Pagina
import br.ufrn.musi.aplicacao.PedidoDePagina
import br.ufrn.musi.aplicacao.RepositorioDeAnotacoes
import br.ufrn.musi.dominio.Anotacao
import br.ufrn.musi.dominio.Faceta
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.toKotlinInstant

/** A porta `RepositorioDeAnotacoes` sobre PostgreSQL. Toda consulta é restrita à obra. */
class AnotacoesPostgres(private val db: Database) : RepositorioDeAnotacoes {

    override suspend fun listar(obraId: String, filtro: FiltroDeAnotacoes, pedido: PedidoDePagina): Pagina<Anotacao> =
        suspendTransaction(db) {
            var onde: Op<Boolean> = Anotacoes.obraId eq obraId
            filtro.curador?.let { onde = onde and (Anotacoes.curador eq it) }
            filtro.dimensao?.let { onde = onde and (Anotacoes.dimensao eq it) }

            val total = Anotacoes.selectAll().where(onde).count()
            val itens = Anotacoes.selectAll().where(onde)
                .orderBy(Anotacoes.criadoEm to SortOrder.ASC, Anotacoes.id to SortOrder.ASC)
                .limit(pedido.tamanho).offset(pedido.deslocamento)
                .map { it.paraAnotacao() }
            Pagina(itens, pedido, total)
        }

    override suspend fun buscar(obraId: String, id: Long): Anotacao? = suspendTransaction(db) {
        Anotacoes.selectAll().where { (Anotacoes.id eq id) and (Anotacoes.obraId eq obraId) }
            .singleOrNull()?.paraAnotacao()
    }

    // A data é do banco (DEFAULT now()); RETURNING devolve a linha como ficou gravada.
    override suspend fun criar(obraId: String, dados: DadosDaAnotacao): Anotacao = traduzindoConflito {
        suspendTransaction(db) {
            val id = Anotacoes.insert {
                it[Anotacoes.obraId] = obraId
                it[dimensao] = dados.faceta.dimensao
                it[valor] = dados.faceta.valor
                it[curador] = dados.curador
            } get Anotacoes.id
            Anotacoes.selectAll().where { Anotacoes.id eq id }.single().paraAnotacao()
        }
    }

    override suspend fun atualizar(obraId: String, id: Long, dados: DadosDaAnotacao): Anotacao? = traduzindoConflito {
        suspendTransaction(db) {
            val alteradas = Anotacoes.update({ (Anotacoes.id eq id) and (Anotacoes.obraId eq obraId) }) {
                it[dimensao] = dados.faceta.dimensao
                it[valor] = dados.faceta.valor
                it[curador] = dados.curador
            }
            if (alteradas == 0) null
            else Anotacoes.selectAll().where { Anotacoes.id eq id }.single().paraAnotacao()
        }
    }

    override suspend fun remover(obraId: String, id: Long): Boolean = suspendTransaction(db) {
        Anotacoes.deleteWhere { (Anotacoes.id eq id) and (Anotacoes.obraId eq obraId) } > 0
    }

    private fun ResultRow.paraAnotacao() = Anotacao(
        id = this[Anotacoes.id],
        obraId = this[Anotacoes.obraId],
        faceta = Faceta(this[Anotacoes.dimensao], this[Anotacoes.valor]),
        curador = this[Anotacoes.curador],
        criadoEm = this[Anotacoes.criadoEm].toInstant().toKotlinInstant(),
    )
}
