package br.ufrn.musi.adaptadores.web

import br.ufrn.musi.adaptadores.busca.FiltroDto
import br.ufrn.musi.adaptadores.busca.ObraDto
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode

/**
 * A documentação OpenAPI das rotas — fora da árvore de rotas.
 *
 * Cada `val` é o bloco que descreve UMA rota para o gerador de OpenAPI. Mantê-los aqui
 * deixa `Rotas.kt` com a árvore limpa (`get(Doc.buscaSimples) { ... }`) sem abrir mão da
 * geração a partir do código: mudou a rota, muda o bloco aqui, e o spec acompanha — não
 * há um arquivo estático que possa divergir.
 *
 * O tipo é `RouteConfig.() -> Unit`, o mesmo bloco que se passaria inline ao `get`/`post`.
 */
object Doc {

    val health: RouteConfig.() -> Unit = {
        summary = "Verificação de saúde"
        response { code(HttpStatusCode.OK) { description = "No ar" } }
    }

    val buscaSimples: RouteConfig.() -> Unit = {
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
    }

    val buscaComposta: RouteConfig.() -> Unit = {
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
    }
}
