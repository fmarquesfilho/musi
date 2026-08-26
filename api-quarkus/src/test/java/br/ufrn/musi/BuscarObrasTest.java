package br.ufrn.musi;

import br.ufrn.musi.aplicacao.BuscarObras;
import br.ufrn.musi.aplicacao.FonteDeObras;
import br.ufrn.musi.dominio.Dominio.*;
import org.junit.jupiter.api.Test;
import java.util.List;

import static br.ufrn.musi.dominio.Dominio.satisfaz;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Repare que não há `@QuarkusTest`: é um teste de unidade comum, porque a classe
 * sob teste não depende de framework.
 */
class BuscarObrasTest {

    static final List<Obra> ACERVO = DominioTest.ACERVO;

    final FonteDeObras emMemoria = filtro ->
        ACERVO.stream().filter(o -> satisfaz(o, filtro)).toList();

    @Test void buscaPorRitmo() {
        var obras = new BuscarObras(emMemoria).executar(new Tem("ritmo", "baiao"));
        assertEquals(List.of("obra-03", "obra-05"), obras.stream().map(Obra::id).toList());
    }

    @Test void buscaComposta() {
        var filtro = new E(List.of(new Tem("genero", "mpb"), new Ate(1970)));
        assertEquals(2, new BuscarObras(emMemoria).executar(filtro).size());
    }
}
