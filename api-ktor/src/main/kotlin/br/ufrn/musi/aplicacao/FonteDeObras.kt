package br.ufrn.musi.aplicacao

import br.ufrn.musi.dominio.Filtro
import br.ufrn.musi.dominio.Obra

/**
 * Define o que a aplicação precisa, sem dizer quem fornece.
 *
 * Repare no que não há aqui: nenhuma anotação, nenhuma menção a HTTP ou a Ktor.
 * Esta interface pertence a quem a USA, não a quem a implementa — é essa inversão
 * que faz a seta da regra de dependência apontar para dentro.
 *
 * `suspend` porque a implementação real faz E/S. Quem chama não precisa saber se a
 * resposta vem da rede, do banco ou da memória.
 */
interface FonteDeObras {
    suspend fun buscar(filtro: Filtro): List<Obra>
}
