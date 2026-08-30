package br.ufrn.musi

import br.ufrn.musi.adaptadores.web.Problema
import br.ufrn.musi.adaptadores.web.rotas
import io.github.smiley4.ktoropenapi.OpenApi
import io.ktor.http.CacheControl
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.CachingOptions
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.server.plugins.conditionalheaders.ConditionalHeaders
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import org.koin.ktor.plugin.Koin

fun main() {
    val porta = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val urlBusca = System.getenv("MUSI_BUSCA_URL") ?: "http://localhost:9090"

    // Engine CIO (corrotinas puras): menor footprint que o Netty. Ver docs/BENCHMARK.md.
    embeddedServer(CIO, port = porta, host = "0.0.0.0") {
        modulo(urlBusca)
    }.start(wait = true)
}

/**
 * A configuração da aplicação, em plugins.
 *
 * Cada `install` é uma preocupação transversal, explícita e ordenada. Compare com
 * autoconfiguração por classpath: aqui, o que está ligado está escrito.
 */
fun Application.modulo(urlBusca: String) {

    install(Koin) { modules(modulosDaAplicacao(urlBusca)) }

    // OpenAPI gerado a partir das rotas documentadas em Rotas.kt.
    // A documentação vive junto da rota, no mesmo DSL — não num arquivo à parte
    // que pode divergir do código. As rotas de spec e do Swagger UI ficam em Rotas.kt.
    install(OpenApi) {
        info {
            title = "MUSI — API de busca (Ktor)"
            version = "0.1.0"
            description = "Fachada de leitura do acervo. A ordenação é sempre " +
                "declarada por quem consulta, nunca automática (ADR-0002)."
        }
        server {
            url = "http://localhost:8080"
            description = "Execução local"
        }
    }

    // explicitNulls = false: campos nulos (como `mbid` antes da conciliação) saem do JSON,
    // em vez de virarem `"mbid": null` em toda obra. Ver ADR-0003.
    install(ContentNegotiation) { json(Json { explicitNulls = false }) }

    install(CallLogging)

    // Cache-Control nas respostas de leitura — o assunto da Parte 2 da aula.
    install(CachingHeaders) {
        options { _, _ ->
            CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 60))
        }
    }

    // ETag e o `304` da requisição condicional, sem escrever nada à mão.
    install(ConditionalHeaders)

    // Exceção vira resposta em application/problem+json — RFC 9457.
    install(StatusPages) {
        exception<IllegalArgumentException> { call, causa ->
            call.respond(
                HttpStatusCode.UnprocessableEntity,
                Problema(
                    type = "https://musi.ufrn.br/erros/filtro-invalido",
                    title = "Filtro inválido",
                    status = 422,
                    detail = causa.message,
                ),
            )
        }
        exception<Throwable> { call, causa ->
            call.application.environment.log.error("erro não tratado", causa)
            call.respond(
                HttpStatusCode.InternalServerError,
                Problema(
                    type = "https://musi.ufrn.br/erros/interno",
                    title = "Erro interno",
                    status = 500,
                ),
            )
        }
    }

    rotas()
}
