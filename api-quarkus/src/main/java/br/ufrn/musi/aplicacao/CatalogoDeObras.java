package br.ufrn.musi.aplicacao;

import br.ufrn.musi.aplicacao.ErroDeAplicacao.EntradaInvalida;
import br.ufrn.musi.aplicacao.ErroDeAplicacao.NaoEncontrado;
import br.ufrn.musi.dominio.Dominio;
import br.ufrn.musi.dominio.Dominio.Obra;
import br.ufrn.musi.dominio.Dominio.Ordenacao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Casos de uso do CRUD de obras: valida com as regras do domínio antes de chegar ao
 * banco e transforma "não achei" em erro com nome. Mesmo papel de CatalogoDeObras.kt.
 *
 * `jakarta.enterprise`/`jakarta.inject` são da especificação CDI, não do Quarkus; o teste
 * de arquitetura permite essas duas e recusa o resto (ArquiteturaTest).
 */
@ApplicationScoped
public class CatalogoDeObras {

    private static final Pattern MBID =
        Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    private final RepositorioDeObras obras;

    @Inject
    public CatalogoDeObras(RepositorioDeObras obras) {
        this.obras = obras;
    }

    public Pagina<Obra> listar(FiltroDeObras filtro, Ordenacao ordenacao, PedidoDePagina pedido) {
        if (filtro.anoDe() != null && filtro.anoAte() != null && filtro.anoDe() > filtro.anoAte())
            throw new EntradaInvalida(List.of("anoDe maior que anoAte"));
        return obras.listar(filtro, ordenacao, pedido);
    }

    public Obra buscar(String id) {
        return obras.buscar(id).orElseThrow(() -> new NaoEncontrado("obra", id));
    }

    public Obra criar(DadosDaObra dados) {
        validar(dados);
        return obras.criar(dados);
    }

    public Obra atualizar(String id, DadosDaObra dados) {
        validar(dados);
        return obras.atualizar(id, dados).orElseThrow(() -> new NaoEncontrado("obra", id));
    }

    public void remover(String id) {
        if (!obras.remover(id)) throw new NaoEncontrado("obra", id);
    }

    private void validar(DadosDaObra d) {
        var violacoes = new ArrayList<>(Dominio.violacoesDaObra(d.titulo(), d.artista(), d.ano(), d.facetas()));
        Stream.of(d.mbid(), d.mbidComposicao())
            .filter(m -> m != null && !MBID.matcher(m).matches())
            .forEach(m -> violacoes.add("MBID `" + m + "` não é um UUID"));
        if (!violacoes.isEmpty()) throw new EntradaInvalida(violacoes);
    }
}
