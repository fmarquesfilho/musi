package br.ufrn.musi.adaptadores.web

import br.ufrn.musi.adaptadores.busca.FiltroDto
import br.ufrn.musi.adaptadores.busca.paraDominio
import br.ufrn.musi.adaptadores.busca.paraDto
import br.ufrn.musi.aplicacao.BuscarObras
import br.ufrn.musi.dominio.Filtro
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

/**
 * As rotas são CÓDIGO, não anotações.
 *
 * A árvore de rotas inteira cabe numa tela e se lê de cima para baixo, sem procurar
 * anotações espalhadas por várias classes.
 *
 * O controller é um adaptador: traduz HTTP para chamadas de caso de uso. Se você
 * encontrar aqui um `if` que decide algo sobre obras, ele está no lugar errado.
 */
fun Application.rotas() {
    val buscarObras by inject<BuscarObras>()   // Koin

    routing {
        get("/health") {
            call.respond(mapOf("status" to "UP"))
        }

        route("/obras") {

            // Busca simples: uma faceta, pela query string.
            get {
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
            post {
                val filtro = call.receive<FiltroDto>().paraDominio()
                call.respond(buscarObras(filtro).map { it.paraDto() })
            }
        }
    }
}
