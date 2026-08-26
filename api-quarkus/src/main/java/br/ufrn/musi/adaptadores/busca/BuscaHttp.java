package br.ufrn.musi.adaptadores.busca;

import br.ufrn.musi.aplicacao.FonteDeObras;
import br.ufrn.musi.dominio.Dominio.Filtro;
import br.ufrn.musi.dominio.Dominio.Obra;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/** adaptador: implementa a porta usando o cliente declarativo. */
@ApplicationScoped
public class BuscaHttp implements FonteDeObras {

    private final ClienteBusca cliente;

    @Inject
    public BuscaHttp(@RestClient ClienteBusca cliente) {
        this.cliente = cliente;
    }

    @Override
    public List<Obra> buscar(Filtro filtro) {
        return cliente.buscar(Dtos.paraDto(filtro)).stream()
                      .map(Dtos::paraDominio)
                      .toList();
    }
}
