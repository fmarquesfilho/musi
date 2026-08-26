package br.ufrn.musi

import br.ufrn.musi.adaptadores.web.Problema
import br.ufrn.musi.adaptadores.web.rotas
import io.ktor.http.CacheControl
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.CachingOptions
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
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

    embeddedServer(Netty, port = porta, host = "0.0.0.0") {
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

    install(ContentNegotiation) { json() }

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
