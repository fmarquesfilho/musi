package br.ufrn.musi.adaptadores.web

import br.ufrn.musi.adaptadores.busca.FiltroDto
import br.ufrn.musi.adaptadores.busca.paraDominio
import br.ufrn.musi.adaptadores.busca.paraDto
import br.ufrn.musi.aplicacao.BuscarObras
import br.ufrn.musi.dominio.Filtro
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.ExperimentalKtorApi
import org.koin.ktor.ext.inject

/**
 * A busca por árvore de filtro, delegada ao serviço Go (`BuscaHttp`). Não passa pelo
 * banco: o Go avalia a árvore sobre o acervo dele (ADR-0004 explica a convivência).
 */
@OptIn(ExperimentalKtorApi::class)
fun Route.rotasDeBusca() {
    val buscarObras by inject<BuscarObras>()   // Koin

    route("/busca") {

        // Busca simples: uma faceta, pela query string. Cacheável: Cache-Control em Aplicacao.kt.
        get {
            val dimensao = call.texto("dimensao")
            val valor = call.texto("valor")
            if (dimensao == null || valor == null) {
                return@get call.responderProblema(
                    HttpStatusCode.BadRequest, "parametro-ausente", "Parâmetros obrigatórios ausentes",
                    "Informe `dimensao` e `valor`.",
                )
            }
            call.respond(buscarObras(Filtro.Tem(dimensao, valor)).map { it.paraDto() })
        }.describe(Doc.buscaSimples)

        // Busca composta: a árvore de filtro chega no corpo.
        //
        // É POST porque a árvore não cabe confortavelmente numa query string — e essa
        // escolha custa o cache: respostas de POST não são cacheáveis por padrão.
        post {
            val filtro = call.receive<FiltroDto>().paraDominio()
            call.respond(buscarObras(filtro).map { it.paraDto() })
        }.describe(Doc.buscaComposta)
    }
}
