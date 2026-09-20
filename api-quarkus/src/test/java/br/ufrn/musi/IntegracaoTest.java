package br.ufrn.musi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.sql.SQLException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * A API inteira contra um PostgreSQL de verdade. `@QuarkusTest` sobe a aplicação; sem URL
 * de banco no perfil de teste, o Dev Services sobe um PostgreSQL 17 num container (é o
 * Testcontainers por baixo) e o Flyway aplica as migrações — as mesmas da api-ktor.
 *
 * Os casos espelham IntegracaoPostgresTest.kt e RotasTest.kt. Tag `integracao`:
 * `-DexcludedGroups=integracao` a deixa de fora quando não há Docker.
 */
@QuarkusTest
@Tag("integracao")
class IntegracaoTest {

    @Inject
    AgroalDataSource banco;

    private static String criarObra(String corpo) {
        return given().contentType(ContentType.JSON).body(corpo)
            .when().post("/obras")
            .then().statusCode(201).extract().path("id");
    }

    @Test
    void migracoesCarregamOAcervo() throws SQLException {
        given().when().get("/obras/obra-03")
            .then().statusCode(200)
            .body("facetas.valor", contains("forro", "baiao", "sanfona"));
        try (var c = banco.getConnection();
             var rs = c.createStatement().executeQuery("SELECT string_agg(version, ',' ORDER BY installed_rank) FROM flyway_schema_history")) {
            rs.next();
            assertEquals("1,2", rs.getString(1));
        }
    }

    @Test
    void crudCompletoDeObra() {
        var local = given().contentType(ContentType.JSON)
            .body("{\"titulo\":\"Andar com Fé\",\"artista\":\"Gilberto Gil\",\"ano\":1982,\"facetas\":[{\"dimensao\":\"genero\",\"valor\":\"mpb\"}]}")
            .when().post("/obras")
            .then().statusCode(201).header("Location", containsString("/obras/"))
            .extract().header("Location");

        // Troca e inverte as facetas: o PUT substitui a lista inteira.
        given().contentType(ContentType.JSON)
            .body("{\"titulo\":\"Andar com Fé\",\"artista\":\"Gilberto Gil\",\"ano\":1982,\"facetas\":[{\"dimensao\":\"ritmo\",\"valor\":\"xote\"},{\"dimensao\":\"genero\",\"valor\":\"mpb\"}]}")
            .when().put(local).then().statusCode(200);
        given().when().get(local).then().statusCode(200).body("facetas.valor", contains("xote", "mpb"));

        given().when().delete(local).then().statusCode(204);
        given().when().get(local).then().statusCode(404);
        given().when().delete(local).then().statusCode(404);
    }

    @Test
    void paginacaoEFiltrosNoSql() {
        given().when().get("/obras?ordem=ano-crescente&anoAte=1975&tamanho=2")
            .then().statusCode(200).body("itens.id", contains("obra-03", "obra-01"));
        given().when().get("/obras?ordem=ano-crescente&anoAte=1975&tamanho=2&pagina=1")
            .then().statusCode(200).body("itens.id", contains("obra-02", "obra-05"));
        given().when().get("/obras?dimensao=ritmo&valor=baiao")
            .then().statusCode(200).body("itens.id", containsInAnyOrder("obra-03", "obra-05"));
        given().when().get("/obras?artista=%25").then().statusCode(200).body("total", equalTo(0));
    }

    @Test
    void entradaErradaTemStatusCerto() {
        given().contentType(ContentType.JSON).body("{\"titulo\":\"\",\"artista\":\"Gil\",\"ano\":1500}")
            .when().post("/obras")
            .then().statusCode(422).contentType("application/problem+json")
            .body("violacoes", contains("título vazio", "ano 1500 fora de 1877..2100"));
        given().contentType(ContentType.JSON).body("{\"titulo\":\"Beira Mar\"}")
            .when().post("/obras").then().statusCode(400).contentType("application/problem+json")
            .body("violacoes", contains("ano: obrigatório", "artista: obrigatório"));
        given().contentType(ContentType.JSON).body("{\"titulo\":")
            .when().post("/obras").then().statusCode(400).contentType("application/problem+json");
        given().when().get("/obras?pagina=abc").then().statusCode(400);
        given().when().get("/obras?ordem=relevancia").then().statusCode(400);
        given().when().get("/obras?tamanho=101").then().statusCode(422);
        given().when().get("/obras/obra-03/anotacoes/xyz").then().statusCode(400);
        given().when().get("/obras/obra-99").then().statusCode(404).contentType("application/problem+json");
    }

    @Test
    void mbidRepetidoEhConflito() {
        given().contentType(ContentType.JSON)
            .body("{\"titulo\":\"Ponteio\",\"artista\":\"Edu Lobo\",\"ano\":1967,\"mbid\":\"11111111-1111-4111-8111-111111111111\"}")
            .when().post("/obras")
            .then().statusCode(409).contentType("application/problem+json");
    }

    @Test
    void anotacoesSaoUmParaMuitosComChaveEstrangeira() throws SQLException {
        var obraId = criarObra("{\"titulo\":\"Expresso 2222\",\"artista\":\"Gilberto Gil\",\"ano\":1972}");
        var anotacao = "{\"faceta\":{\"dimensao\":\"ritmo\",\"valor\":\"baiao\"},\"curador\":\"ana\"}";

        var local = given().contentType(ContentType.JSON).body(anotacao)
            .when().post("/obras/" + obraId + "/anotacoes")
            .then().statusCode(201).body("criadoEm", startsWith("20"))
            .extract().header("Location");
        given().contentType(ContentType.JSON).body("{\"faceta\":{\"dimensao\":\"ritmo\",\"valor\":\"xote\"},\"curador\":\"bia\"}")
            .when().post("/obras/" + obraId + "/anotacoes").then().statusCode(201);
        given().contentType(ContentType.JSON).body(anotacao)
            .when().post("/obras/" + obraId + "/anotacoes").then().statusCode(409);
        given().contentType(ContentType.JSON).body(anotacao)
            .when().post("/obras/obra-99/anotacoes").then().statusCode(404);

        given().when().get("/obras/" + obraId + "/anotacoes?curador=ana")
            .then().statusCode(200).body("total", equalTo(1)).body("itens.curador", contains("ana"));
        given().when().get(local).then().statusCode(200).body("faceta.valor", equalTo("baiao"));
        int id = given().when().get(local).then().extract().path("id");
        given().when().get("/obras/obra-03/anotacoes/" + id).then().statusCode(404);

        // Remover a obra leva as anotações junto (ON DELETE CASCADE).
        given().when().delete("/obras/" + obraId).then().statusCode(204);
        try (var c = banco.getConnection();
             var ps = c.prepareStatement("SELECT count(*) FROM anotacoes WHERE obra_id = ?")) {
            ps.setString(1, obraId);
            var rs = ps.executeQuery();
            rs.next();
            assertEquals(0, rs.getInt(1));
        }
    }

    @Test
    void buscaDelegadaContinuaFuncionando() {
        given().when().get("/busca?dimensao=ritmo&valor=baiao")
            .then().statusCode(200).header("Cache-Control", containsString("max-age=60"))
            .body("id", contains("obra-03", "obra-05"));
        given().when().get("/busca").then().statusCode(400);
        given().contentType(ContentType.JSON).body("{\"tipo\":\"talvez\"}")
            .when().post("/busca").then().statusCode(422);
    }

    @Test
    void openApiDescreveAsRotas() {
        given().when().get("/q/openapi?format=json")
            .then().statusCode(200)
            .body("paths.keySet()", hasItems("/obras", "/obras/{id}", "/obras/{id}/anotacoes",
                "/obras/{id}/anotacoes/{anotacaoId}", "/busca"))
            .body("paths.'/obras/{id}/anotacoes/{anotacaoId}'.get.parameters.find { it.name == 'anotacaoId' }.schema.type",
                equalTo("integer"));
    }
}
