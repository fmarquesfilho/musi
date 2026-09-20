package br.ufrn.musi.adaptadores.web;

import br.ufrn.musi.adaptadores.busca.Dtos;
import br.ufrn.musi.adaptadores.busca.Dtos.ObraDto;
import br.ufrn.musi.adaptadores.web.DtosWeb.NovaObraDto;
import br.ufrn.musi.adaptadores.web.DtosWeb.PaginaDeObrasDto;
import br.ufrn.musi.aplicacao.CatalogoDeObras;
import br.ufrn.musi.aplicacao.FiltroDeObras;
import br.ufrn.musi.dominio.Dominio.Faceta;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
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
 * O CRUD de obras. As anotações da obra ficam em AnotacaoResource, aninhado em
 * `/obras/{id}/anotacoes`. Os erros (400, 404, 409, 422, 503) saem do ErrosMapper.
 *
 * Os parâmetros numéricos de consulta chegam como `String`, com o tipo real declarado para
 * o OpenAPI em `@Schema(type = INTEGER)`; ver Parametros.java.
 */
@Path("/obras")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "obras")
public class ObraResource {

    private final CatalogoDeObras catalogo;

    @Inject
    public ObraResource(CatalogoDeObras catalogo) {
        this.catalogo = catalogo;
    }

    @GET
    @Operation(summary = "Lista as obras, paginado e filtrado",
        description = "Filtros combinados com E. A paginação e os filtros vão para o SQL.")
    @APIResponse(responseCode = "200", description = "Uma página de obras")
    @APIResponse(responseCode = "400", description = "Parâmetro com tipo errado, ou `dimensao` sem `valor`", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "422", description = "Página ou tamanho fora da faixa; `anoDe` > `anoAte`", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "503", description = "Instância sem banco configurado (ADR-0004)", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public PaginaDeObrasDto listar(
            @Parameter(description = "Página, a partir de 0. Padrão: 0.", schema = @Schema(type = SchemaType.INTEGER)) @QueryParam("pagina") String pagina,
            @Parameter(description = "Itens por página, de 1 a 100. Padrão: 20.", schema = @Schema(type = SchemaType.INTEGER)) @QueryParam("tamanho") String tamanho,
            @Parameter(description = "Critério de ordenação, declarado por quem consulta (ADR-0002): `titulo`, `artista`, `ano-crescente`, `ano-decrescente`. Padrão: `titulo`.") @QueryParam("ordem") String ordem,
            @Parameter(description = "Contém este texto, sem diferenciar maiúsculas.") @QueryParam("artista") String artista,
            @Parameter(description = "Ano mínimo, inclusive.", schema = @Schema(type = SchemaType.INTEGER)) @QueryParam("anoDe") String anoDe,
            @Parameter(description = "Ano máximo, inclusive.", schema = @Schema(type = SchemaType.INTEGER)) @QueryParam("anoAte") String anoAte,
            @Parameter(description = "Tem a faceta (`dimensao`, `valor`). Vem junto com `valor`.") @QueryParam("dimensao") String dimensao,
            @Parameter(description = "Ver `dimensao`.") @QueryParam("valor") String valor) {

        var filtro = new FiltroDeObras(Parametros.texto(artista), Parametros.inteiro("anoDe", anoDe),
            Parametros.inteiro("anoAte", anoAte), faceta(dimensao, valor));
        return PaginaDeObrasDto.de(catalogo.listar(filtro, Parametros.ordenacao(ordem), Parametros.pagina(pagina, tamanho)));
    }

    @POST
    @Operation(summary = "Cria uma obra", description = "O `id` é atribuído pelo servidor e volta no cabeçalho `Location`.")
    @APIResponse(responseCode = "201", description = "Criada",
        headers = @Header(name = "Location", description = "/obras/{id} da obra criada", schema = @Schema(type = SchemaType.STRING)),
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ObraDto.class)))
    @APIResponse(responseCode = "400", description = "Corpo que não é JSON válido, ou sem campo obrigatório", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "409", description = "`mbid` repetido", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "422", description = "Viola regra do domínio; a lista vem em `violacoes`", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "503", description = "Instância sem banco configurado (ADR-0004)", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public Response criar(@NotNull @Valid NovaObraDto nova) {
        var criada = catalogo.criar(nova.paraDados());
        return Response.created(URI.create("/obras/" + criada.id())).entity(Dtos.paraDto(criada)).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Busca uma obra pelo id")
    @APIResponse(responseCode = "200", description = "A obra")
    @APIResponse(responseCode = "404", description = "Obra inexistente", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "503", description = "Instância sem banco configurado (ADR-0004)", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public ObraDto buscar(@Parameter(description = "Id da obra, ex.: `obra-03`.") @PathParam("id") String id) {
        return Dtos.paraDto(catalogo.buscar(id));
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Substitui uma obra", description = "PUT substitui a obra inteira, facetas incluídas.")
    @APIResponse(responseCode = "200", description = "A obra como ficou")
    @APIResponse(responseCode = "400", description = "Corpo que não é JSON válido, ou sem campo obrigatório", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "404", description = "Obra inexistente", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "409", description = "`mbid` repetido", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "422", description = "Viola regra do domínio; a lista vem em `violacoes`", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "503", description = "Instância sem banco configurado (ADR-0004)", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public ObraDto atualizar(@PathParam("id") String id, @NotNull @Valid NovaObraDto nova) {
        return Dtos.paraDto(catalogo.atualizar(id, nova.paraDados()));
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Remove uma obra e as anotações dela")
    @APIResponse(responseCode = "204", description = "Removida")
    @APIResponse(responseCode = "404", description = "Obra inexistente", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    @APIResponse(responseCode = "503", description = "Instância sem banco configurado (ADR-0004)", content = @Content(mediaType = Problema.MEDIA_TYPE, schema = @Schema(implementation = Problema.class)))
    public void remover(@PathParam("id") String id) {
        catalogo.remover(id);   // método void: o Quarkus REST responde 204
    }

    /** `?dimensao=&valor=` filtra por faceta; os dois vêm juntos ou nenhum. */
    private static Faceta faceta(String dimensao, String valor) {
        String d = Parametros.texto(dimensao), v = Parametros.texto(valor);
        if ((d == null) != (v == null)) throw new BadRequestException("informe `dimensao` e `valor` juntos");
        return d == null ? null : new Faceta(d, v);
    }
}
