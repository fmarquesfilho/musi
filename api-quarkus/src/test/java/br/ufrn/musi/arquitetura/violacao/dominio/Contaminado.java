package br.ufrn.musi.arquitetura.violacao.dominio;

import jakarta.ws.rs.core.Response;

/** Fixture de ArquiteturaTest: um "domínio" que importa Jakarta REST. Não imite. */
public class Contaminado {
    public Response.Status status() {
        return Response.Status.OK;
    }
}
