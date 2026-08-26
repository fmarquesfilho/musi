package br.ufrn.musi.aplicacao

import br.ufrn.musi.dominio.Filtro
import br.ufrn.musi.dominio.Obra

/**
 * Caso de uso. Kotlin puro: nenhuma anotação, nenhum framework.
 *
 * A ligação com a implementação é declarada no módulo de Koin, em `Modulos.kt`.
 * O grafo de dependências fica num arquivo só, legível de cima a baixo, em vez de
 * espalhado por anotações.
 *
 * Consequência: esta classe é instanciável com `BuscarObras(fonte)` num teste, sem
 * nenhuma infraestrutura.
 */
class BuscarObras(private val fonte: FonteDeObras) {
    suspend operator fun invoke(filtro: Filtro): List<Obra> = fonte.buscar(filtro)
}
