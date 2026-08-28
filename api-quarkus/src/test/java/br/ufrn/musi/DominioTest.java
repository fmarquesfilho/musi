package br.ufrn.musi;

import br.ufrn.musi.adaptadores.busca.Dtos;
import br.ufrn.musi.adaptadores.busca.Dtos.FiltroDto;
import br.ufrn.musi.adaptadores.busca.Dtos.ObraDto;
import br.ufrn.musi.dominio.Dominio.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static br.ufrn.musi.dominio.Dominio.descrever;
import static br.ufrn.musi.dominio.Dominio.satisfaz;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Os casos de busca são CARREGADOS de contratos/exemplos/, não transcritos. O
 * `FiltroTest.kt` de `shared/` e o `dominio_test.go` de `services/` carregam os MESMOS
 * arquivos, então um caso novo alcança as três implementações de uma vez. Se discordarem,
 * uma delas está errada — ver docs/decisoes/0001-stacks-e-estrutura.md.
 */
class DominioTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Acervo compartilhado, carregado uma vez. Usado tambem por BuscarObrasTest. */
    static final List<Obra> ACERVO = carregarAcervo();

    // Localiza contratos/exemplos/<nome> subindo a partir do diretorio do modulo.
    private static Path exemplos(String nome) {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            Path candidato = dir.resolve("contratos/exemplos/" + nome);
            if (candidato.toFile().exists()) {
                return candidato;
            }
        }
        throw new IllegalStateException("não encontrei contratos/exemplos/" + nome);
    }

    private static List<Obra> carregarAcervo() {
        try {
            JsonNode obras = JSON.readTree(exemplos("acervo.json").toFile()).get("obras");
            List<ObraDto> dtos = JSON.convertValue(obras,
                JSON.getTypeFactory().constructCollectionType(List.class, ObraDto.class));
            return dtos.stream().map(Dtos::paraDominio).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @TestFactory
    Stream<DynamicTest> casosCompartilhados() throws IOException {
        JsonNode casos = JSON.readTree(exemplos("casos-de-busca.json").toFile()).get("casos");
        return StreamSupport.stream(casos.spliterator(), false).map(caso -> {
            String nome = caso.get("nome").asText();
            return dynamicTest(nome, () -> {
                Filtro filtro = Dtos.paraDominio(JSON.convertValue(caso.get("filtro"), FiltroDto.class));
                List<String> esperado = JSON.convertValue(caso.get("esperado"),
                    JSON.getTypeFactory().constructCollectionType(List.class, String.class));
                List<String> obtido = ACERVO.stream().filter(o -> satisfaz(o, filtro)).map(Obra::id).toList();
                assertEquals(esperado, obtido, nome);
            });
        });
    }

    @Test void descreverAninhado() {
        var f = new E(List.of(
            new Ou(List.of(new Tem("ritmo", "ijexa"), new Tem("ritmo", "baiao"))),
            new Exceto(new Tem("genero", "forro"))));
        assertEquals("((ritmo=ijexa ou ritmo=baiao) e não genero=forro)", descrever(f));
    }
}
