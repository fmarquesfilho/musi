package br.ufrn.musi.adaptadores.web;

import br.ufrn.musi.adaptadores.busca.Dtos;
import br.ufrn.musi.adaptadores.busca.Dtos.FiltroDto;
import br.ufrn.musi.adaptadores.busca.Dtos.ObraDto;
import br.ufrn.musi.aplicacao.BuscarObras;
import br.ufrn.musi.dominio.Dominio.Tem;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * A busca por árvore de filtro, delegada ao serviço Go (`BuscaHttp`). Não passa pelo
 * banco: o Go avalia a árvore sobre o acervo dele (ADR-0004 explica a convivência).
 *
 * Compare com RotasDeBusca.kt do lado Ktor: lá as rotas são código, numa árvore; aqui são
 * anotações, distribuídas por classe.
 */
@Path("/busca")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "busca")
public class BuscaResource {

    private final BuscarObras buscarObras;

    @Inject
    public BuscaResource(BuscarObras buscarObras) {
        this.buscarObras = buscarObras;
    }

    /** Busca simples: uma faceta, pela query string. */
    @GET
    @Operation(summary = "Busca simples por uma faceta",
        description = "Filtro `Tem(dimensao, valor)`, avaliado pelo serviço Go. Para buscas compostas, use POST /busca.")
    @APIResponse(responseCode = "200", description = "Obras que têm a faceta pedida")
    @APIResponse(responseCode = "400", description = "Falta `dimensao` ou `valor`",
        content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Response porFaceta(
            @Parameter(required = true, description = "A dimensão da faceta, ex.: `ritmo`.") @QueryParam("dimensao") String dimensao,
            @Parameter(required = true, description = "O valor da faceta, ex.: `baiao`.") @QueryParam("valor") String valor) {

        if (Parametros.texto(dimensao) == null || Parametros.texto(valor) == null) {
            return Problema.resposta(400, "parametro-ausente", "Parâmetros obrigatórios ausentes",
                "Informe `dimensao` e `valor`.", "/busca", null);
        }

        var obras = buscarObras.executar(new Tem(dimensao, valor))
                               .stream().map(Dtos::paraDto).toList();

        var cache = new CacheControl();
        cache.setMaxAge(60);   // o acervo do Go não muda em execução
        return Response.ok(obras).cacheControl(cache).build();
    }

    /**
     * Busca composta: a árvore de filtro chega no corpo. POST porque a árvore não cabe
     * confortavelmente numa query string — e isso custa o cache.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Busca composta pela árvore de filtro",
        description = "A árvore (E, OU, EXCETO, ATÉ, TEM) é avaliada pelo serviço Go. Espelha contratos/filtro.schema.json.")
    @APIResponse(responseCode = "200", description = "Obras que satisfazem o filtro")
    @APIResponse(responseCode = "400", description = "Corpo que não é JSON de filtro",
        content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "422", description = "Filtro inválido, ex.: `tipo` desconhecido",
        content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public List<ObraDto> buscar(FiltroDto dto) {
        return buscarObras.executar(Dtos.paraDominio(dto))
                          .stream().map(Dtos::paraDto).toList();
    }
}
