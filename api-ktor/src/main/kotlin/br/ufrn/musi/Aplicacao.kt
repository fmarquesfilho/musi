package br.ufrn.musi

import br.ufrn.musi.adaptadores.persistencia.ConfigBanco
import br.ufrn.musi.adaptadores.persistencia.criarDataSource
import br.ufrn.musi.adaptadores.persistencia.migrar
import br.ufrn.musi.adaptadores.web.rotas
import br.ufrn.musi.adaptadores.web.tratarErros
import io.ktor.http.CacheControl
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.CachingOptions
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.server.plugins.conditionalheaders.ConditionalHeaders
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.module.Module
import org.koin.ktor.plugin.Koin

fun main() {
    val porta = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val urlBusca = System.getenv("MUSI_BUSCA_URL") ?: "http://localhost:9090"

    // Engine CIO (corrotinas puras): menor footprint que o Netty. Ver docs/BENCHMARK.md.
    embeddedServer(CIO, port = porta, host = "0.0.0.0") {
        modulo(urlBusca, ConfigBanco.doAmbiente())
    }.start(wait = true)
}

/**
 * Produção: com `DB_URL`, abre o pool, aplica as migrações e liga o CRUD ao PostgreSQL.
 * Sem `DB_URL` (o Render, até a Sprint 3 — ADR-0004), sobe só com a busca.
 */
fun Application.modulo(urlBusca: String, configBanco: ConfigBanco?) {
    val banco = configBanco?.let { config ->
        val dataSource = criarDataSource(config)
        migrar(dataSource)
        monitor.subscribe(ApplicationStopped) { dataSource.close() }
        Database.connect(dataSource)
    }
    if (banco == null) environment.log.warn("DB_URL ausente: CRUD responde 503; a busca funciona")
    configurar(modulosDaAplicacao(urlBusca, banco))
}

/**
 * A configuração da aplicação, em plugins. Recebe o grafo pronto: os testes de rota
 * passam repositórios em memória, os de integração, o PostgreSQL do Testcontainers.
 *
 * Cada `install` é uma preocupação transversal, explícita e ordenada. Compare com
 * autoconfiguração por classpath: aqui, o que está ligado está escrito.
 */
fun Application.configurar(modulos: List<Module>) {

    install(Koin) { modules(modulos) }

    // CORS: sem isto, o navegador barra chamadas de outra origem (Hoppscotch web,
    // Swagger servido de outra porta, o app Compose/Web...) antes de chegarem aqui —
    // o preflight OPTIONS volta 405 e a resposta não traz Access-Control-Allow-Origin.
    // anyHost() é liberal de propósito: API pública, sem credenciais nem cookies, voltada
    // ao ensino. Com autenticação (entrega final), troque por allowHost(...) com as origens
    // conhecidas.
    install(CORS) {
        anyHost()
        listOf(HttpMethod.Options, HttpMethod.Get, HttpMethod.Post, HttpMethod.Put, HttpMethod.Delete)
            .forEach { allowMethod(it) }
        allowHeader(HttpHeaders.ContentType)
        exposeHeader(HttpHeaders.Location)
    }

    // explicitNulls = false: campos nulos (como `mbid` antes da conciliação) saem do JSON,
    // em vez de virarem `"mbid": null` em toda obra. Ver ADR-0003.
    install(ContentNegotiation) { json(Json { explicitNulls = false }) }

    install(CallLogging)

    // Cache-Control só na busca simples (GET /busca): o acervo do Go não muda em execução.
    // O CRUD muda a cada escrita, então não leva max-age.
    install(CachingHeaders) {
        options { call, _ ->
            if (call.request.httpMethod == HttpMethod.Get && call.request.path() == "/busca")
                CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 60))
            else null
        }
    }

    // Responde `304` a If-None-Match / If-Modified-Since quando a resposta declara versão
    // (ETag, Last-Modified). Nenhuma rota declara ainda: gerar ETag é a história P2 da
    // Sprint 2 (docs/proposta.md, seção 3).
    install(ConditionalHeaders)

    // Exceção vira application/problem+json — RFC 9457. Ver adaptadores/web/Erros.kt.
    tratarErros()

    rotas()
}
