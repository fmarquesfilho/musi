package br.ufrn.musi.adaptadores.web;

import br.ufrn.musi.aplicacao.PedidoDePagina;
import br.ufrn.musi.dominio.Dominio.Ordenacao;
import jakarta.ws.rs.BadRequestException;
import java.util.Arrays;

/**
 * Leitura dos parâmetros de consulta e de caminho.
 *
 * Os parâmetros numéricos chegam como `String` de propósito: com `@QueryParam Integer`,
 * um `?pagina=abc` vira `404` (é o que a especificação Jakarta REST manda), e o certo é
 * `400`. Mesmo comportamento do Parametros.kt do lado Ktor.
 */
final class Parametros {
    private Parametros() {}

    static Integer inteiro(String nome, String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return Integer.valueOf(valor);
        } catch (NumberFormatException e) {
            throw new BadRequestException("`" + nome + "` deve ser um inteiro: `" + valor + "`");
        }
    }

    static long longo(String nome, String valor) {
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException e) {
            throw new BadRequestException("`" + nome + "` deve ser um inteiro: `" + valor + "`");
        }
    }

    static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }

    static PedidoDePagina pagina(String pagina, String tamanho) {
        Integer p = inteiro("pagina", pagina);
        Integer t = inteiro("tamanho", tamanho);
        return new PedidoDePagina(p == null ? 0 : p, t == null ? PedidoDePagina.PADRAO : t);
    }

    /** `?ordem=` com os valores de `Ordenacao`, em minúsculas e com hífen (ADR-0002). */
    static Ordenacao ordenacao(String valor) {
        if (texto(valor) == null) return Ordenacao.TITULO;
        return Arrays.stream(Ordenacao.values())
            .filter(o -> paraParametro(o).equals(valor))
            .findFirst()
            .orElseThrow(() -> new BadRequestException("`ordem` deve ser um de "
                + Arrays.stream(Ordenacao.values()).map(Parametros::paraParametro).toList() + ": `" + valor + "`"));
    }

    static String paraParametro(Ordenacao o) {
        return o.name().toLowerCase().replace('_', '-');
    }
}
