package br.ufrn.musi.adaptadores.persistencia;

import br.ufrn.musi.aplicacao.RepositorioDeAnotacoes;
import br.ufrn.musi.aplicacao.RepositorioDeObras;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Quem fornece as portas de persistência: Panache, ou "sem banco" quando o Hibernate está
 * desligado (MUSI_COM_BANCO=false, ver application.properties).
 *
 * É a única escolha feita em execução, e é o equivalente dos módulos `persistenciaPostgres`
 * e `persistenciaSemBanco` de Modulos.kt: lá, a montagem do Koin; aqui, um produtor CDI.
 * ObrasPanache e AnotacoesPanache usam `@Typed` para não disputar a mesma porta com ele.
 */
public class Repositorios {

    @ConfigProperty(name = "quarkus.hibernate-orm.active", defaultValue = "true")
    boolean comBanco;

    @Produces
    @ApplicationScoped
    RepositorioDeObras obras(ObrasPanache panache) {
        return comBanco ? panache : new SemBanco.Obras();
    }

    @Produces
    @ApplicationScoped
    RepositorioDeAnotacoes anotacoes(AnotacoesPanache panache) {
        return comBanco ? panache : new SemBanco.Anotacoes();
    }
}
