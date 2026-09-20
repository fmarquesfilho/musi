package br.ufrn.musi.aplicacao;

import br.ufrn.musi.aplicacao.ErroDeAplicacao.EntradaInvalida;
import java.util.List;

/**
 * Qual fatia da listagem se quer. `pagina` começa em 0; o tamanho tem teto. A fatia vai
 * para o SQL (`page(...)` do Panache vira LIMIT/OFFSET); nada é paginado em memória.
 */
public record PedidoDePagina(int pagina, int tamanho) {
    public static final int PADRAO = 20;
    public static final int MAXIMO = 100;

    public PedidoDePagina {
        if (pagina < 0) throw new EntradaInvalida(List.of("pagina deve ser >= 0"));
        if (tamanho < 1 || tamanho > MAXIMO)
            throw new EntradaInvalida(List.of("tamanho deve estar entre 1 e " + MAXIMO));
    }
}
