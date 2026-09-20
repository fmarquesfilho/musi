package br.ufrn.musi;

import br.ufrn.musi.aplicacao.FonteDeObras;
import br.ufrn.musi.dominio.Dominio;
import br.ufrn.musi.dominio.Dominio.Filtro;
import br.ufrn.musi.dominio.Dominio.Obra;
import io.quarkus.test.Mock;
import java.util.List;

/**
 * Nos `@QuarkusTest`, o serviço Go é substituído pelo acervo compartilhado de
 * contratos/exemplos/. `@Mock` é um `@Alternative` com prioridade, só nos testes:
 * substitui o `BuscaHttp` sem tocar no código de produção.
 */
@Mock
public class FonteEmMemoria implements FonteDeObras {
    @Override
    public List<Obra> buscar(Filtro filtro) {
        return DominioTest.ACERVO.stream().filter(o -> Dominio.satisfaz(o, filtro)).toList();
    }
}
