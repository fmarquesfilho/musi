package br.ufrn.musi.aplicacao;

import br.ufrn.musi.dominio.Dominio.Obra;
import br.ufrn.musi.dominio.Dominio.Ordenacao;
import java.util.Optional;

/**
 * Porta de persistência das obras. Como `FonteDeObras`, pertence a quem a usa: nenhuma
 * menção a JPA, Panache ou transação. Compare com RepositorioDeObras em Repositorios.kt:
 * lá as funções são `suspend`; aqui, bloqueantes.
 */
public interface RepositorioDeObras {
    Pagina<Obra> listar(FiltroDeObras filtro, Ordenacao ordenacao, PedidoDePagina pedido);
    Optional<Obra> buscar(String id);
    boolean existe(String id);
    Obra criar(DadosDaObra dados);
    /** Vazio se a obra não existe. */
    Optional<Obra> atualizar(String id, DadosDaObra dados);
    /** `false` se a obra não existe. Remove também as anotações dela. */
    boolean remover(String id);
}
