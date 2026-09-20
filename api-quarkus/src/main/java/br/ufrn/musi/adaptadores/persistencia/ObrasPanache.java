package br.ufrn.musi.adaptadores.persistencia;

import br.ufrn.musi.aplicacao.DadosDaObra;
import br.ufrn.musi.aplicacao.FiltroDeObras;
import br.ufrn.musi.aplicacao.Pagina;
import br.ufrn.musi.aplicacao.PedidoDePagina;
import br.ufrn.musi.aplicacao.RepositorioDeObras;
import br.ufrn.musi.dominio.Dominio.Obra;
import br.ufrn.musi.dominio.Dominio.Ordenacao;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;

/**
 * A porta `RepositorioDeObras` sobre PostgreSQL, com Panache. Compare com ObrasPostgres.kt:
 * lá o SQL é montado no DSL do Exposed; aqui, em HQL, sobre as entidades.
 *
 * Escrita precisa de transação (`@Transactional`); leitura, não.
 */
@ApplicationScoped
@Typed(ObrasPanache.class)   // a porta é fornecida por Repositorios, que escolhe esta ou a sem banco
public class ObrasPanache implements RepositorioDeObras, PanacheRepositoryBase<ObraEntidade, String> {

    @Override
    public Pagina<Obra> listar(FiltroDeObras filtro, Ordenacao ordenacao, PedidoDePagina pedido) {
        var condicoes = new ArrayList<String>();
        var params = new HashMap<String, Object>();
        condicoes.add("1 = 1");
        if (filtro.artista() != null) {
            condicoes.add("lower(o.artista) like :artista escape '\\'");
            params.put("artista", "%" + escaparLike(filtro.artista().toLowerCase()) + "%");
        }
        if (filtro.anoDe() != null) {
            condicoes.add("o.ano >= :anoDe");
            params.put("anoDe", filtro.anoDe());
        }
        if (filtro.anoAte() != null) {
            condicoes.add("o.ano <= :anoAte");
            params.put("anoAte", filtro.anoAte());
        }
        // "Tem a faceta": subconsulta, porque faceta é linha, não coluna (ADR-0002).
        if (filtro.faceta() != null) {
            condicoes.add("exists (select 1 from ObraEntidade x join x.facetas f "
                + "where x.id = o.id and f.dimensao = :dimensao and f.valor = :valor)");
            params.put("dimensao", filtro.faceta().dimensao());
            params.put("valor", filtro.faceta().valor());
        }
        String onde = "from ObraEntidade o where " + String.join(" and ", condicoes);

        long total = count(onde, params);
        // page(...) vira LIMIT/OFFSET no SQL. O `id` desempata, para a paginação ser estável.
        var itens = find(onde + " order by " + ordem(ordenacao) + ", o.id", params)
            .page(Page.of(pedido.pagina(), pedido.tamanho()))
            .list().stream().map(ObraEntidade::paraObra).toList();
        return new Pagina<>(itens, pedido, total);
    }

    @Override
    public Optional<Obra> buscar(String id) {
        return findByIdOptional(id).map(ObraEntidade::paraObra);
    }

    @Override
    public boolean existe(String id) {
        return count("id", id) > 0;
    }

    @Override
    @Transactional
    public Obra criar(DadosDaObra dados) {
        var entidade = new ObraEntidade();
        entidade.id = UUID.randomUUID().toString();
        preencher(entidade, dados);
        try {
            persist(entidade);
            flush();   // a restrição do banco dispara aqui, dentro do try
        } catch (ConstraintViolationException e) {
            throw Conflitos.traduzir(e);
        }
        return entidade.paraObra();
    }

    @Override
    @Transactional
    public Optional<Obra> atualizar(String id, DadosDaObra dados) {
        var entidade = findById(id);
        if (entidade == null) return Optional.empty();
        // PUT substitui a obra inteira. Esvaziar e gravar antes de repor as facetas evita
        // violar a chave (obra_id, dimensao, valor) quando elas só trocam de posição.
        entidade.facetas.clear();
        try {
            flush();
            preencher(entidade, dados);
            flush();
        } catch (ConstraintViolationException e) {
            throw Conflitos.traduzir(e);
        }
        return Optional.of(entidade.paraObra());
    }

    // As anotações e as facetas vão junto pelo ON DELETE CASCADE das chaves estrangeiras.
    @Override
    @Transactional
    public boolean remover(String id) {
        return delete("id", id) > 0;
    }

    private static void preencher(ObraEntidade e, DadosDaObra d) {
        e.titulo = d.titulo();
        e.artista = d.artista();
        e.ano = d.ano();
        e.mbid = d.mbid() == null ? null : UUID.fromString(d.mbid());
        e.mbidComposicao = d.mbidComposicao() == null ? null : UUID.fromString(d.mbidComposicao());
        d.facetas().forEach(f -> e.facetas.add(new ObraEntidade.FacetaEmbutida(f)));
    }

    private static String ordem(Ordenacao o) {
        return switch (o) {
            case TITULO -> "o.titulo";
            case ARTISTA -> "o.artista";
            case ANO_CRESCENTE -> "o.ano";
            case ANO_DECRESCENTE -> "o.ano desc";
        };
    }

    /** `%` e `_` digitados por quem busca são texto, não curinga. */
    static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
