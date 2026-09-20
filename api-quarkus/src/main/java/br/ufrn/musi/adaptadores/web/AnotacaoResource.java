package br.ufrn.musi.adaptadores.web;

import br.ufrn.musi.adaptadores.web.DtosWeb.AnotacaoDto;
import br.ufrn.musi.adaptadores.web.DtosWeb.NovaAnotacaoDto;
import br.ufrn.musi.adaptadores.web.DtosWeb.PaginaDeAnotacoesDto;
import br.ufrn.musi.aplicacao.AnotacoesDeObras;
import br.ufrn.musi.aplicacao.FiltroDeAnotacoes;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * As anotações de uma obra: a relação 1:N, na rota aninhada `/obras/{id}/anotacoes`.
 * `{anotacaoId}` chega como `String` para que `xyz` seja `400`, e não `404`.
 */
@Path("/obras/{id}/anotacoes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "anotações")
public class AnotacaoResource {

    private final AnotacoesDeObras anotacoes;

    @Inject
    public AnotacaoResource(AnotacoesDeObras anotacoes) {
        this.anotacoes = anotacoes;
    }

    @GET
    @Operation(summary = "Lista as anotações de uma obra, paginado",
        description = "Em ordem de criação. Anotações divergentes de curadores diferentes coexistem.")
    @APIResponse(responseCode = "200", description = "Uma página de anotações")
    @APIResponse(responseCode = "400", description = "Parâmetro com tipo errado", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "404", description = "Obra inexistente", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "422", description = "Página ou tamanho fora da faixa", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "503", description = "Instância sem banco configurado (ADR-0004)", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public PaginaDeAnotacoesDto listar(
            @PathParam("id") String obraId,
            @Parameter(description = "Página, a partir de 0. Padrão: 0.", schema = @Schema(type = SchemaType.INTEGER)) @QueryParam("pagina") String pagina,
            @Parameter(description = "Itens por página, de 1 a 100. Padrão: 20.", schema = @Schema(type = SchemaType.INTEGER)) @QueryParam("tamanho") String tamanho,
            @Parameter(description = "Só as deste curador.") @QueryParam("curador") String curador,
            @Parameter(description = "Só as desta dimensão.") @QueryParam("dimensao") String dimensao) {
        var filtro = new FiltroDeAnotacoes(Parametros.texto(curador), Parametros.texto(dimensao));
        return PaginaDeAnotacoesDto.de(anotacoes.listar(obraId, filtro, Parametros.pagina(pagina, tamanho)));
    }

    @POST
    @Operation(summary = "Anota uma obra", description = "A anotação é assinada: `curador` é obrigatório. A data é do servidor.")
    @APIResponse(responseCode = "201", description = "Criada",
        headers = @Header(name = "Location", description = "/obras/{id}/anotacoes/{anotacaoId}", schema = @Schema(type = SchemaType.STRING)),
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AnotacaoDto.class)))
    @APIResponse(responseCode = "400", description = "Corpo que não é JSON válido, ou sem campo obrigatório", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "404", description = "Obra inexistente", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "409", description = "O mesmo curador já anotou esta faceta nesta obra", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "422", description = "Viola regra do domínio; a lista vem em `violacoes`", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "503", description = "Instância sem banco configurado (ADR-0004)", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Response criar(@PathParam("id") String obraId, @NotNull @Valid NovaAnotacaoDto nova) {
        var criada = anotacoes.criar(obraId, nova.paraDados());
        return Response.created(URI.create("/obras/" + obraId + "/anotacoes/" + criada.id()))
            .entity(AnotacaoDto.de(criada)).build();
    }

    @GET
    @Path("/{anotacaoId}")
    @Operation(summary = "Busca uma anotação da obra")
    @APIResponse(responseCode = "200", description = "A anotação")
    @APIResponse(responseCode = "400", description = "`anotacaoId` não é inteiro", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "404", description = "Obra ou anotação inexistente", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "503", description = "Instância sem banco configurado (ADR-0004)", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public AnotacaoDto buscar(@PathParam("id") String obraId,
                              @Parameter(schema = @Schema(type = SchemaType.INTEGER, format = "int64")) @PathParam("anotacaoId") String anotacaoId) {
        return AnotacaoDto.de(anotacoes.buscar(obraId, Parametros.longo("anotacaoId", anotacaoId)));
    }

    @PUT
    @Path("/{anotacaoId}")
    @Operation(summary = "Substitui uma anotação da obra")
    @APIResponse(responseCode = "200", description = "A anotação como ficou")
    @APIResponse(responseCode = "400", description = "Corpo que não é JSON válido, ou `anotacaoId` não inteiro", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "404", description = "Obra ou anotação inexistente", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "409", description = "O mesmo curador já anotou esta faceta nesta obra", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "422", description = "Viola regra do domínio; a lista vem em `violacoes`", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "503", description = "Instância sem banco configurado (ADR-0004)", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public AnotacaoDto atualizar(@PathParam("id") String obraId,
                                 @Parameter(schema = @Schema(type = SchemaType.INTEGER, format = "int64")) @PathParam("anotacaoId") String anotacaoId,
                                 @NotNull @Valid NovaAnotacaoDto nova) {
        return AnotacaoDto.de(anotacoes.atualizar(obraId, Parametros.longo("anotacaoId", anotacaoId), nova.paraDados()));
    }

    @DELETE
    @Path("/{anotacaoId}")
    @Operation(summary = "Remove uma anotação da obra")
    @APIResponse(responseCode = "204", description = "Removida")
    @APIResponse(responseCode = "400", description = "`anotacaoId` não é inteiro", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "404", description = "Obra ou anotação inexistente", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "503", description = "Instância sem banco configurado (ADR-0004)", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public void remover(@PathParam("id") String obraId,
                        @Parameter(schema = @Schema(type = SchemaType.INTEGER, format = "int64")) @PathParam("anotacaoId") String anotacaoId) {
        anotacoes.remover(obraId, Parametros.longo("anotacaoId", anotacaoId));
    }
}
