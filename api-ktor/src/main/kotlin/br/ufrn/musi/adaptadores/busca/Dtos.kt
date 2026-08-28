package br.ufrn.musi.adaptadores.busca

import br.ufrn.musi.dominio.*
import kotlinx.serialization.Serializable

/**
 * DTOs da fronteira. Espelham contratos/filtro.schema.json.
 *
 * Por que não usar `Filtro` do domínio direto na serialização? Porque isso faria o
 * formato JSON público virar refém da modelagem interna, e vice-versa: renomear um
 * campo do domínio quebraria todos os clientes.
 */
@Serializable
data class FiltroDto(
    val tipo: String,
    val dimensao: String? = null,
    val valor: String? = null,
    val opcoes: List<FiltroDto>? = null,
    val exigencias: List<FiltroDto>? = null,
    val filtro: FiltroDto? = null,
    val ano: Int? = null,
)

@Serializable
data class FacetaDto(val dimensao: String, val valor: String)

@Serializable
data class ObraDto(
    val id: String,
    val titulo: String,
    val artista: String,
    val ano: Int,
    val facetas: List<FacetaDto>,
    val mbid: String? = null,             // ADR-0003: identidade global, ausente até conciliar
    val mbidComposicao: String? = null,
)

fun ObraDto.paraDominio() =
    Obra(id, titulo, artista, ano, facetas.map { Faceta(it.dimensao, it.valor) }, mbid, mbidComposicao)

fun Obra.paraDto() =
    ObraDto(id, titulo, artista, ano, facetas.map { FacetaDto(it.dimensao, it.valor) }, mbid, mbidComposicao)

/**
 * Domínio → DTO.
 *
 * O `when` é exaustivo sobre a interface selada: acrescentar um construtor de Filtro
 * faz esta função parar de compilar, e o compilador aponta onde falta tratar.
 * Ver ADR-0002.
 */
fun Filtro.paraDto(): FiltroDto = when (this) {
    is Filtro.Tem    -> FiltroDto("tem", dimensao = dimensao, valor = valor)
    is Filtro.Ou     -> FiltroDto("ou", opcoes = opcoes.map { it.paraDto() })
    is Filtro.E      -> FiltroDto("e", exigencias = exigencias.map { it.paraDto() })
    is Filtro.Exceto -> FiltroDto("exceto", filtro = filtro.paraDto())
    is Filtro.Ate    -> FiltroDto("ate", ano = ano)
}

fun FiltroDto.paraDominio(): Filtro = when (tipo) {
    "tem"    -> Filtro.Tem(requireNotNull(dimensao), requireNotNull(valor))
    "ou"     -> Filtro.Ou(opcoes.orEmpty().map { it.paraDominio() })
    "e"      -> Filtro.E(exigencias.orEmpty().map { it.paraDominio() })
    "exceto" -> Filtro.Exceto(requireNotNull(filtro).paraDominio())
    "ate"    -> Filtro.Ate(requireNotNull(ano))
    else     -> throw IllegalArgumentException("tipo de filtro desconhecido: $tipo")
}
