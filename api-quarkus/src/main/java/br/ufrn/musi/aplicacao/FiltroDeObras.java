package br.ufrn.musi.aplicacao;

import br.ufrn.musi.dominio.Dominio.Faceta;

/**
 * Filtros da listagem de obras. Todos opcionais (null = sem filtro) e combinados com E.
 * Não confundir com `Filtro`, a árvore da busca (ADR-0002), que o serviço Go avalia.
 */
public record FiltroDeObras(String artista, Integer anoDe, Integer anoAte, Faceta faceta) {}
