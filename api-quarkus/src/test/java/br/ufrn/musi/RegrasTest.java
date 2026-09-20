package br.ufrn.musi;

import static br.ufrn.musi.dominio.Dominio.violacoes;
import static br.ufrn.musi.dominio.Dominio.violacoesDaObra;
import static br.ufrn.musi.dominio.Dominio.violacoesDoCurador;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.ufrn.musi.dominio.Dominio.Faceta;
import java.util.List;
import org.junit.jupiter.api.Test;

/** As regras de forma, sem banco nem HTTP. Os mesmos casos de RegrasTest.kt, em shared/. */
class RegrasTest {

    @Test void facetaNoPadraoNaoTemViolacao() {
        assertTrue(violacoes(new Faceta("instrumentacao", "viola-caipira")).isEmpty());
    }

    @Test void facetaComMaiusculaOuAcentoEhRecusada() {
        assertEquals(2, violacoes(new Faceta("Ritmo", "baião")).size());
    }

    @Test void obraValida() {
        assertTrue(violacoesDaObra("Asa Branca", "Luiz Gonzaga", 1947, List.of(new Faceta("ritmo", "baiao"))).isEmpty());
    }

    @Test void obraAcumulaTodasAsViolacoes() {
        var v = violacoesDaObra(" ", "", 1500, List.of(new Faceta("ritmo", "baiao"), new Faceta("ritmo", "baiao")));
        assertEquals(List.of("título vazio", "artista vazio", "ano 1500 fora de 1877..2100", "faceta repetida"), v);
    }

    @Test void anotacaoSemCuradorEhRecusada() {
        assertEquals(1, violacoesDoCurador("  ").size());
    }
}
