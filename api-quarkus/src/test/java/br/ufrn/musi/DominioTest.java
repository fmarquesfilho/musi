package br.ufrn.musi;

import br.ufrn.musi.dominio.Dominio.*;
import org.junit.jupiter.api.Test;
import java.util.List;

import static br.ufrn.musi.dominio.Dominio.descrever;
import static br.ufrn.musi.dominio.Dominio.satisfaz;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Este arquivo e o `FiltroTest.kt` de `shared/` verificam exatamente a mesma coisa,
 * em linguagens diferentes. Se os dois discordarem sobre um caso, uma das
 * implementações está errada.
 *
 * É esse mecanismo que permite manter as duas versões do domínio sem que elas
 * divirjam — ver docs/decisoes/0001-stacks-e-estrutura.md.
 */
class DominioTest {

    static final List<Obra> ACERVO = List.of(
        new Obra("obra-01", "Ponteio", "Edu Lobo", 1967, List.of(
            new Faceta("genero", "mpb"), new Faceta("ritmo", "ponteio"),
            new Faceta("movimento", "festivais-da-cancao"))),
        new Obra("obra-02", "Beira Mar", "Gilberto Gil", 1969, List.of(
            new Faceta("genero", "mpb"), new Faceta("ritmo", "ijexa"),
            new Faceta("movimento", "tropicalia"))),
        new Obra("obra-03", "Asa Branca", "Luiz Gonzaga", 1947, List.of(
            new Faceta("genero", "forro"), new Faceta("ritmo", "baiao"),
            new Faceta("instrumentacao", "sanfona"))),
        new Obra("obra-04", "Rio Grande", "Chico Science & Nacao Zumbi", 1994, List.of(
            new Faceta("ritmo", "maracatu"), new Faceta("movimento", "manguebeat"),
            new Faceta("regiao", "recife"))),
        new Obra("obra-05", "Refazenda", "Gilberto Gil", 1975, List.of(
            new Faceta("genero", "mpb"), new Faceta("ritmo", "baiao"))));

    List<String> ids(Filtro f) {
        return ACERVO.stream().filter(o -> satisfaz(o, f)).map(Obra::id).toList();
    }

    @Test void facetaSimples() {
        assertEquals(List.of("obra-03", "obra-05"), ids(new Tem("ritmo", "baiao")));
    }

    @Test void disjuncaoDeRitmos() {
        assertEquals(List.of("obra-02", "obra-04"),
            ids(new Ou(List.of(new Tem("ritmo", "ijexa"), new Tem("ritmo", "maracatu")))));
    }

    @Test void conjuncaoComExclusao() {
        assertEquals(List.of("obra-01", "obra-05"),
            ids(new E(List.of(new Tem("genero", "mpb"),
                              new Exceto(new Tem("ritmo", "ijexa"))))));
    }

    @Test void tresNiveisComCortePorAno() {
        assertEquals(List.of("obra-01"), ids(new E(List.of(
            new Ou(List.of(new Tem("movimento", "tropicalia"),
                           new Tem("movimento", "festivais-da-cancao"))),
            new Ate(1968),
            new Exceto(new Tem("ritmo", "ijexa"))))));
    }

    /** Casos de borda que costumam surpreender. Valem discussão em aula. */
    @Test void disjuncaoVaziaNaoAceitaNada() {
        assertEquals(List.of(), ids(new Ou(List.of())));
    }

    @Test void conjuncaoVaziaAceitaTudo() {
        assertEquals(5, ids(new E(List.of())).size());
    }

    @Test void dimensaoInexistenteNaoCasa() {
        assertEquals(List.of(), ids(new Tem("afinacao", "aberta")));
    }

    @Test void descreverAninhado() {
        var f = new E(List.of(
            new Ou(List.of(new Tem("ritmo", "ijexa"), new Tem("ritmo", "baiao"))),
            new Exceto(new Tem("genero", "forro"))));
        assertEquals("((ritmo=ijexa ou ritmo=baiao) e não genero=forro)", descrever(f));
    }
}
