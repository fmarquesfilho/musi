package br.ufrn.musi

import br.ufrn.musi.adaptadores.persistencia.ObrasSemBanco
import br.ufrn.musi.adaptadores.persistencia.AnotacoesSemBanco
import br.ufrn.musi.aplicacao.RepositorioDeAnotacoes
import br.ufrn.musi.aplicacao.RepositorioDeObras
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * As rotas, com as portas em memória: a semântica HTTP (status, cabeçalhos, problem
 * details) sem Docker, em milissegundos. SQL e restrições do banco ficam para
 * IntegracaoPostgresTest.
 */
class RotasTest {

    private fun ApplicationTestBuilder.app() = application { configurar(modulosEmMemoria(ASA_BRANCA, PONTEIO)) }

    private suspend fun ApplicationTestBuilder.postJson(caminho: String, corpo: String) =
        client.post(caminho) { contentType(ContentType.Application.Json); setBody(corpo) }

    @Test
    fun criarDevolve201ComLocation() = testApplication {
        app()
        val resposta = postJson("/obras", """{"titulo":"Beira Mar","artista":"Gilberto Gil","ano":1969,"facetas":[{"dimensao":"ritmo","valor":"ijexa"}]}""")
        assertEquals(HttpStatusCode.Created, resposta.status)
        val local = assertNotNull(resposta.headers[HttpHeaders.Location])
        assertEquals(HttpStatusCode.OK, client.get(local).status)
    }

    @Test
    fun entradaInvalidaDevolve422EmProblemDetails() = testApplication {
        app()
        val resposta = postJson("/obras", """{"titulo":"","artista":"Gil","ano":1500}""")
        assertEquals(HttpStatusCode.UnprocessableEntity, resposta.status)
        assertEquals(ContentType.Application.ProblemJson, resposta.contentType()?.withoutParameters())
        val corpo = resposta.bodyAsText()
        assertTrue("\"violacoes\":[\"título vazio\",\"ano 1500 fora de 1877..2100\"]" in corpo, corpo)
    }

    @Test
    fun jsonSemCampoObrigatorioDevolve400() = testApplication {
        app()
        assertEquals(HttpStatusCode.BadRequest, postJson("/obras", """{"titulo":"Beira Mar"}""").status)
    }

    @Test
    fun parametroComTipoErradoDevolve400() = testApplication {
        app()
        assertEquals(HttpStatusCode.BadRequest, client.get("/obras?pagina=abc").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/obras/obra-03/anotacoes/xyz").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/obras?ordem=relevancia").status)
    }

    @Test
    fun tamanhoAcimaDoTetoDevolve422() = testApplication {
        app()
        assertEquals(HttpStatusCode.UnprocessableEntity, client.get("/obras?tamanho=101").status)
    }

    @Test
    fun obraInexistenteDevolve404() = testApplication {
        app()
        assertEquals(HttpStatusCode.NotFound, client.get("/obras/obra-99").status)
        assertEquals(HttpStatusCode.NotFound, client.get("/obras/obra-99/anotacoes").status)
    }

    @Test
    fun anotacaoAninhadaCriaEDepoisRemove() = testApplication {
        app()
        val criada = postJson("/obras/obra-03/anotacoes", """{"faceta":{"dimensao":"ritmo","valor":"xote"},"curador":"ana"}""")
        assertEquals(HttpStatusCode.Created, criada.status)
        val local = assertNotNull(criada.headers[HttpHeaders.Location])
        assertTrue(local.startsWith("/obras/obra-03/anotacoes/"), local)

        val repetida = postJson("/obras/obra-03/anotacoes", """{"faceta":{"dimensao":"ritmo","valor":"xote"},"curador":"ana"}""")
        assertEquals(HttpStatusCode.Conflict, repetida.status)

        assertEquals(HttpStatusCode.NoContent, client.delete(local).status)
        assertEquals(HttpStatusCode.NotFound, client.get(local).status)
    }

    @Test
    fun buscaDelegadaContinuaFuncionando() = testApplication {
        app()
        val simples = client.get("/busca?dimensao=ritmo&valor=baiao")
        assertEquals(HttpStatusCode.OK, simples.status)
        assertEquals("max-age=60", simples.headers[HttpHeaders.CacheControl])
        assertTrue("obra-03" in simples.bodyAsText())

        assertEquals(HttpStatusCode.BadRequest, client.get("/busca").status)
        assertEquals(HttpStatusCode.UnprocessableEntity, postJson("/busca", """{"tipo":"talvez"}""").status)
    }

    @Test
    fun semBancoOCrudDevolve503EABuscaFunciona() = testApplication {
        application {
            configurar(listOf(
                modulosEmMemoria(ASA_BRANCA).first(),
                module {
                    single<RepositorioDeObras> { ObrasSemBanco }
                    single<RepositorioDeAnotacoes> { AnotacoesSemBanco }
                },
                casosDeUso,
            ))
        }
        assertEquals(HttpStatusCode.ServiceUnavailable, client.get("/obras").status)
        assertEquals(HttpStatusCode.OK, client.get("/busca?dimensao=ritmo&valor=baiao").status)
    }

    @Test
    fun openApiDescreveAsRotasComOsTiposCertos() = testApplication {
        app()
        val spec = client.get("/openapi.json").bodyAsText()
        listOf("/obras", "/obras/{id}", "/obras/{id}/anotacoes", "/obras/{id}/anotacoes/{anotacaoId}", "/busca")
            .forEach { assertTrue("\"$it\"" in spec, "falta $it no OpenAPI") }
        assertTrue("application/problem+json" in spec)
        assertEquals(HttpStatusCode.OK, client.get("/swagger").status)
    }
}
