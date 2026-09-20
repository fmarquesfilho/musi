package br.ufrn.musi.adaptadores.web

import br.ufrn.musi.adaptadores.busca.FiltroDto
import br.ufrn.musi.adaptadores.busca.ObraDto
import br.ufrn.musi.aplicacao.PedidoDePagina
import br.ufrn.musi.dominio.Ordenacao
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.Operation
import io.ktor.openapi.Parameters
import io.ktor.openapi.Responses
import io.ktor.openapi.jsonSchema

/**
 * A documentação OpenAPI das rotas — fora da árvore de rotas.
 *
 * Cada `val` é o bloco que o `.describe(...)` de UMA rota recebe. Mantê-los aqui deixa as
 * rotas com a árvore limpa sem abrir mão da geração a partir do código: mudou a rota, muda
 * o bloco aqui, e o spec acompanha — não há arquivo estático que possa divergir.
 *
 * Todas as respostas de erro estão declaradas, com o media type `application/problem+json`.
 */
private typealias Descricao = Operation.Builder.() -> Unit

object Doc {

    val health: Descricao = {
        summary = "Verificação de saúde"
        tag("operação")
        responses { HttpStatusCode.OK { description = "No ar" } }
    }

    // ---------------------------------------------------------------- busca (Go)

    val buscaSimples: Descricao = {
        summary = "Busca simples por uma faceta"
        description = "Filtro `Tem(dimensao, valor)`, avaliado pelo serviço Go. Para " +
            "buscas compostas (E, OU, EXCETO, ATÉ), use POST /busca."
        tag("busca")
        parameters {
            query("dimensao") { description = "A dimensão da faceta, ex.: `ritmo`."; required = true; schema = jsonSchema<String>() }
            query("valor") { description = "O valor da faceta, ex.: `baiao`."; required = true; schema = jsonSchema<String>() }
        }
        responses {
            HttpStatusCode.OK { description = "Obras que têm a faceta pedida"; schema = jsonSchema<List<ObraDto>>() }
            problema(HttpStatusCode.BadRequest, "Falta `dimensao` ou `valor`")
        }
    }

    val buscaComposta: Descricao = {
        summary = "Busca composta pela árvore de filtro"
        description = "A árvore de filtro chega no corpo (E, OU, EXCETO, ATÉ, TEM) e é " +
            "avaliada pelo serviço Go. Espelha contratos/filtro.schema.json."
        tag("busca")
        requestBody { required = true; schema = jsonSchema<FiltroDto>() }
        responses {
            HttpStatusCode.OK { description = "Obras que satisfazem o filtro"; schema = jsonSchema<List<ObraDto>>() }
            problema(HttpStatusCode.BadRequest, "Corpo que não é JSON de filtro")
            problema(HttpStatusCode.UnprocessableEntity, "Filtro inválido, ex.: `tipo` desconhecido")
        }
    }

    // ---------------------------------------------------------------- obras

    val listarObras: Descricao = {
        summary = "Lista as obras, paginado e filtrado"
        description = "Filtros combinados com E. A paginação e os filtros vão para o SQL."
        tag("obras")
        parameters {
            paginacao()
            query("ordem") {
                description = "Critério de ordenação, declarado por quem consulta (ADR-0002): " +
                    Ordenacao.entries.joinToString { "`${it.paraParametro()}`" } + ". Padrão: `titulo`."
                schema = jsonSchema<String>()
            }
            query("artista") { description = "Contém este texto, sem diferenciar maiúsculas."; schema = jsonSchema<String>() }
            query("anoDe") { description = "Ano mínimo, inclusive."; schema = jsonSchema<Int>() }
            query("anoAte") { description = "Ano máximo, inclusive."; schema = jsonSchema<Int>() }
            query("dimensao") { description = "Tem a faceta (`dimensao`, `valor`). Vem junto com `valor`."; schema = jsonSchema<String>() }
            query("valor") { description = "Ver `dimensao`."; schema = jsonSchema<String>() }
        }
        responses {
            HttpStatusCode.OK { description = "Uma página de obras"; schema = jsonSchema<PaginaDeObrasDto>() }
            problema(HttpStatusCode.BadRequest, "Parâmetro com tipo errado, ou `dimensao` sem `valor`")
            problema(HttpStatusCode.UnprocessableEntity, "Página ou tamanho fora da faixa; `anoDe` > `anoAte`")
            semBanco()
        }
    }

    val criarObra: Descricao = {
        summary = "Cria uma obra"
        description = "O `id` é atribuído pelo servidor e volta no cabeçalho `Location`."
        tag("obras")
        requestBody { required = true; schema = jsonSchema<NovaObraDto>() }
        responses {
            HttpStatusCode.Created {
                description = "Criada"
                schema = jsonSchema<ObraDto>()
                headers { header("Location") { description = "/obras/{id} da obra criada"; schema = jsonSchema<String>() } }
            }
            escrita()
        }
    }

    val buscarObra: Descricao = {
        summary = "Busca uma obra pelo id"
        tag("obras")
        parameters { idDaObra() }
        responses {
            HttpStatusCode.OK { schema = jsonSchema<ObraDto>() }
            problema(HttpStatusCode.NotFound, "Obra inexistente")
            semBanco()
        }
    }

    val atualizarObra: Descricao = {
        summary = "Substitui uma obra"
        description = "PUT substitui a obra inteira, facetas incluídas."
        tag("obras")
        parameters { idDaObra() }
        requestBody { required = true; schema = jsonSchema<NovaObraDto>() }
        responses {
            HttpStatusCode.OK { schema = jsonSchema<ObraDto>() }
            problema(HttpStatusCode.NotFound, "Obra inexistente")
            escrita()
        }
    }

    val removerObra: Descricao = {
        summary = "Remove uma obra e as anotações dela"
        tag("obras")
        parameters { idDaObra() }
        responses {
            HttpStatusCode.NoContent { description = "Removida" }
            problema(HttpStatusCode.NotFound, "Obra inexistente")
            semBanco()
        }
    }

    // ---------------------------------------------------------------- anotações

    val listarAnotacoes: Descricao = {
        summary = "Lista as anotações de uma obra, paginado"
        description = "Em ordem de criação. Anotações divergentes de curadores diferentes coexistem."
        tag("anotações")
        parameters {
            idDaObra()
            paginacao()
            query("curador") { description = "Só as deste curador."; schema = jsonSchema<String>() }
            query("dimensao") { description = "Só as desta dimensão."; schema = jsonSchema<String>() }
        }
        responses {
            HttpStatusCode.OK { schema = jsonSchema<PaginaDeAnotacoesDto>() }
            problema(HttpStatusCode.BadRequest, "Parâmetro com tipo errado")
            problema(HttpStatusCode.NotFound, "Obra inexistente")
            problema(HttpStatusCode.UnprocessableEntity, "Página ou tamanho fora da faixa")
            semBanco()
        }
    }

    val criarAnotacao: Descricao = {
        summary = "Anota uma obra"
        description = "A anotação é assinada: `curador` é obrigatório. A data é do servidor."
        tag("anotações")
        parameters { idDaObra() }
        requestBody { required = true; schema = jsonSchema<NovaAnotacaoDto>() }
        responses {
            HttpStatusCode.Created {
                description = "Criada"
                schema = jsonSchema<AnotacaoDto>()
                headers { header("Location") { description = "/obras/{id}/anotacoes/{anotacaoId}"; schema = jsonSchema<String>() } }
            }
            problema(HttpStatusCode.NotFound, "Obra inexistente")
            escrita()
        }
    }

    val buscarAnotacao: Descricao = {
        summary = "Busca uma anotação da obra"
        tag("anotações")
        parameters { idDaObra(); idDaAnotacao() }
        responses {
            HttpStatusCode.OK { schema = jsonSchema<AnotacaoDto>() }
            problema(HttpStatusCode.BadRequest, "`anotacaoId` não é inteiro")
            problema(HttpStatusCode.NotFound, "Obra ou anotação inexistente")
            semBanco()
        }
    }

    val atualizarAnotacao: Descricao = {
        summary = "Substitui uma anotação da obra"
        tag("anotações")
        parameters { idDaObra(); idDaAnotacao() }
        requestBody { required = true; schema = jsonSchema<NovaAnotacaoDto>() }
        responses {
            HttpStatusCode.OK { schema = jsonSchema<AnotacaoDto>() }
            problema(HttpStatusCode.NotFound, "Obra ou anotação inexistente")
            escrita()
        }
    }

    val removerAnotacao: Descricao = {
        summary = "Remove uma anotação da obra"
        tag("anotações")
        parameters { idDaObra(); idDaAnotacao() }
        responses {
            HttpStatusCode.NoContent { description = "Removida" }
            problema(HttpStatusCode.BadRequest, "`anotacaoId` não é inteiro")
            problema(HttpStatusCode.NotFound, "Obra ou anotação inexistente")
            semBanco()
        }
    }
}

// ------------------------------------------------------------------ peças comuns

private fun Parameters.Builder.idDaObra() =
    path("id") { description = "Id da obra, ex.: `obra-03`."; schema = jsonSchema<String>() }

// Sem esta linha, o `{anotacaoId}` sairia como `string`: na rota ele chega como texto.
private fun Parameters.Builder.idDaAnotacao() =
    path("anotacaoId") { description = "Id da anotação."; schema = jsonSchema<Long>() }

private fun Parameters.Builder.paginacao() {
    query("pagina") { description = "Página, a partir de 0. Padrão: 0."; schema = jsonSchema<Int>() }
    query("tamanho") {
        description = "Itens por página, de 1 a ${PedidoDePagina.MAXIMO}. Padrão: ${PedidoDePagina.PADRAO}."
        schema = jsonSchema<Int>()
    }
}

private fun Responses.Builder.problema(status: HttpStatusCode, descricao: String) =
    status {
        description = descricao
        ContentType.Application.ProblemJson { schema = jsonSchema<Problema>() }
    }

/** As respostas de erro comuns a toda escrita (POST e PUT). */
private fun Responses.Builder.escrita() {
    problema(HttpStatusCode.BadRequest, "Corpo que não é JSON válido, ou sem campo obrigatório")
    problema(HttpStatusCode.Conflict, "Viola unicidade: `mbid` repetido, ou anotação repetida do mesmo curador")
    problema(HttpStatusCode.UnprocessableEntity, "Viola regra do domínio; a lista vem em `violacoes`")
    semBanco()
}

private fun Responses.Builder.semBanco() =
    problema(HttpStatusCode.ServiceUnavailable, "Instância sem banco configurado (ADR-0004)")
