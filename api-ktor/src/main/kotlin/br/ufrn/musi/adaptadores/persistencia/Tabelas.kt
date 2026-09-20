package br.ufrn.musi.adaptadores.persistencia

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

/**
 * O mapeamento das tabelas para o Exposed. O esquema em si é da migração
 * (V1__cria_obras_e_anotacoes.sql); estes objetos só dizem ao Exposed como ler e escrever.
 */
object Obras : Table("obras") {
    val id = varchar("id", 64)
    val titulo = varchar("titulo", 300)
    val artista = varchar("artista", 300)
    val ano = integer("ano")
    val mbid = javaUUID("mbid").nullable()   // java.util.UUID; `uuid()` seria kotlin.uuid.Uuid
    val mbidComposicao = javaUUID("mbid_composicao").nullable()
    override val primaryKey = PrimaryKey(id)
}

object Facetas : Table("facetas") {
    val obraId = varchar("obra_id", 64).references(Obras.id, onDelete = ReferenceOption.CASCADE)
    val posicao = integer("posicao")
    val dimensao = varchar("dimensao", 60)
    val valor = varchar("valor", 60)
    override val primaryKey = PrimaryKey(obraId, dimensao, valor)
}

object Anotacoes : Table("anotacoes") {
    val id = long("id").autoIncrement()
    val obraId = varchar("obra_id", 64).references(Obras.id, onDelete = ReferenceOption.CASCADE)
    val dimensao = varchar("dimensao", 60)
    val valor = varchar("valor", 60)
    val curador = varchar("curador", 120)
    val criadoEm = timestampWithTimeZone("criado_em")
    override val primaryKey = PrimaryKey(id)
}
