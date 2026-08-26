package br.ufrn.musi.adaptadores.web;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Transforma exceção em resposta `application/problem+json`.
 *
 * É o equivalente do plugin `StatusPages` do Ktor. Nas duas pilhas, o objetivo é o
 * mesmo: o domínio lança exceção, e a borda decide o status HTTP.
 */
@Provider
public class FiltroInvalidoMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException causa) {
        return Response.status(422)
            .type("application/problem+json")
            .entity(new Problema(
                "https://musi.ufrn.br/erros/filtro-invalido",
                "Filtro inválido", 422, causa.getMessage(), "/obras"))
            .build();
    }
}
