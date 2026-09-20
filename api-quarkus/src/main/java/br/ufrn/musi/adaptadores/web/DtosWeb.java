package br.ufrn.musi.adaptadores.web;

import br.ufrn.musi.adaptadores.busca.Dtos;
import br.ufrn.musi.adaptadores.busca.Dtos.FacetaDto;
import br.ufrn.musi.adaptadores.busca.Dtos.ObraDto;
import br.ufrn.musi.aplicacao.DadosDaAnotacao;
import br.ufrn.musi.aplicacao.DadosDaObra;
import br.ufrn.musi.aplicacao.Pagina;
import br.ufrn.musi.dominio.Dominio.Anotacao;
import br.ufrn.musi.dominio.Dominio.Faceta;
import br.ufrn.musi.dominio.Dominio.Obra;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * DTOs do CRUD. Os de leitura de obra (`ObraDto`, `FacetaDto`) são os mesmos da busca.
 *
 * Bean Validation cuida da FORMA (campo ausente → 400); as regras do domínio (termo de
 * faceta, faixa de ano) ficam no caso de uso (→ 422), como no Ktor. `ano` é `Integer`, e
 * não `int`: com `int`, um `ano` ausente viraria 0 sem aviso.
 */
public final class DtosWeb {
    private DtosWeb() {}

    static final String OBRIGATORIO = "obrigatório";

    public record NovaObraDto(
        @NotNull(message = OBRIGATORIO) String titulo,
        @NotNull(message = OBRIGATORIO) String artista,
        @NotNull(message = OBRIGATORIO) Integer ano,
        List<@Valid @NotNull(message = OBRIGATORIO) FacetaValidada> facetas,
        String mbid,
        String mbidComposicao) {

        DadosDaObra paraDados() {
            var fs = facetas == null ? List.<Faceta>of() : facetas.stream().map(FacetaValidada::paraDominio).toList();
            return new DadosDaObra(titulo, artista, ano, fs, mbid, mbidComposicao);
        }
    }

    /** Faceta de entrada: os dois campos obrigatórios. */
    public record FacetaValidada(@NotNull(message = OBRIGATORIO) String dimensao, @NotNull(message = OBRIGATORIO) String valor) {
        Faceta paraDominio() {
            return new Faceta(dimensao, valor);
        }
    }

    public record NovaAnotacaoDto(@NotNull(message = OBRIGATORIO) @Valid FacetaValidada faceta, @NotNull(message = OBRIGATORIO) String curador) {
        DadosDaAnotacao paraDados() {
            return new DadosDaAnotacao(faceta.paraDominio(), curador);
        }
    }

    /** `criadoEm` em ISO-8601, UTC. */
    public record AnotacaoDto(long id, String obraId, FacetaDto faceta, String curador, String criadoEm) {
        static AnotacaoDto de(Anotacao a) {
            return new AnotacaoDto(a.id(), a.obraId(), new FacetaDto(a.faceta().dimensao(), a.faceta().valor()),
                a.curador(), a.criadoEm().toString());
        }
    }

    /** Uma página da listagem. `total` permite calcular quantas páginas existem. */
    public record PaginaDeObrasDto(List<ObraDto> itens, int pagina, int tamanho, long total) {
        static PaginaDeObrasDto de(Pagina<Obra> p) {
            return new PaginaDeObrasDto(p.itens().stream().map(Dtos::paraDto).toList(),
                p.pedido().pagina(), p.pedido().tamanho(), p.total());
        }
    }

    public record PaginaDeAnotacoesDto(List<AnotacaoDto> itens, int pagina, int tamanho, long total) {
        static PaginaDeAnotacoesDto de(Pagina<Anotacao> p) {
            return new PaginaDeAnotacoesDto(p.itens().stream().map(AnotacaoDto::de).toList(),
                p.pedido().pagina(), p.pedido().tamanho(), p.total());
        }
    }
}
