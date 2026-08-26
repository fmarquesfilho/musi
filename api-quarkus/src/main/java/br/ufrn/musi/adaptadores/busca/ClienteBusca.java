package br.ufrn.musi.adaptadores.busca;

import br.ufrn.musi.adaptadores.busca.Dtos.FiltroDto;
import br.ufrn.musi.adaptadores.busca.Dtos.ObraDto;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * CLIENTE HTTP como adaptador que fala com services/busca, em Go.
 *
 * Não há implementação: a interface é o contrato, e o Quarkus gera o código da
 * chamada em tempo de compilação.
 *
 * É o mesmo papel do `BuscaHttp` do lado Ktor, resolvido de outro jeito:
 *
 *   Quarkus  → interface anotada; a implementação é gerada
 *   Ktor     → classe que usa o HttpClient explicitamente
 */
@Path("/buscar")
@RegisterRestClient(configKey = "busca")
public interface ClienteBusca {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    List<ObraDto> buscar(FiltroDto filtro);
}
