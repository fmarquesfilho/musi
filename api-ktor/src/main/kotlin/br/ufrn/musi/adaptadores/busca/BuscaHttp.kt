package br.ufrn.musi.adaptadores.busca

import br.ufrn.musi.aplicacao.FonteDeObras
import br.ufrn.musi.dominio.Filtro
import br.ufrn.musi.dominio.Obra
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Adaptador que implementa a porta falando com o serviço Go.
 *
 * Usa o **Ktor Client** — a mesma biblioteca que o app usa para falar com esta api.
 * Quem cursa as duas disciplinas aprende uma biblioteca HTTP, não duas.
 */
class BuscaHttp(
    private val cliente: HttpClient,
    private val urlBase: String,
) : FonteDeObras {

    override suspend fun buscar(filtro: Filtro): List<Obra> =
        cliente.post("$urlBase/buscar") {
            contentType(ContentType.Application.Json)
            setBody(filtro.paraDto())
        }.body<List<ObraDto>>().map { it.paraDominio() }
}
