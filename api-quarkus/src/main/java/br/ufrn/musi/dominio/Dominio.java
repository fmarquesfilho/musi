package br.ufrn.musi.dominio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

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

    /**
     * Uma faceta atribuída a uma obra por um curador identificável — ver docs/DOMINIO.md.
     * Anotações divergentes de curadores diferentes coexistem.
     */
    public record Anotacao(long id, String obraId, Faceta faceta, String curador, Instant criadoEm) {}

    /*
     * As regras de forma do domínio, as mesmas de contratos/obra.schema.json e de
     * `Regras` no Dominio.kt. Devolvem a lista de violações em vez de lançar exceção:
     * quem valida uma entrada quer todas as mensagens de uma vez.
     */
    public static final Pattern TERMO = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    public static final int ANO_MINIMO = 1877;   // o fonógrafo
    public static final int ANO_MAXIMO = 2100;
    public static final int TAMANHO_MAXIMO_TEXTO = 300;
    public static final int TAMANHO_MAXIMO_TERMO = 60;
    public static final int TAMANHO_MAXIMO_CURADOR = 120;

    public static List<String> violacoes(Faceta f) {
        var v = new ArrayList<String>();
        if (!termoValido(f.dimensao()))
            v.add("dimensão `" + f.dimensao() + "` fora do padrão: minúsculas, sem acento, com hífen");
        if (!termoValido(f.valor()))
            v.add("valor `" + f.valor() + "` fora do padrão: minúsculas, sem acento, com hífen");
        return v;
    }

    /** Violações dos campos de uma obra, antes de ela ter identidade. */
    public static List<String> violacoesDaObra(String titulo, String artista, int ano, List<Faceta> facetas) {
        var v = new ArrayList<String>();
        if (titulo.isBlank()) v.add("título vazio");
        if (titulo.length() > TAMANHO_MAXIMO_TEXTO) v.add("título com mais de " + TAMANHO_MAXIMO_TEXTO + " caracteres");
        if (artista.isBlank()) v.add("artista vazio");
        if (artista.length() > TAMANHO_MAXIMO_TEXTO) v.add("artista com mais de " + TAMANHO_MAXIMO_TEXTO + " caracteres");
        if (ano < ANO_MINIMO || ano > ANO_MAXIMO) v.add("ano " + ano + " fora de " + ANO_MINIMO + ".." + ANO_MAXIMO);
        facetas.forEach(f -> v.addAll(violacoes(f)));
        if (new HashSet<>(facetas).size() != facetas.size()) v.add("faceta repetida");
        return v;
    }

    public static List<String> violacoesDoCurador(String curador) {
        var v = new ArrayList<String>();
        if (curador.isBlank()) v.add("curador vazio: toda anotação é assinada");
        if (curador.length() > TAMANHO_MAXIMO_CURADOR) v.add("curador com mais de " + TAMANHO_MAXIMO_CURADOR + " caracteres");
        return v;
    }

    private static boolean termoValido(String termo) {
        return TERMO.matcher(termo).matches() && termo.length() <= TAMANHO_MAXIMO_TERMO;
    }
}
