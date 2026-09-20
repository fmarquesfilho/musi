package br.ufrn.musi.adaptadores.persistencia;

import br.ufrn.musi.aplicacao.DadosDaAnotacao;
import br.ufrn.musi.aplicacao.FiltroDeAnotacoes;
import br.ufrn.musi.aplicacao.Pagina;
import br.ufrn.musi.aplicacao.PedidoDePagina;
import br.ufrn.musi.aplicacao.RepositorioDeAnotacoes;
import br.ufrn.musi.dominio.Dominio.Anotacao;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;

/** A porta `RepositorioDeAnotacoes` sobre PostgreSQL. Toda consulta é restrita à obra. */
@ApplicationScoped
@Typed(AnotacoesPanache.class)   // a porta é fornecida por Repositorios, que escolhe esta ou a sem banco
public class AnotacoesPanache implements RepositorioDeAnotacoes, PanacheRepositoryBase<AnotacaoEntidade, Long> {

    @Override
    public Pagina<Anotacao> listar(String obraId, FiltroDeAnotacoes filtro, PedidoDePagina pedido) {
        var onde = new StringBuilder("obra.id = :obraId");
        var params = new HashMap<String, Object>();
        params.put("obraId", obraId);
        if (filtro.curador() != null) {
            onde.append(" and curador = :curador");
            params.put("curador", filtro.curador());
        }
        if (filtro.dimensao() != null) {
            onde.append(" and dimensao = :dimensao");
            params.put("dimensao", filtro.dimensao());
        }
        long total = count(onde.toString(), params);
        var itens = find(onde + " order by criadoEm, id", params)
            .page(Page.of(pedido.pagina(), pedido.tamanho()))
            .list().stream().map(AnotacaoEntidade::paraAnotacao).toList();
        return new Pagina<>(itens, pedido, total);
    }

    @Override
    public Optional<Anotacao> buscar(String obraId, long id) {
        return daObra(obraId, id).map(AnotacaoEntidade::paraAnotacao);
    }

    @Override
    @Transactional
    public Anotacao criar(String obraId, DadosDaAnotacao dados) {
        var entidade = new AnotacaoEntidade();
        // Referência sem SELECT: só a chave estrangeira interessa.
        entidade.obra = getEntityManager().getReference(ObraEntidade.class, obraId);
        preencher(entidade, dados);
        try {
            persist(entidade);
            flush();
        } catch (ConstraintViolationException e) {
            throw Conflitos.traduzir(e);
        }
        return entidade.paraAnotacao();
    }

    @Override
    @Transactional
    public Optional<Anotacao> atualizar(String obraId, long id, DadosDaAnotacao dados) {
        var entidade = daObra(obraId, id);
        if (entidade.isEmpty()) return Optional.empty();
        preencher(entidade.get(), dados);
        try {
            flush();
        } catch (ConstraintViolationException e) {
            throw Conflitos.traduzir(e);
        }
        return entidade.map(AnotacaoEntidade::paraAnotacao);
    }

    @Override
    @Transactional
    public boolean remover(String obraId, long id) {
        return delete("id = ?1 and obra.id = ?2", id, obraId) > 0;
    }

    private Optional<AnotacaoEntidade> daObra(String obraId, long id) {
        return find("id = ?1 and obra.id = ?2", id, obraId).firstResultOptional();
    }

    private static void preencher(AnotacaoEntidade e, DadosDaAnotacao d) {
        e.dimensao = d.faceta().dimensao();
        e.valor = d.faceta().valor();
        e.curador = d.curador();
    }
}
