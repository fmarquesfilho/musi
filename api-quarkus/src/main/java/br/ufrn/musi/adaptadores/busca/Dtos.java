package br.ufrn.musi.adaptadores.busca;

import br.ufrn.musi.dominio.Dominio.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * DTOs da fronteira. Espelham contratos/filtro.schema.json.
 */
public final class Dtos {
    private Dtos() {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FiltroDto(String tipo, String dimensao, String valor,
                            List<FiltroDto> opcoes, List<FiltroDto> exigencias,
                            FiltroDto filtro, Integer ano) {

        public static FiltroDto tem(String d, String v) {
            return new FiltroDto("tem", d, v, null, null, null, null);
        }
    }

    public record FacetaDto(String dimensao, String valor) {}

    public record ObraDto(String id, String titulo, String artista, int ano,
                          List<FacetaDto> facetas) {}

    public static Obra paraDominio(ObraDto d) {
        return new Obra(d.id(), d.titulo(), d.artista(), d.ano(),
            d.facetas().stream().map(f -> new Faceta(f.dimensao(), f.valor())).toList());
    }

    public static ObraDto paraDto(Obra o) {
        return new ObraDto(o.id(), o.titulo(), o.artista(), o.ano(),
            o.facetas().stream().map(f -> new FacetaDto(f.dimensao(), f.valor())).toList());
    }

    /**
     * Domínio → DTO.
     *
     * Switch exaustivo sobre a interface selada: acrescentar um construtor de Filtro
     * faz este método parar de compilar. É o equivalente exato do `when` do lado
     * Kotlin, em `Dtos.kt`.
     */
    public static FiltroDto paraDto(Filtro f) {
        return switch (f) {
            case Tem t    -> new FiltroDto("tem", t.dimensao(), t.valor(), null, null, null, null);
            case Ou o     -> new FiltroDto("ou", null, null,
                                 o.opcoes().stream().map(Dtos::paraDto).toList(), null, null, null);
            case E e      -> new FiltroDto("e", null, null, null,
                                 e.exigencias().stream().map(Dtos::paraDto).toList(), null, null);
            case Exceto x -> new FiltroDto("exceto", null, null, null, null, paraDto(x.filtro()), null);
            case Ate a    -> new FiltroDto("ate", null, null, null, null, null, a.ano());
        };
    }

    /** DTO → domínio, na fronteira HTTP pública. */
    public static Filtro paraDominio(FiltroDto d) {
        return switch (d.tipo()) {
            case "tem"    -> new Tem(d.dimensao(), d.valor());
            case "ou"     -> new Ou(d.opcoes() == null ? List.of()
                                 : d.opcoes().stream().map(Dtos::paraDominio).toList());
            case "e"      -> new E(d.exigencias() == null ? List.of()
                                 : d.exigencias().stream().map(Dtos::paraDominio).toList());
            case "exceto" -> new Exceto(paraDominio(d.filtro()));
            case "ate"    -> new Ate(d.ano());
            default -> throw new IllegalArgumentException("tipo de filtro desconhecido: " + d.tipo());
        };
    }
}
