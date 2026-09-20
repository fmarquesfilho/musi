package br.ufrn.musi

import br.ufrn.musi.adaptadores.persistencia.ConfigBanco
import br.ufrn.musi.adaptadores.web.AnotacaoDto
import br.ufrn.musi.adaptadores.web.PaginaDeAnotacoesDto
import br.ufrn.musi.adaptadores.web.PaginaDeObrasDto
import br.ufrn.musi.adaptadores.busca.ObraDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Tag
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * A API inteira contra um PostgreSQL de verdade, num container descartável.
 *
 * Confere o que os dublês em memória não alcançam: as migrações, o SQL da paginação e dos
 * filtros, a chave estrangeira e as restrições (`UNIQUE`, `CHECK`, `ON DELETE CASCADE`).
 *
 * Roda igual no Docker Desktop e no GitHub Actions: a URL vem do container, nunca de
 * configuração. Tag `integracao`: `-PsemDocker` a deixa de fora (ver build.gradle.kts).
 */
@Tag("integracao")
class IntegracaoPostgresTest {

    companion object {
        // Um banco para a classe inteira; cada teste cria as próprias obras e não depende
        // do que os outros gravaram, só do acervo de exemplo (V2).
        private val postgres = PostgreSQLContainer("postgres:17-alpine").apply { start() }
        private val config = ConfigBanco(postgres.jdbcUrl, postgres.username, postgres.password)
    }

    private fun ApplicationTestBuilder.appComBanco(): HttpClient {
        application { modulo("http://localhost:9090", config) }
        return createClient { install(ContentNegotiation) { json() } }
    }

    private suspend fun HttpClient.criarObra(corpo: String) =
        post("/obras") { contentType(ContentType.Application.Json); setBody(corpo) }

    private suspend fun HttpClient.anotar(obraId: String, corpo: String) =
        post("/obras/$obraId/anotacoes") { contentType(ContentType.Application.Json); setBody(corpo) }

    @Test
    fun migracoesCriamOEsquemaECarregamOAcervo() = testApplication {
        val cliente = appComBanco()
        val pagina = cliente.get("/obras?tamanho=100&artista=gonzaga").body<PaginaDeObrasDto>()
        val asaBranca = pagina.itens.single { it.id == "obra-03" }
        assertEquals(listOf("forro", "baiao", "sanfona"), asaBranca.facetas.map { it.valor })

        // O Flyway registra o que aplicou; rodar de novo (outro testApplication) não muda nada.
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { c ->
            val versoes = c.createStatement().executeQuery("SELECT version FROM flyway_schema_history ORDER BY installed_rank")
                .let { rs -> buildList { while (rs.next()) add(rs.getString(1)) } }
            assertEquals(listOf("1", "2"), versoes)
        }
    }

    @Test
    fun crudCompletoDeObra() = testApplication {
        val cliente = appComBanco()
        val criada = cliente.criarObra("""{"titulo":"Andar com Fé","artista":"Gilberto Gil","ano":1982,"facetas":[{"dimensao":"genero","valor":"mpb"}]}""")
        assertEquals(HttpStatusCode.Created, criada.status)
        val local = assertNotNull(criada.headers[HttpHeaders.Location])

        val atualizada = cliente.put(local) {
            contentType(ContentType.Application.Json)
            setBody("""{"titulo":"Andar com Fé","artista":"Gilberto Gil","ano":1982,"facetas":[{"dimensao":"ritmo","valor":"xote"}]}""")
        }.body<ObraDto>()
        assertEquals(listOf("xote"), atualizada.facetas.map { it.valor })
        assertEquals(atualizada, cliente.get(local).body<ObraDto>())

        assertEquals(HttpStatusCode.NoContent, cliente.delete(local).status)
        assertEquals(HttpStatusCode.NotFound, cliente.get(local).status)
        assertEquals(HttpStatusCode.NotFound, cliente.delete(local).status)
    }

    @Test
    fun paginacaoEFiltrosNoSql() = testApplication {
        val cliente = appComBanco()
        val porAno = cliente.get("/obras?ordem=ano-crescente&anoAte=1975&tamanho=2").body<PaginaDeObrasDto>()
        assertEquals(listOf("obra-03", "obra-01"), porAno.itens.map { it.id })
        assertTrue(porAno.total >= 4, "total ${porAno.total}")

        val segunda = cliente.get("/obras?ordem=ano-crescente&anoAte=1975&tamanho=2&pagina=1").body<PaginaDeObrasDto>()
        assertEquals(listOf("obra-02", "obra-05"), segunda.itens.map { it.id })

        val baiao = cliente.get("/obras?dimensao=ritmo&valor=baiao").body<PaginaDeObrasDto>()
        assertEquals(setOf("obra-03", "obra-05"), baiao.itens.map { it.id }.toSet())

        // `%` digitado é texto, não curinga.
        assertEquals(0, cliente.get("/obras?artista=%25").body<PaginaDeObrasDto>().total)
    }

    @Test
    fun mbidRepetidoEhConflito() = testApplication {
        val cliente = appComBanco()
        // obra-01 (V2) já tem este mbid: duplicata, ADR-0003. O UNIQUE do banco decide.
        val resposta = cliente.criarObra("""{"titulo":"Ponteio","artista":"Edu Lobo","ano":1967,"mbid":"11111111-1111-4111-8111-111111111111"}""")
        assertEquals(HttpStatusCode.Conflict, resposta.status)
        assertEquals(ContentType.Application.ProblemJson, resposta.contentType()?.withoutParameters())
    }

    @Test
    fun anotacoesSaoUmParaMuitosComChaveEstrangeira() = testApplication {
        val cliente = appComBanco()
        val obra = cliente.criarObra("""{"titulo":"Expresso 2222","artista":"Gilberto Gil","ano":1972}""").body<ObraDto>()

        val ana = cliente.anotar(obra.id, """{"faceta":{"dimensao":"ritmo","valor":"baiao"},"curador":"ana"}""")
        assertEquals(HttpStatusCode.Created, ana.status)
        assertEquals(HttpStatusCode.Created, cliente.anotar(obra.id, """{"faceta":{"dimensao":"ritmo","valor":"xote"},"curador":"bia"}""").status)
        assertEquals(HttpStatusCode.Conflict, cliente.anotar(obra.id, """{"faceta":{"dimensao":"ritmo","valor":"baiao"},"curador":"ana"}""").status)
        assertEquals(HttpStatusCode.NotFound, cliente.anotar("obra-99", """{"faceta":{"dimensao":"ritmo","valor":"baiao"},"curador":"ana"}""").status)

        val daAna = cliente.get("/obras/${obra.id}/anotacoes?curador=ana").body<PaginaDeAnotacoesDto>()
        assertEquals(1L, daAna.total)
        val anotacao = daAna.itens.single()
        assertEquals(anotacao, cliente.get(assertNotNull(ana.headers[HttpHeaders.Location])).body<AnotacaoDto>())

        // A anotação não aparece por outra obra: a rota aninhada restringe pela chave.
        assertEquals(HttpStatusCode.NotFound, cliente.get("/obras/obra-03/anotacoes/${anotacao.id}").status)

        // Remover a obra leva as anotações junto (ON DELETE CASCADE).
        assertEquals(HttpStatusCode.NoContent, cliente.delete("/obras/${obra.id}").status)
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { c ->
            val rs = c.prepareStatement("SELECT count(*) FROM anotacoes WHERE obra_id = ?").apply { setString(1, obra.id) }.executeQuery()
            rs.next()
            assertEquals(0, rs.getInt(1))
        }
    }
}
