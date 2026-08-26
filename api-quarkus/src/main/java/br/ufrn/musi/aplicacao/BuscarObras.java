package br.ufrn.musi.aplicacao;

import br.ufrn.musi.dominio.Dominio.Filtro;
import br.ufrn.musi.dominio.Dominio.Obra;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

/**
 * `jakarta.enterprise.context` e `jakarta.inject` vêm da especificação CDI, do
 * Jakarta EE. Não são anotações do Quarkus: o mesmo símbolo é reconhecido por
 * qualquer implementação de CDI.
 *
 * Diferenças para Kotlin:
 *
 *   Quarkus + CDI  → o grafo é resolvido em tempo de compilação. Uma dependência
 *                    sem produtor faz o build falhar.
 *   Ktor + Koin    → o grafo é resolvido em tempo de execução, e um `single`
 *                    faltando aparece na inicialização.
 *
 * Nenhuma das duas é errada: uma troca verificação por explicitude, a outra o
 * contrário. Ver docs/decisoes/0001-stacks-e-estrutura.md.
 */
@ApplicationScoped
public class BuscarObras {

    private final FonteDeObras fonte;

    @Inject
    public BuscarObras(FonteDeObras fonte) {   // injeção por construtor
        this.fonte = fonte;
    }

    public List<Obra> executar(Filtro filtro) {
        return fonte.buscar(filtro);
    }
}
