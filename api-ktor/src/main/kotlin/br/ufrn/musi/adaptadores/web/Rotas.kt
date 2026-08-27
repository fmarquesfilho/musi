package br.ufrn.musi.adaptadores.web

import br.ufrn.musi.adaptadores.busca.FiltroDto
import br.ufrn.musi.adaptadores.busca.ObraDto
import br.ufrn.musi.adaptadores.busca.paraDominio
import br.ufrn.musi.adaptadores.busca.paraDto
import br.ufrn.musi.aplicacao.BuscarObras
import br.ufrn.musi.dominio.Filtro
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

/**
 * As rotas são CÓDIGO, não anotações.
 *
 * A árvore de rotas inteira cabe numa tela e se lê de cima para baixo, sem procurar
 * anotações espalhadas por várias classes.
 *
 * O `get`/`post` aqui vêm de `io.github.smiley4.ktoropenapi`, não do Ktor: estendem o
 * mesmo DSL com um bloco de documentação opcional. O OpenAPI sai daí, gerado — o spec
 * não é um arquivo à parte que pode divergir do código. Compare com o lado Quarkus, onde
 * o mesmo efeito vem de anotações. Ver a instalação do plugin em Aplicacao.kt.
 *
 * O controller é um adaptador: traduz HTTP para chamadas de caso de uso. Se você
 * encontrar aqui um `if` que decide algo sobre obras, ele está no lugar errado.
 */
fun Application.rotas() {
    val buscarObras by inject<BuscarObras>()   // Koin

    routing {

        // Spec gerado e o Swagger UI. Estas duas rotas não entram no próprio spec.
        //   spec:       http://localhost:8080/openapi.json
        //   Swagger UI: http://localhost:8080/swagger
        route("openapi.json") { openApi() }
        route("swagger") { swaggerUI("/openapi.json") }

        get("/health", {
            summary = "Verificação de saúde"
            response { code(HttpStatusCode.OK) { description = "No ar" } }
        }) {
            call.respond(mapOf("status" to "UP"))
        }

        route("/obras") {

            // Busca simples: uma faceta, pela query string.
            get({
                summary = "Busca simples por uma faceta"
                description = "Filtro `Tem(dimensao, valor)` montado a partir da query " +
                    "string. Para buscas compostas (E, OU, EXCETO, ATÉ), use POST /obras."
                request {
                    queryParameter<String>("dimensao") {
                        description = "A dimensão da faceta, ex.: `ritmo`."
                        required = true
                    }
                    queryParameter<String>("valor") {
                        description = "O valor da faceta, ex.: `baiao`."
                        required = true
                    }
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Obras que têm a faceta pedida"
                        body<List<ObraDto>>()
                    }
                    code(HttpStatusCode.BadRequest) {
                        description = "Falta `dimensao` ou `valor`"
                        body<Problema>()
                    }
                }
            }) {
                val dimensao = call.request.queryParameters["dimensao"]
                val valor = call.request.queryParameters["valor"]

                if (dimensao == null || valor == null) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        Problema(
                            type = "https://musi.ufrn.br/erros/parametro-ausente",
                            title = "Parâmetros obrigatórios ausentes",
                            status = 400,
                            detail = "Informe `dimensao` e `valor`.",
                        ),
                    )
                }

                // Cache-Control e ETag são acrescentados pelos plugins,
                // configurados em Aplicacao.kt.
                call.respond(buscarObras(Filtro.Tem(dimensao, valor)).map { it.paraDto() })
            }

            // Busca composta: a árvore de filtro chega no corpo.
            //
            // É POST porque a árvore não cabe confortavelmente numa query string — e
            // essa escolha custa o cache: respostas de POST não são cacheáveis por
            // padrão. Vale discutir a alternativa em aula.
            post({
                summary = "Busca composta pela árvore de filtro"
                description = "A árvore de filtro chega no corpo (E, OU, EXCETO, ATÉ, TEM)."
                request {
                    body<FiltroDto> {
                        description = "A árvore de filtro. Espelha contratos/filtro.schema.json."
                        required = true
                    }
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Obras que satisfazem o filtro"
                        body<List<ObraDto>>()
                    }
                    code(HttpStatusCode.UnprocessableEntity) {
                        description = "Filtro inválido, ex.: `tipo` desconhecido"
                        body<Problema>()
                    }
                }
            }) {
                val filtro = call.receive<FiltroDto>().paraDominio()
                call.respond(buscarObras(filtro).map { it.paraDto() })
            }
        }
    }
}
