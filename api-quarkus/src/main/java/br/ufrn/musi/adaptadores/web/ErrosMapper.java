package br.ufrn.musi.adaptadores.web;

import br.ufrn.musi.aplicacao.ErroDeAplicacao;
import br.ufrn.musi.aplicacao.ErroDeAplicacao.Conflito;
import br.ufrn.musi.aplicacao.ErroDeAplicacao.EntradaInvalida;
import br.ufrn.musi.aplicacao.ErroDeAplicacao.NaoEncontrado;
import br.ufrn.musi.aplicacao.ErroDeAplicacao.PersistenciaIndisponivel;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Exceção vira resposta, num lugar só. É o equivalente do StatusPages do Ktor (Erros.kt):
 * os casos de uso lançam erros com nome, e é aqui que cada nome ganha um status HTTP.
 *
 * `@ServerExceptionMapper` é a forma do Quarkus REST para `ExceptionMapper`: um método por
 * tipo de exceção, na mesma classe.
 */
public class ErrosMapper {

    private static final Logger LOG = Logger.getLogger(ErrosMapper.class);

    @Context
    UriInfo uri;

    /** Os erros da aplicação. O switch é exaustivo: a classe é `sealed`. */
    @ServerExceptionMapper
    public Response erroDeAplicacao(ErroDeAplicacao erro) {
        return switch (erro) {
            case EntradaInvalida e -> problema(422, "entrada-invalida", "Entrada inválida",
                "A entrada viola " + e.violacoes().size() + " regra(s) do domínio.", e.violacoes());
            case NaoEncontrado e -> problema(404, "nao-encontrado", "Recurso inexistente", e.getMessage(), null);
            case Conflito e -> problema(409, "conflito", "Conflito com o estado atual", e.getMessage(), null);
            case PersistenciaIndisponivel e -> problema(503, "sem-banco", "Persistência indisponível",
                e.getMessage() + ". A busca continua disponível em /busca.", null);
        };
    }

    /** Bean Validation: falta campo obrigatório no corpo. É a FORMA do JSON, então 400. */
    @ServerExceptionMapper
    public Response validacao(ConstraintViolationException e) {
        var violacoes = e.getConstraintViolations().stream()
            .map(v -> ultimoNo(v.getPropertyPath().toString()) + ": " + v.getMessage())
            .sorted().toList();
        return problema(400, "requisicao-malformada", "Requisição malformada",
            "O corpo não tem a forma esperada.", violacoes);
    }

    /** Corpo que não é JSON, ou com tipo errado num campo. */
    @ServerExceptionMapper
    public Response jsonInvalido(JsonProcessingException e) {
        return problema(400, "requisicao-malformada", "Requisição malformada", e.getOriginalMessage(), null);
    }

    /** O que o próprio Quarkus REST lança (404 de rota inexistente, 405, 415...), no mesmo formato. */
    @ServerExceptionMapper
    public Response web(WebApplicationException e) {
        int status = e.getResponse().getStatus();
        return problema(status, "http-" + status, e.getResponse().getStatusInfo().getReasonPhrase(), e.getMessage(), null);
    }

    /** A árvore de filtro com `tipo` desconhecido (Dtos.paraDominio, adaptadores/busca). */
    @ServerExceptionMapper
    public Response filtroInvalido(IllegalArgumentException e) {
        return problema(422, "filtro-invalido", "Filtro inválido", e.getMessage(), null);
    }

    @ServerExceptionMapper
    public Response inesperado(RuntimeException e) {
        LOG.error("erro não tratado", e);
        return problema(500, "interno", "Erro interno", null, null);
    }

    private Response problema(int status, String tipo, String titulo, String detalhe, List<String> violacoes) {
        return Problema.resposta(status, tipo, titulo, detalhe, "/" + uri.getPath().replaceFirst("^/", ""), violacoes);
    }

    private static String ultimoNo(String caminho) {
        return caminho.substring(caminho.lastIndexOf('.') + 1);   // `criar.arg0.ano` → `ano`
    }
}
