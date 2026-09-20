package br.ufrn.musi.adaptadores.persistencia;

import br.ufrn.musi.aplicacao.DadosDaAnotacao;
import br.ufrn.musi.aplicacao.DadosDaObra;
import br.ufrn.musi.aplicacao.ErroDeAplicacao.PersistenciaIndisponivel;
import br.ufrn.musi.aplicacao.FiltroDeAnotacoes;
import br.ufrn.musi.aplicacao.FiltroDeObras;
import br.ufrn.musi.aplicacao.Pagina;
import br.ufrn.musi.aplicacao.PedidoDePagina;
import br.ufrn.musi.aplicacao.RepositorioDeAnotacoes;
import br.ufrn.musi.aplicacao.RepositorioDeObras;
import br.ufrn.musi.dominio.Dominio.Anotacao;
import br.ufrn.musi.dominio.Dominio.Obra;
import br.ufrn.musi.dominio.Dominio.Ordenacao;
import java.util.Optional;

/**
 * As portas quando não há banco (MUSI_COM_BANCO=false): o deploy no Render até a Sprint 3,
 * ADR-0004. Toda operação lança `PersistenciaIndisponivel`, que a borda responde como 503.
 * Mesmo papel de SemBanco.kt do lado Ktor.
 */
final class SemBanco {
    private SemBanco() {}

    static final class Obras implements RepositorioDeObras {
        @Override public Pagina<Obra> listar(FiltroDeObras f, Ordenacao o, PedidoDePagina p) { throw new PersistenciaIndisponivel(); }
        @Override public Optional<Obra> buscar(String id) { throw new PersistenciaIndisponivel(); }
        @Override public boolean existe(String id) { throw new PersistenciaIndisponivel(); }
        @Override public Obra criar(DadosDaObra d) { throw new PersistenciaIndisponivel(); }
        @Override public Optional<Obra> atualizar(String id, DadosDaObra d) { throw new PersistenciaIndisponivel(); }
        @Override public boolean remover(String id) { throw new PersistenciaIndisponivel(); }
    }

    static final class Anotacoes implements RepositorioDeAnotacoes {
        @Override public Pagina<Anotacao> listar(String o, FiltroDeAnotacoes f, PedidoDePagina p) { throw new PersistenciaIndisponivel(); }
        @Override public Optional<Anotacao> buscar(String o, long id) { throw new PersistenciaIndisponivel(); }
        @Override public Anotacao criar(String o, DadosDaAnotacao d) { throw new PersistenciaIndisponivel(); }
        @Override public Optional<Anotacao> atualizar(String o, long id, DadosDaAnotacao d) { throw new PersistenciaIndisponivel(); }
        @Override public boolean remover(String o, long id) { throw new PersistenciaIndisponivel(); }
    }
}
