package br.ufrn.musi.adaptadores.persistencia

import br.ufrn.musi.aplicacao.Conflito
import br.ufrn.musi.aplicacao.DadosDaObra
import br.ufrn.musi.aplicacao.FiltroDeObras
import br.ufrn.musi.aplicacao.Pagina
import br.ufrn.musi.aplicacao.PedidoDePagina
import br.ufrn.musi.aplicacao.RepositorioDeObras
import br.ufrn.musi.dominio.Faceta
import br.ufrn.musi.dominio.Obra
import br.ufrn.musi.dominio.Ordenacao
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.exists
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

/**
 * A porta `RepositorioDeObras` sobre PostgreSQL, com o DSL do Exposed.
 *
 * Toda operação roda em `suspendTransaction`: a escrita da obra e das facetas dela é
 * atômica, e a corrotina não prende a thread enquanto espera o banco.
 */
class ObrasPostgres(private val db: Database) : RepositorioDeObras {

    override suspend fun listar(filtro: FiltroDeObras, ordenacao: Ordenacao, pedido: PedidoDePagina): Pagina<Obra> =
        suspendTransaction(db) {
            val onde = condicoes(filtro)
            val total = Obras.selectAll().where(onde).count()

            // A fatia é do SQL: ORDER BY ... LIMIT ... OFFSET. O `id` desempata, para que a
            // mesma obra não apareça em duas páginas quando os títulos se repetem.
            val linhas = Obras.selectAll().where(onde)
                .orderBy(ordem(ordenacao), Obras.id to SortOrder.ASC)
                .limit(pedido.tamanho).offset(pedido.deslocamento)
                .toList()

            // Facetas da página inteira numa consulta só, e não uma por obra (N+1).
            val facetas = facetasDe(linhas.map { it[Obras.id] })
            Pagina(linhas.map { it.paraObra(facetas[it[Obras.id]].orEmpty()) }, pedido, total)
        }

    override suspend fun buscar(id: String): Obra? = suspendTransaction(db) {
        Obras.selectAll().where { Obras.id eq id }.singleOrNull()
            ?.paraObra(facetasDe(listOf(id))[id].orEmpty())
    }

    override suspend fun existe(id: String): Boolean = suspendTransaction(db) {
        Obras.select(Obras.id).where { Obras.id eq id }.any()
    }

    override suspend fun criar(dados: DadosDaObra): Obra = traduzindoConflito {
        val id = UUID.randomUUID().toString()
        suspendTransaction(db) {
            Obras.insert {
                it[Obras.id] = id
                it[titulo] = dados.titulo
                it[artista] = dados.artista
                it[ano] = dados.ano
                it[mbid] = dados.mbid?.let(UUID::fromString)
                it[mbidComposicao] = dados.mbidComposicao?.let(UUID::fromString)
            }
            inserirFacetas(id, dados.facetas)
        }
        dados.paraObra(id)
    }

    override suspend fun atualizar(id: String, dados: DadosDaObra): Obra? = traduzindoConflito {
        suspendTransaction(db) {
            val alteradas = Obras.update({ Obras.id eq id }) {
                it[titulo] = dados.titulo
                it[artista] = dados.artista
                it[ano] = dados.ano
                it[mbid] = dados.mbid?.let(UUID::fromString)
                it[mbidComposicao] = dados.mbidComposicao?.let(UUID::fromString)
            }
            if (alteradas == 0) return@suspendTransaction null

            // PUT substitui a obra inteira, facetas incluídas.
            Facetas.deleteWhere { Facetas.obraId eq id }
            inserirFacetas(id, dados.facetas)
            dados.paraObra(id)
        }
    }

    // As anotações vão junto pelo ON DELETE CASCADE da chave estrangeira.
    override suspend fun remover(id: String): Boolean = suspendTransaction(db) {
        Obras.deleteWhere { Obras.id eq id } > 0
    }

    private fun condicoes(f: FiltroDeObras): Op<Boolean> {
        var onde: Op<Boolean> = Op.TRUE
        f.artista?.let { onde = onde and (Obras.artista.lowerCase() like "%${escaparLike(it.lowercase())}%") }
        f.anoDe?.let { onde = onde and (Obras.ano greaterEq it) }
        f.anoAte?.let { onde = onde and (Obras.ano lessEq it) }
        // "Tem a faceta": EXISTS numa subconsulta, porque faceta é linha, não coluna (ADR-0002).
        f.faceta?.let { faceta ->
            onde = onde and exists(
                Facetas.select(Facetas.obraId).where {
                    (Facetas.obraId eq Obras.id) and
                        (Facetas.dimensao eq faceta.dimensao) and (Facetas.valor eq faceta.valor)
                },
            )
        }
        return onde
    }

    private fun ordem(o: Ordenacao) = when (o) {
        Ordenacao.TITULO          -> Obras.titulo to SortOrder.ASC
        Ordenacao.ARTISTA         -> Obras.artista to SortOrder.ASC
        Ordenacao.ANO_CRESCENTE   -> Obras.ano to SortOrder.ASC
        Ordenacao.ANO_DECRESCENTE -> Obras.ano to SortOrder.DESC
    }

    private fun facetasDe(ids: List<String>): Map<String, List<Faceta>> =
        if (ids.isEmpty()) emptyMap()
        else Facetas.selectAll().where { Facetas.obraId inList ids }
            .orderBy(Facetas.posicao to SortOrder.ASC)
            .groupBy({ it[Facetas.obraId] }, { Faceta(it[Facetas.dimensao], it[Facetas.valor]) })

    private fun inserirFacetas(obraId: String, facetas: List<Faceta>) {
        Facetas.batchInsert(facetas.withIndex()) { (i, f) ->
            this[Facetas.obraId] = obraId
            this[Facetas.posicao] = i
            this[Facetas.dimensao] = f.dimensao
            this[Facetas.valor] = f.valor
        }
    }

    private fun ResultRow.paraObra(facetas: List<Faceta>) = Obra(
        id = this[Obras.id],
        titulo = this[Obras.titulo],
        artista = this[Obras.artista],
        ano = this[Obras.ano],
        facetas = facetas,
        mbid = this[Obras.mbid]?.toString(),
        mbidComposicao = this[Obras.mbidComposicao]?.toString(),
    )

    private fun DadosDaObra.paraObra(id: String) =
        Obra(id, titulo, artista, ano, facetas, mbid, mbidComposicao)
}

/** `%` e `_` digitados por quem busca são texto, não curinga. */
internal fun escaparLike(texto: String) =
    texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

/**
 * Violação de `UNIQUE` (SQLSTATE 23505) vira `Conflito`, com o nome da restrição.
 *
 * A regra "MBID não se repete" mora no banco, e não num `SELECT` antes do `INSERT`: duas
 * requisições simultâneas passariam as duas pela consulta.
 */
internal suspend fun <T> traduzindoConflito(bloco: suspend () -> T): T =
    try {
        bloco()
    } catch (e: ExposedSQLException) {
        if (e.sqlState != "23505") throw e
        val restricao: String? = (e.cause as? org.postgresql.util.PSQLException)?.serverErrorMessage?.constraint
        throw Conflito(
            when (restricao) {
                "obras_mbid_key"  -> "já existe obra com este mbid (duplicata, ADR-0003)"
                "anotacao_unica"  -> "este curador já anotou esta faceta nesta obra"
                else              -> "violação de unicidade${restricao?.let { " ($it)" } ?: ""}"
            },
        )
    }
