package br.ufrn.musi.aplicacao;

import br.ufrn.musi.dominio.Dominio.Faceta;
import java.util.List;

/** O que se informa para criar ou substituir uma obra. A identidade é do servidor. */
public record DadosDaObra(String titulo, String artista, int ano, List<Faceta> facetas,
                          String mbid, String mbidComposicao) {}
