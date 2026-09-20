package br.ufrn.musi.aplicacao;

import br.ufrn.musi.dominio.Dominio.Faceta;

/** O que se informa numa anotação. A data é do servidor. */
public record DadosDaAnotacao(Faceta faceta, String curador) {}
