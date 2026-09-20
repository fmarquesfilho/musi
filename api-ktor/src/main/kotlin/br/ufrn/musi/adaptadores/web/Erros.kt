package br.ufrn.musi.adaptadores.web

import br.ufrn.musi.aplicacao.Conflito
import br.ufrn.musi.aplicacao.EntradaInvalida
import br.ufrn.musi.aplicacao.NaoEncontrado
import br.ufrn.musi.aplicacao.PersistenciaIndisponivel
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages

/**
 * Exceção vira resposta, num lugar só. Os casos de uso lançam erros com nome
 * (aplicacao/Erros.kt), e é aqui que cada nome ganha um status HTTP.
 *
 * | Erro                          | Status |
 * |-------------------------------|--------|
 * | JSON malformado, parâmetro com tipo errado | 400 |
 * | NaoEncontrado                 | 404    |
 * | Conflito                      | 409    |
 * | EntradaInvalida, filtro de busca inválido | 422 |
 * | PersistenciaIndisponivel      | 503    |
 * | qualquer outro                | 500, sem detalhe interno |
 */
fun Application.tratarErros() {
    install(StatusPages) {
        exception<BadRequestException> { call, causa ->
            call.responderProblema(
                HttpStatusCode.BadRequest, "requisicao-malformada", "Requisição malformada",
                causa.cause?.message ?: causa.message,
            )
        }
        exception<EntradaInvalida> { call, causa ->
            call.responderProblema(
                HttpStatusCode.UnprocessableEntity, "entrada-invalida", "Entrada inválida",
                "A entrada viola ${causa.violacoes.size} regra(s) do domínio.", causa.violacoes,
            )
        }
        exception<NaoEncontrado> { call, causa ->
            call.responderProblema(HttpStatusCode.NotFound, "nao-encontrado", "Recurso inexistente", causa.message)
        }
        exception<Conflito> { call, causa ->
            call.responderProblema(HttpStatusCode.Conflict, "conflito", "Conflito com o estado atual", causa.message)
        }
        exception<PersistenciaIndisponivel> { call, causa ->
            call.responderProblema(
                HttpStatusCode.ServiceUnavailable, "sem-banco", "Persistência indisponível",
                "${causa.message}. A busca continua disponível em /busca.",
            )
        }
        // A árvore de filtro com `tipo` desconhecido (Dtos.kt, adaptadores/busca).
        exception<IllegalArgumentException> { call, causa ->
            call.responderProblema(HttpStatusCode.UnprocessableEntity, "filtro-invalido", "Filtro inválido", causa.message)
        }
        exception<Throwable> { call, causa ->
            call.application.environment.log.error("erro não tratado", causa)
            call.responderProblema(HttpStatusCode.InternalServerError, "interno", "Erro interno")
        }
    }
}
