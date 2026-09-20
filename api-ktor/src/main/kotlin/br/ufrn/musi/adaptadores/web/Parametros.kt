package br.ufrn.musi.adaptadores.web

import br.ufrn.musi.aplicacao.PedidoDePagina
import br.ufrn.musi.dominio.Ordenacao
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException

/**
 * Leitura dos parâmetros de caminho e de consulta.
 *
 * Tipo errado (`?pagina=abc`, `/anotacoes/xyz`) é requisição malformada: `400`. Valor
 * bem formado que viola regra (`?tamanho=500`) é `422`, decidido pela aplicação.
 */
fun ApplicationCall.inteiro(nome: String): Int? =
    request.queryParameters[nome]?.let { it.toIntOrNull() ?: throw BadRequestException("`$nome` deve ser um inteiro: `$it`") }

fun ApplicationCall.texto(nome: String): String? = request.queryParameters[nome]?.takeIf { it.isNotBlank() }

fun ApplicationCall.caminho(nome: String): String = parameters[nome]!!

fun ApplicationCall.caminhoLong(nome: String): Long =
    parameters[nome]!!.let { it.toLongOrNull() ?: throw BadRequestException("`$nome` deve ser um inteiro: `$it`") }

fun ApplicationCall.pedidoDePagina() =
    PedidoDePagina(inteiro("pagina") ?: 0, inteiro("tamanho") ?: PedidoDePagina.PADRAO)

/**
 * `?ordem=` com os valores de `Ordenacao`, em minúsculas e com hífen. Sem padrão oculto de
 * relevância: a ordenação é sempre declarada por quem consulta (ADR-0002).
 */
fun ApplicationCall.ordenacao(): Ordenacao {
    val valor = texto("ordem") ?: return Ordenacao.TITULO
    return Ordenacao.entries.firstOrNull { it.paraParametro() == valor }
        ?: throw BadRequestException("`ordem` deve ser um de ${Ordenacao.entries.map { it.paraParametro() }}: `$valor`")
}

fun Ordenacao.paraParametro() = name.lowercase().replace('_', '-')
