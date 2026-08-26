package br.ufrn.musi.adaptadores.web

import kotlinx.serialization.Serializable

/**
 * Erro no formato da RFC 9457 — `application/problem+json`.
 *
 * O `type` é uma URI que identifica a CLASSE do erro; o `detail` descreve AQUELA
 * ocorrência. Um `400` sem corpo obriga quem consome a adivinhar.
 */
@Serializable
data class Problema(
    val type: String,
    val title: String,
    val status: Int,
    val detail: String? = null,
    val instance: String? = null,
)
