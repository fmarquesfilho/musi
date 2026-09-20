package br.ufrn.musi.aplicacao;

import br.ufrn.musi.dominio.Dominio.Anotacao;
import java.util.Optional;

/** Porta de persistência das anotações. Toda operação é restrita à obra. */
public interface RepositorioDeAnotacoes {
    Pagina<Anotacao> listar(String obraId, FiltroDeAnotacoes filtro, PedidoDePagina pedido);
    Optional<Anotacao> buscar(String obraId, long id);
    Anotacao criar(String obraId, DadosDaAnotacao dados);
    Optional<Anotacao> atualizar(String obraId, long id, DadosDaAnotacao dados);
    boolean remover(String obraId, long id);
}
