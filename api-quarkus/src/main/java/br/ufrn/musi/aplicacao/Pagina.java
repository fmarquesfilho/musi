package br.ufrn.musi.aplicacao;

import java.util.List;
import java.util.function.Function;

/** Uma fatia da listagem e o total, para quem consome saber quantas páginas há. */
public record Pagina<T>(List<T> itens, PedidoDePagina pedido, long total) {

    public <R> Pagina<R> map(Function<T, R> f) {
        return new Pagina<>(itens.stream().map(f).toList(), pedido, total);
    }
}
