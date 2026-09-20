package br.ufrn.musi

import br.ufrn.musi.adaptadores.busca.BuscaHttp
import br.ufrn.musi.adaptadores.persistencia.AnotacoesPostgres
import br.ufrn.musi.adaptadores.persistencia.AnotacoesSemBanco
import br.ufrn.musi.adaptadores.persistencia.ObrasPostgres
import br.ufrn.musi.adaptadores.persistencia.ObrasSemBanco
import br.ufrn.musi.aplicacao.AnotacoesDeObras
import br.ufrn.musi.aplicacao.BuscarObras
import br.ufrn.musi.aplicacao.CatalogoDeObras
import br.ufrn.musi.aplicacao.FonteDeObras
import br.ufrn.musi.aplicacao.RepositorioDeAnotacoes
import br.ufrn.musi.aplicacao.RepositorioDeObras
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * O grafo de dependências, num arquivo só.
 *
 * Esta é a diferença mais visível em relação a Spring ou Micronaut: em vez de
 * anotações espalhadas pelas classes, a montagem fica reunida e legível. Dá para
 * responder "quem fornece RepositorioDeObras?" lendo este arquivo.
 *
 * Três módulos, para trocar uma parte sem mexer nas outras: os testes de rota usam
 * `casosDeUso` com repositórios em memória; o deploy sem banco usa `persistenciaSemBanco`.
 *
 * O custo: Koin resolve em tempo de execução, então um `single` faltando aparece na
 * inicialização, e não na compilação. O teste `ModulosTest` usa `verify()` e move a
 * descoberta para o CI.
 */
fun busca(urlBusca: String) = module {
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) { json() }
        }
    }
    single<FonteDeObras> { BuscaHttp(cliente = get(), urlBase = urlBusca) }
    single { BuscarObras(fonte = get()) }
}

fun persistenciaPostgres(banco: Database) = module {
    single<RepositorioDeObras> { ObrasPostgres(banco) }
    single<RepositorioDeAnotacoes> { AnotacoesPostgres(banco) }
}

val persistenciaSemBanco = module {
    single<RepositorioDeObras> { ObrasSemBanco }
    single<RepositorioDeAnotacoes> { AnotacoesSemBanco }
}

val casosDeUso = module {
    single { CatalogoDeObras(obras = get()) }
    single { AnotacoesDeObras(obras = get(), anotacoes = get()) }
}

/** O conjunto usado em produção, com ou sem banco. */
fun modulosDaAplicacao(urlBusca: String, banco: Database?): List<Module> =
    listOf(busca(urlBusca), banco?.let(::persistenciaPostgres) ?: persistenciaSemBanco, casosDeUso)
