package br.ufrn.musi.adaptadores.web

import io.ktor.http.ContentType
import io.ktor.openapi.OpenApiDoc
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.openapi.hide
import io.ktor.server.routing.openapi.plus
import io.ktor.server.routing.routing
import io.ktor.server.routing.routingRoot
import io.ktor.utils.io.ExperimentalKtorApi

/**
 * As rotas são CÓDIGO, não anotações.
 *
 * A árvore inteira se lê de cima para baixo: saúde e documentação aqui, a busca em
 * RotasDeBusca.kt, o CRUD em RotasDeObras.kt. Cada rota termina em `.describe(Doc.x)`, e é
 * desse bloco que o OpenAPI é gerado — pelo próprio Ktor (`ktor-server-routing-openapi`),
 * sem biblioteca de terceiros. Os blocos ficam em `Doc` (RotasDoc.kt), fora da árvore.
 *
 * O controller é um adaptador: traduz HTTP para chamadas de caso de uso. Se você
 * encontrar aqui um `if` que decide algo sobre obras, ele está no lugar errado.
 *
 * `describe` é API experimental no Ktor 3.5 (`@OptIn(ExperimentalKtorApi::class)`).
 */
@OptIn(ExperimentalKtorApi::class)
fun Application.rotas() {
    routing {

        // Especificação gerada das rotas, para importar em Postman, Hoppscotch, Bruno.
        // Swagger UI em /swagger, com a mesma especificação. Nenhuma das duas entra no spec.
        get("/openapi.json") {
            call.respond(OpenApiDoc(info = INFO) + call.application.routingRoot.descendants())
        }.hide()
        swaggerUI(path = "swagger") {
            info = INFO
            source = OpenApiDocSource.Routing(contentType = ContentType.Application.Json)
        }.hide()

        get("/health") { call.respond(mapOf("status" to "UP")) }.describe(Doc.health)

        rotasDeBusca()
        rotasDeObras()
    }
}

private val INFO = OpenApiInfo(
    title = "MUSI — API (Ktor)",
    version = "0.2.0",
    description = "CRUD de obras e anotações sobre PostgreSQL, e a busca por árvore de " +
        "filtro, delegada ao serviço Go. A ordenação é sempre declarada por quem consulta, " +
        "nunca automática (ADR-0002).",
)
