package br.ufrn.musi.dominio;

import java.util.List;

/**
 * O domínio do MUSI em Java.
 *
 * É a mesma modelagem que existe em Kotlin, em `shared/`.
 *
 * A duplicação é proposital — ver docs/decisoes/0001-stacks-e-estrutura.md. A comparação
 * entre as duas linguagens é um importante conteúdo da disciplina, e importar o módulo Kotlin
 * diretamente anularia isso. O que impede as duas de divergirem são os casos de teste
 * compartilhados de `contratos/exemplos/`.
 *
 * Vale abrir este arquivo ao lado de `shared/src/commonMain/.../Dominio.kt`.
 */
public final class Dominio {
    private Dominio() {}

    /** Um par (dimensão, valor). A lista de dimensões não é fixa — ADR-0002. */
    public record Faceta(String dimensao, String valor) {}

    /**
     * `mbid` e `mbidComposicao` são a identidade global, preenchida na conciliação com
     * o MusicBrainz — ADR-0003. Nulos até a obra ser conciliada; a busca não depende deles.
     */
    public record Obra(String id, String titulo, String artista, int ano,
                       List<Faceta> facetas, String mbid, String mbidComposicao) {}

    /**
     * A busca é uma árvore — ADR-0002.
     *
     * `sealed` em Java 21 corresponde à `sealed interface` de Kotlin. Com pattern
     * matching em `switch`, o compilador exige que todos os casos sejam tratados —
     * a mesma garantia que o `when` exaustivo dá do outro lado.
     */
    public sealed interface Filtro permits Tem, Ou, E, Exceto, Ate {}

    public record Tem(String dimensao, String valor) implements Filtro {}
    public record Ou(List<Filtro> opcoes)            implements Filtro {}
    public record E(List<Filtro> exigencias)         implements Filtro {}
    public record Exceto(Filtro filtro)              implements Filtro {}
    public record Ate(int ano)                       implements Filtro {}

    /**
     * Avalia o filtro contra uma obra.
     *
     * Não há `default:`, e essa é uma escolha deliberada: o switch é exaustivo sobre
     * a interface selada, então acrescentar um construtor faz este método deixar de
     * compilar, e o compilador indica onde falta tratamento.
     */
    public static boolean satisfaz(Obra obra, Filtro filtro) {
        return switch (filtro) {
            case Tem t -> obra.facetas().stream()
                              .anyMatch(f -> f.dimensao().equals(t.dimensao())
                                          && f.valor().equals(t.valor()));
            case Ou o     -> o.opcoes().stream().anyMatch(f -> satisfaz(obra, f));
            case E e      -> e.exigencias().stream().allMatch(f -> satisfaz(obra, f));
            case Exceto x -> !satisfaz(obra, x.filtro());
            case Ate a    -> obra.ano() <= a.ano();
        };
    }

    public static String descrever(Filtro filtro) {
        return switch (filtro) {
            case Tem t    -> t.dimensao() + "=" + t.valor();
            case Ou o     -> juntar(o.opcoes(), " ou ");
            case E e      -> juntar(e.exigencias(), " e ");
            case Exceto x -> "não " + descrever(x.filtro());
            case Ate a    -> "ano<=" + a.ano();
        };
    }

    private static String juntar(List<Filtro> filtros, String separador) {
        var sb = new StringBuilder("(");
        for (int i = 0; i < filtros.size(); i++) {
            if (i > 0) sb.append(separador);
            sb.append(descrever(filtros.get(i)));
        }
        return sb.append(")").toString();
    }

    /**
     * A ordenação é sempre declarada por quem consulta — ADR-0002.
     */
    public enum Ordenacao { TITULO, ARTISTA, ANO_CRESCENTE, ANO_DECRESCENTE }
}
