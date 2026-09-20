package br.ufrn.musi.adaptadores.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * Erro no formato da RFC 9457 — `application/problem+json`.
 *
 * O `type` é uma URI que identifica a classe do erro; o `detail` descreve a
 * ocorrência. `violacoes` é membro de extensão (a RFC permite): a lista completa do que
 * está errado na entrada. Idêntico ao `Problema.kt` do lado Ktor.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Problema(String type, String title, int status,
                       String detail, String instance, List<String> violacoes) {

    public static final String MEDIA_TYPE = "application/problem+json";

    /** A resposta pronta, com o media type da RFC. */
    public static Response resposta(int status, String tipo, String titulo, String detalhe,
                                    String instancia, List<String> violacoes) {
        return Response.status(status)
            .type(MEDIA_TYPE)
            .entity(new Problema("https://musi.ufrn.br/erros/" + tipo, titulo, status,
                                 detalhe, instancia, violacoes))
            .build();
    }
}
