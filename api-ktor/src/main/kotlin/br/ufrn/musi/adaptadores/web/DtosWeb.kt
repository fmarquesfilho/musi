package br.ufrn.musi.adaptadores.web

import br.ufrn.musi.adaptadores.busca.FacetaDto
import br.ufrn.musi.adaptadores.busca.ObraDto
import br.ufrn.musi.adaptadores.busca.paraDto
import br.ufrn.musi.aplicacao.DadosDaAnotacao
import br.ufrn.musi.aplicacao.DadosDaObra
import br.ufrn.musi.aplicacao.Pagina
import br.ufrn.musi.dominio.Anotacao
import br.ufrn.musi.dominio.Faceta
import br.ufrn.musi.dominio.Obra
import kotlinx.serialization.Serializable

/**
 * DTOs do CRUD. Os de leitura de obra (`ObraDto`, `FacetaDto`) são os mesmos da busca:
 * uma obra tem a mesma forma venha do banco ou do serviço Go (contratos/obra.schema.json).
 *
 * Os de entrada não têm `id` nem data: são do servidor.
 */
@Serializable
data class NovaObraDto(
    val titulo: String,
    val artista: String,
    val ano: Int,
    val facetas: List<FacetaDto> = emptyList(),
    val mbid: String? = null,
    val mbidComposicao: String? = null,
)

@Serializable
data class NovaAnotacaoDto(val faceta: FacetaDto, val curador: String)

@Serializable
data class AnotacaoDto(
    val id: Long,
    val obraId: String,
    val faceta: FacetaDto,
    val curador: String,
    val criadoEm: String,   // ISO-8601, em UTC
)

/** Uma página da listagem. `total` permite calcular quantas páginas existem. */
@Serializable
data class PaginaDeObrasDto(val itens: List<ObraDto>, val pagina: Int, val tamanho: Int, val total: Long)

@Serializable
data class PaginaDeAnotacoesDto(val itens: List<AnotacaoDto>, val pagina: Int, val tamanho: Int, val total: Long)

fun FacetaDto.paraDominio() = Faceta(dimensao, valor)

fun NovaObraDto.paraDados() =
    DadosDaObra(titulo, artista, ano, facetas.map { it.paraDominio() }, mbid, mbidComposicao)

fun NovaAnotacaoDto.paraDados() = DadosDaAnotacao(faceta.paraDominio(), curador)

fun Anotacao.paraDto() =
    AnotacaoDto(id, obraId, FacetaDto(faceta.dimensao, faceta.valor), curador, criadoEm.toString())

fun Pagina<Obra>.paraDto() =
    PaginaDeObrasDto(itens.map { it.paraDto() }, pedido.pagina, pedido.tamanho, total)

@JvmName("paginaDeAnotacoesParaDto")
fun Pagina<Anotacao>.paraDto() =
    PaginaDeAnotacoesDto(itens.map { it.paraDto() }, pedido.pagina, pedido.tamanho, total)
