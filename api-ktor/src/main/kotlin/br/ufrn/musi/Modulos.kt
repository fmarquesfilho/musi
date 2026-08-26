package br.ufrn.musi

import br.ufrn.musi.adaptadores.busca.BuscaHttp
import br.ufrn.musi.aplicacao.BuscarObras
import br.ufrn.musi.aplicacao.FonteDeObras
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.dsl.module

/**
 * O grafo de dependências, num arquivo só.
 *
 * Esta é a diferença mais visível em relação a Spring ou Micronaut: em vez de
 * anotações espalhadas pelas classes, a montagem fica reunida e legível. Dá para
 * responder "quem fornece FonteDeObras?" lendo dez linhas.
 *
 * O custo: Koin resolve em tempo de execução, então um `single` faltando aparece na
 * inicialização, e não na compilação. O teste `ModulosTest` usa `verify()` e move a
 * descoberta para o CI.
 */
fun modulosDaAplicacao(urlBusca: String) = module {

    single {
        HttpClient(CIO) {
            install(ContentNegotiation) { json() }
        }
    }

    single<FonteDeObras> { BuscaHttp(cliente = get(), urlBase = urlBusca) }

    single { BuscarObras(fonte = get()) }
}
