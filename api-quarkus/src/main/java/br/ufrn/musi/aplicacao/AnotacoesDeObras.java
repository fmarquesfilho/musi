package br.ufrn.musi.aplicacao;

import br.ufrn.musi.aplicacao.ErroDeAplicacao.EntradaInvalida;
import br.ufrn.musi.aplicacao.ErroDeAplicacao.NaoEncontrado;
import br.ufrn.musi.dominio.Dominio;
import br.ufrn.musi.dominio.Dominio.Anotacao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;

/**
 * Casos de uso das anotações, sempre dentro de uma obra. Duas regras além da forma:
 * anotar obra inexistente é `NaoEncontrado` da OBRA; o mesmo curador não repete a mesma
 * faceta na mesma obra (`Conflito`, garantido pelo UNIQUE do banco).
 */
@ApplicationScoped
public class AnotacoesDeObras {

    private final RepositorioDeObras obras;
    private final RepositorioDeAnotacoes anotacoes;

    @Inject
    public AnotacoesDeObras(RepositorioDeObras obras, RepositorioDeAnotacoes anotacoes) {
        this.obras = obras;
        this.anotacoes = anotacoes;
    }

    public Pagina<Anotacao> listar(String obraId, FiltroDeAnotacoes filtro, PedidoDePagina pedido) {
        exigirObra(obraId);
        return anotacoes.listar(obraId, filtro, pedido);
    }

    public Anotacao buscar(String obraId, long id) {
        exigirObra(obraId);
        return anotacoes.buscar(obraId, id).orElseThrow(() -> new NaoEncontrado("anotação", id));
    }

    public Anotacao criar(String obraId, DadosDaAnotacao dados) {
        validar(dados);
        exigirObra(obraId);
        return anotacoes.criar(obraId, dados);
    }

    public Anotacao atualizar(String obraId, long id, DadosDaAnotacao dados) {
        validar(dados);
        exigirObra(obraId);
        return anotacoes.atualizar(obraId, id, dados).orElseThrow(() -> new NaoEncontrado("anotação", id));
    }

    public void remover(String obraId, long id) {
        exigirObra(obraId);
        if (!anotacoes.remover(obraId, id)) throw new NaoEncontrado("anotação", id);
    }

    private void exigirObra(String obraId) {
        if (!obras.existe(obraId)) throw new NaoEncontrado("obra", obraId);
    }

    private void validar(DadosDaAnotacao d) {
        var violacoes = new ArrayList<>(Dominio.violacoes(d.faceta()));
        violacoes.addAll(Dominio.violacoesDoCurador(d.curador()));
        if (!violacoes.isEmpty()) throw new EntradaInvalida(violacoes);
    }
}
