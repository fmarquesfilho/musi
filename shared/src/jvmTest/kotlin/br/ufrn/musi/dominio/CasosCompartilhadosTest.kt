package br.ufrn.musi.dominio

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Casos de busca CARREGADOS de contratos/exemplos/, não transcritos.
 *
 * O `dominio_test.go` de `services/` e o `DominioTest.java` de `api-quarkus/` carregam os
 * MESMOS arquivos, então um caso novo alcança as três implementações de uma vez. Se
 * discordarem, uma delas está errada — ver docs/decisoes/0001-stacks-e-estrutura.md.
 *
 * Este teste vive em `jvmTest` (não em `commonTest`) porque ler arquivo é específico de
 * plataforma. A avaliação usa `satisfaz` e preserva a ordem do acervo — como as versões Go
 * e Java —, e não `buscar`, que ordena por título.
 */
class CasosCompartilhadosTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class FacetaJson(val dimensao: String, val valor: String)

    @Serializable
    private data class ObraJson(
        val id: String, val titulo: String, val artista: String, val ano: Int,
        val facetas: List<FacetaJson>,
        val mbid: String? = null, val mbidComposicao: String? = null,
    )

    @Serializable
    private data class AcervoJson(val obras: List<ObraJson>)

    @Serializable
    private data class FiltroJson(
        val tipo: String,
        val dimensao: String? = null, val valor: String? = null,
        val opcoes: List<FiltroJson>? = null, val exigencias: List<FiltroJson>? = null,
        val filtro: FiltroJson? = null, val ano: Int? = null,
    )

    @Serializable
    private data class CasoJson(val nome: String, val filtro: FiltroJson, val esperado: List<String>)

    @Serializable
    private data class CasosJson(val casos: List<CasoJson>)

    private fun ObraJson.paraDominio() =
        Obra(id, titulo, artista, ano, facetas.map { Faceta(it.dimensao, it.valor) }, mbid, mbidComposicao)

    private fun FiltroJson.paraDominio(): Filtro = when (tipo) {
        "tem"    -> Filtro.Tem(dimensao!!, valor!!)
        "ou"     -> Filtro.Ou(opcoes.orEmpty().map { it.paraDominio() })
        "e"      -> Filtro.E(exigencias.orEmpty().map { it.paraDominio() })
        "exceto" -> Filtro.Exceto(filtro!!.paraDominio())
        "ate"    -> Filtro.Ate(ano!!)
        else     -> error("tipo de filtro desconhecido: $tipo")
    }

    private fun exemplos(nome: String): String {
        var dir: File? = File(System.getProperty("user.dir"))
        while (dir != null) {
            val arquivo = File(dir, "contratos/exemplos/$nome")
            if (arquivo.exists()) return arquivo.readText()
            dir = dir.parentFile
        }
        error("não encontrei contratos/exemplos/$nome")
    }

    @Test
    fun casosCompartilhados() {
        val acervo = json.decodeFromString<AcervoJson>(exemplos("acervo.json")).obras.map { it.paraDominio() }
        val casos = json.decodeFromString<CasosJson>(exemplos("casos-de-busca.json")).casos

        assertTrue(casos.isNotEmpty(), "nenhum caso carregado de casos-de-busca.json")

        for (caso in casos) {
            val f = caso.filtro.paraDominio()
            val obtido = acervo.filter { it.satisfaz(f) }.map { it.id }
            assertEquals(caso.esperado, obtido, caso.nome)
        }
    }
}
