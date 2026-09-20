package br.ufrn.musi.adaptadores.web

import br.ufrn.musi.adaptadores.busca.paraDto
import br.ufrn.musi.aplicacao.AnotacoesDeObras
import br.ufrn.musi.aplicacao.CatalogoDeObras
import br.ufrn.musi.aplicacao.FiltroDeAnotacoes
import br.ufrn.musi.aplicacao.FiltroDeObras
import br.ufrn.musi.dominio.Faceta
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.utils.io.ExperimentalKtorApi
import org.koin.ktor.ext.inject

/**
 * O CRUD de obras e, aninhado, o das anotações de cada obra (relação 1:N).
 *
 * | Método | Caminho                               | Sucesso          |
 * |--------|---------------------------------------|------------------|
 * | GET    | /obras                                | 200, paginado    |
 * | POST   | /obras                                | 201 + Location   |
 * | GET    | /obras/{id}                           | 200              |
 * | PUT    | /obras/{id}                           | 200              |
 * | DELETE | /obras/{id}                           | 204              |
 * | GET    | /obras/{id}/anotacoes                 | 200, paginado    |
 * | POST   | /obras/{id}/anotacoes                 | 201 + Location   |
 * | GET    | /obras/{id}/anotacoes/{anotacaoId}    | 200              |
 * | PUT    | /obras/{id}/anotacoes/{anotacaoId}    | 200              |
 * | DELETE | /obras/{id}/anotacoes/{anotacaoId}    | 204              |
 *
 * Os erros (400, 404, 409, 422, 503) saem do StatusPages (Erros.kt), não daqui.
 */
@OptIn(ExperimentalKtorApi::class)
fun Route.rotasDeObras() {
    val catalogo by inject<CatalogoDeObras>()
    val anotacoes by inject<AnotacoesDeObras>()

    route("/obras") {

        get {
            val filtro = FiltroDeObras(
                artista = call.texto("artista"),
                anoDe = call.inteiro("anoDe"),
                anoAte = call.inteiro("anoAte"),
                faceta = call.faceta(),
            )
            call.respond(catalogo.listar(filtro, call.ordenacao(), call.pedidoDePagina()).paraDto())
        }.describe(Doc.listarObras)

        post {
            val criada = catalogo.criar(call.receive<NovaObraDto>().paraDados())
            call.response.header(HttpHeaders.Location, "/obras/${criada.id}")
            call.respond(HttpStatusCode.Created, criada.paraDto())
        }.describe(Doc.criarObra)

        route("/{id}") {
            get { call.respond(catalogo.buscar(call.caminho("id")).paraDto()) }.describe(Doc.buscarObra)

            put {
                val dados = call.receive<NovaObraDto>().paraDados()
                call.respond(catalogo.atualizar(call.caminho("id"), dados).paraDto())
            }.describe(Doc.atualizarObra)

            delete {
                catalogo.remover(call.caminho("id"))
                call.respond(HttpStatusCode.NoContent)
            }.describe(Doc.removerObra)

            route("/anotacoes") {
                get {
                    val filtro = FiltroDeAnotacoes(curador = call.texto("curador"), dimensao = call.texto("dimensao"))
                    call.respond(anotacoes.listar(call.caminho("id"), filtro, call.pedidoDePagina()).paraDto())
                }.describe(Doc.listarAnotacoes)

                post {
                    val obraId = call.caminho("id")
                    val criada = anotacoes.criar(obraId, call.receive<NovaAnotacaoDto>().paraDados())
                    call.response.header(HttpHeaders.Location, "/obras/$obraId/anotacoes/${criada.id}")
                    call.respond(HttpStatusCode.Created, criada.paraDto())
                }.describe(Doc.criarAnotacao)

                route("/{anotacaoId}") {
                    get {
                        call.respond(anotacoes.buscar(call.caminho("id"), call.caminhoLong("anotacaoId")).paraDto())
                    }.describe(Doc.buscarAnotacao)

                    put {
                        val dados = call.receive<NovaAnotacaoDto>().paraDados()
                        call.respond(anotacoes.atualizar(call.caminho("id"), call.caminhoLong("anotacaoId"), dados).paraDto())
                    }.describe(Doc.atualizarAnotacao)

                    delete {
                        anotacoes.remover(call.caminho("id"), call.caminhoLong("anotacaoId"))
                        call.respond(HttpStatusCode.NoContent)
                    }.describe(Doc.removerAnotacao)
                }
            }
        }
    }
}

/** `?dimensao=&valor=` filtra por faceta; os dois vêm juntos ou nenhum. */
private fun io.ktor.server.application.ApplicationCall.faceta(): Faceta? {
    val dimensao = texto("dimensao")
    val valor = texto("valor")
    if ((dimensao == null) != (valor == null)) throw BadRequestException("informe `dimensao` e `valor` juntos")
    return if (dimensao != null && valor != null) Faceta(dimensao, valor) else null
}
