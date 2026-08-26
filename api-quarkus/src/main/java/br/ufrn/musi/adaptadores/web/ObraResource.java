package br.ufrn.musi.adaptadores.web;

import br.ufrn.musi.adaptadores.busca.Dtos;
import br.ufrn.musi.adaptadores.busca.Dtos.FiltroDto;
import br.ufrn.musi.adaptadores.busca.Dtos.ObraDto;
import br.ufrn.musi.aplicacao.BuscarObras;
import br.ufrn.musi.dominio.Dominio.Tem;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * O recurso é um adaptador que traduz HTTP para chamadas de caso de uso.
 *
 * Compare com `Rotas.kt` do lado Ktor: lá as rotas são código, numa árvore que cabe
 * numa tela; aqui são anotações, distribuídas por classe.
 */
@Path("/obras")
@Produces(MediaType.APPLICATION_JSON)
public class ObraResource {

    private final BuscarObras buscarObras;

    @Inject
    public ObraResource(BuscarObras buscarObras) {
        this.buscarObras = buscarObras;
    }

    /** Busca simples: uma faceta, pela query string. */
    @GET
    public Response porFaceta(@QueryParam("dimensao") String dimensao,
                              @QueryParam("valor") String valor) {

        if (dimensao == null || valor == null) {
            return Response.status(400)
                .type("application/problem+json")
                .entity(new Problema(
                    "https://musi.ufrn.br/erros/parametro-ausente",
                    "Parâmetros obrigatórios ausentes", 400,
                    "Informe `dimensao` e `valor`.", "/obras"))
                .build();
        }

        var obras = buscarObras.executar(new Tem(dimensao, valor))
                               .stream().map(Dtos::paraDto).toList();

        var cache = new CacheControl();
        cache.setMaxAge(60);   // o assunto da Parte 2 da aula
        return Response.ok(obras).cacheControl(cache).build();
    }

    /**
     * Busca composta: a árvore de filtro chega no corpo.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public List<ObraDto> buscar(FiltroDto dto) {
        return buscarObras.executar(Dtos.paraDominio(dto))
                          .stream().map(Dtos::paraDto).toList();
    }
}
