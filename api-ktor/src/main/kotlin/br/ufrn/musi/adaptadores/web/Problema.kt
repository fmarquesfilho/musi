package br.ufrn.musi.adaptadores.web

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.path
import io.ktor.server.response.respondText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Erro no formato da RFC 9457 — `application/problem+json`.
 *
 * O `type` é uma URI que identifica a CLASSE do erro; o `detail` descreve AQUELA
 * ocorrência. Um `400` sem corpo obriga quem consome a adivinhar. `violacoes` é um membro
 * de extensão (a RFC permite): a lista completa do que está errado na entrada.
 */
@Serializable
data class Problema(
    val type: String,
    val title: String,
    val status: Int,
    val detail: String? = null,
    val instance: String? = null,
    val violacoes: List<String>? = null,
)

private val json = Json { explicitNulls = false }

/**
 * Responde o problema com o media type da RFC, `application/problem+json`, e não
 * `application/json`. O `instance` é o caminho da requisição.
 */
suspend fun ApplicationCall.responderProblema(
    status: HttpStatusCode,
    tipo: String,
    titulo: String,
    detalhe: String? = null,
    violacoes: List<String>? = null,
) {
    val problema = Problema(
        type = "https://musi.ufrn.br/erros/$tipo",
        title = titulo,
        status = status.value,
        detail = detalhe,
        instance = request.path(),
        violacoes = violacoes,
    )
    respondText(json.encodeToString(problema), ContentType.Application.ProblemJson, status)
}
