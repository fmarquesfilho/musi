package br.ufrn.musi.arquitetura.violacao.dominio

import io.ktor.http.HttpStatusCode

/** Fixture de ArquiteturaTest: um "domínio" que importa Ktor. Não imite. */
class Contaminado {
    fun status(): HttpStatusCode = HttpStatusCode.OK
}
