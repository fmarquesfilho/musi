package br.ufrn.musi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.ufrn.musi.aplicacao.AnotacoesDeObras;
import br.ufrn.musi.aplicacao.CatalogoDeObras;
import br.ufrn.musi.aplicacao.DadosDaAnotacao;
import br.ufrn.musi.aplicacao.DadosDaObra;
import br.ufrn.musi.aplicacao.ErroDeAplicacao.Conflito;
import br.ufrn.musi.aplicacao.ErroDeAplicacao.EntradaInvalida;
import br.ufrn.musi.aplicacao.ErroDeAplicacao.NaoEncontrado;
import br.ufrn.musi.aplicacao.FiltroDeObras;
import br.ufrn.musi.aplicacao.PedidoDePagina;
import br.ufrn.musi.dominio.Dominio.Faceta;
import br.ufrn.musi.dominio.Dominio.Obra;
import br.ufrn.musi.dominio.Dominio.Ordenacao;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Os casos de uso com as portas em memória. Sem `@QuarkusTest`: as classes não dependem
 * do framework, então `new` basta. Os mesmos casos de CasosDeUsoTest.kt.
 */
class CasosDeUsoTest {

    static final Obra ASA_BRANCA = new Obra("obra-03", "Asa Branca", "Luiz Gonzaga", 1947,
        List.of(new Faceta("genero", "forro"), new Faceta("ritmo", "baiao")), null, null);

    final EmMemoria.Obras obras = new EmMemoria.Obras(ASA_BRANCA);
    final CatalogoDeObras catalogo = new CatalogoDeObras(obras);
    final AnotacoesDeObras anotacoes = new AnotacoesDeObras(obras, new EmMemoria.Anotacoes());

    @Test void criarValidaTodasAsRegrasDeUmaVez() {
        var erro = assertThrows(EntradaInvalida.class, () -> catalogo.criar(
            new DadosDaObra("", "Gil", 1500, List.of(new Faceta("Ritmo", "ijexa")), "nao-e-uuid", null)));
        assertEquals(4, erro.violacoes().size(), erro.violacoes().toString());
    }

    @Test void obraInexistenteEhNaoEncontrado() {
        assertThrows(NaoEncontrado.class, () -> catalogo.buscar("obra-99"));
        assertThrows(NaoEncontrado.class, () -> catalogo.remover("obra-99"));
    }

    @Test void faixaDeAnosInvertidaEhRecusada() {
        assertThrows(EntradaInvalida.class, () ->
            catalogo.listar(new FiltroDeObras(null, 1970, 1960, null), Ordenacao.TITULO, new PedidoDePagina(0, 20)));
    }

    @Test void tamanhoDePaginaTemTeto() {
        assertThrows(EntradaInvalida.class, () -> new PedidoDePagina(0, PedidoDePagina.MAXIMO + 1));
    }

    @Test void anotarObraInexistenteEhNaoEncontradoDaObra() {
        var erro = assertThrows(NaoEncontrado.class, () ->
            anotacoes.criar("obra-99", new DadosDaAnotacao(new Faceta("ritmo", "xote"), "ana")));
        assertEquals("obra `obra-99` não existe", erro.getMessage());
    }

    @Test void anotacaoSemCuradorEhRecusada() {
        assertThrows(EntradaInvalida.class, () ->
            anotacoes.criar("obra-03", new DadosDaAnotacao(new Faceta("ritmo", "xote"), " ")));
    }

    @Test void curadoresDiferentesDivergemMasOMesmoNaoRepete() {
        anotacoes.criar("obra-03", new DadosDaAnotacao(new Faceta("ritmo", "xote"), "ana"));
        anotacoes.criar("obra-03", new DadosDaAnotacao(new Faceta("ritmo", "baiao"), "bia"));
        assertThrows(Conflito.class, () ->
            anotacoes.criar("obra-03", new DadosDaAnotacao(new Faceta("ritmo", "xote"), "ana")));
    }
}
