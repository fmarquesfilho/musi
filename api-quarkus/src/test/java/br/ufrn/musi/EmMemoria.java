package br.ufrn.musi;

import br.ufrn.musi.aplicacao.DadosDaAnotacao;
import br.ufrn.musi.aplicacao.DadosDaObra;
import br.ufrn.musi.aplicacao.ErroDeAplicacao.Conflito;
import br.ufrn.musi.aplicacao.FiltroDeAnotacoes;
import br.ufrn.musi.aplicacao.FiltroDeObras;
import br.ufrn.musi.aplicacao.Pagina;
import br.ufrn.musi.aplicacao.PedidoDePagina;
import br.ufrn.musi.aplicacao.RepositorioDeAnotacoes;
import br.ufrn.musi.aplicacao.RepositorioDeObras;
import br.ufrn.musi.dominio.Dominio.Anotacao;
import br.ufrn.musi.dominio.Dominio.Obra;
import br.ufrn.musi.dominio.Dominio.Ordenacao;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Dublês (fakes) das portas, para testar os casos de uso sem banco: JUnit puro, sem
 * `@QuarkusTest`, em milissegundos. Mesmo papel de EmMemoria.kt do lado Ktor.
 */
final class EmMemoria {
    private EmMemoria() {}

    static final class Obras implements RepositorioDeObras {
        private final Map<String, Obra> obras = new LinkedHashMap<>();
        private int proximo = 1;

        Obras(Obra... iniciais) {
            for (Obra o : iniciais) obras.put(o.id(), o);
        }

        @Override public Pagina<Obra> listar(FiltroDeObras filtro, Ordenacao ordenacao, PedidoDePagina pedido) {
            var todas = new ArrayList<>(obras.values());
            int de = Math.min(pedido.pagina() * pedido.tamanho(), todas.size());
            return new Pagina<>(todas.subList(de, Math.min(de + pedido.tamanho(), todas.size())), pedido, todas.size());
        }
        @Override public Optional<Obra> buscar(String id) { return Optional.ofNullable(obras.get(id)); }
        @Override public boolean existe(String id) { return obras.containsKey(id); }
        @Override public Obra criar(DadosDaObra d) {
            if (d.mbid() != null && obras.values().stream().anyMatch(o -> d.mbid().equals(o.mbid())))
                throw new Conflito("já existe obra com este mbid");
            var o = obra("mem-" + proximo++, d);
            obras.put(o.id(), o);
            return o;
        }
        @Override public Optional<Obra> atualizar(String id, DadosDaObra d) {
            if (!obras.containsKey(id)) return Optional.empty();
            obras.put(id, obra(id, d));
            return Optional.of(obras.get(id));
        }
        @Override public boolean remover(String id) { return obras.remove(id) != null; }

        private static Obra obra(String id, DadosDaObra d) {
            return new Obra(id, d.titulo(), d.artista(), d.ano(), d.facetas(), d.mbid(), d.mbidComposicao());
        }
    }

    static final class Anotacoes implements RepositorioDeAnotacoes {
        private final List<Anotacao> anotacoes = new ArrayList<>();
        private long proximo = 1;

        @Override public Pagina<Anotacao> listar(String obraId, FiltroDeAnotacoes f, PedidoDePagina p) {
            var daObra = anotacoes.stream().filter(a -> a.obraId().equals(obraId)).toList();
            return new Pagina<>(daObra, p, daObra.size());
        }
        @Override public Optional<Anotacao> buscar(String obraId, long id) {
            return anotacoes.stream().filter(a -> a.id() == id && a.obraId().equals(obraId)).findFirst();
        }
        @Override public Anotacao criar(String obraId, DadosDaAnotacao d) {
            if (anotacoes.stream().anyMatch(a -> a.obraId().equals(obraId) && a.curador().equals(d.curador())
                    && Objects.equals(a.faceta(), d.faceta())))
                throw new Conflito("este curador já anotou esta faceta nesta obra");
            var a = new Anotacao(proximo++, obraId, d.faceta(), d.curador(), Instant.now());
            anotacoes.add(a);
            return a;
        }
        @Override public Optional<Anotacao> atualizar(String obraId, long id, DadosDaAnotacao d) {
            return buscar(obraId, id).map(a -> {
                var nova = new Anotacao(id, obraId, d.faceta(), d.curador(), a.criadoEm());
                anotacoes.set(anotacoes.indexOf(a), nova);
                return nova;
            });
        }
        @Override public boolean remover(String obraId, long id) {
            return anotacoes.removeIf(a -> a.id() == id && a.obraId().equals(obraId));
        }
    }
}
