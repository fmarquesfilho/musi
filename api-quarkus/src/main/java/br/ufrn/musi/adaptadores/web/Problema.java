package br.ufrn.musi.adaptadores.web;

/**
 * Erro no formato da RFC 9457 — `application/problem+json`.
 *
 * O `type` é uma URI que identifica a classe do erro; o `detail` descreve a
 * ocorrência. Idêntico ao `Problema.kt` do lado Ktor.
 */
public record Problema(String type, String title, int status,
                       String detail, String instance) {}
