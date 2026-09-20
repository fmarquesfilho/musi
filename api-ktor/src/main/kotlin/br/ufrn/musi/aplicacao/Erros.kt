package br.ufrn.musi.aplicacao

/**
 * Os erros que a aplicação sabe nomear.
 *
 * Nenhum deles fala de HTTP: quem decide que `NaoEncontrado` vira `404` é a borda
 * (StatusPages, em adaptadores/web). O caso de uso só diz o que aconteceu.
 */
sealed class ErroDeAplicacao(mensagem: String) : RuntimeException(mensagem)

/** A entrada viola uma regra de forma. Traz todas as violações, não só a primeira. */
class EntradaInvalida(val violacoes: List<String>) : ErroDeAplicacao(violacoes.joinToString("; "))

class NaoEncontrado(recurso: String, id: Any) : ErroDeAplicacao("$recurso `$id` não existe")

/** A operação contraria o estado atual: MBID já usado, anotação repetida. */
class Conflito(mensagem: String) : ErroDeAplicacao(mensagem)

/** Não há banco configurado nesta instância (o deploy sem banco, até a Sprint 3 — ADR-0004). */
class PersistenciaIndisponivel : ErroDeAplicacao("persistência não configurada nesta instância")
