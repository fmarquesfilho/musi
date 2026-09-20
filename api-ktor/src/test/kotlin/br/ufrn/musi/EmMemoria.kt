package br.ufrn.musi

import br.ufrn.musi.aplicacao.BuscarObras
import br.ufrn.musi.aplicacao.Conflito
import br.ufrn.musi.aplicacao.DadosDaAnotacao
import br.ufrn.musi.aplicacao.DadosDaObra
import br.ufrn.musi.aplicacao.FiltroDeAnotacoes
import br.ufrn.musi.aplicacao.FiltroDeObras
import br.ufrn.musi.aplicacao.FonteDeObras
import br.ufrn.musi.aplicacao.Pagina
import br.ufrn.musi.aplicacao.PedidoDePagina
import br.ufrn.musi.aplicacao.RepositorioDeAnotacoes
import br.ufrn.musi.aplicacao.RepositorioDeObras
import br.ufrn.musi.dominio.Anotacao
import br.ufrn.musi.dominio.Faceta
import br.ufrn.musi.dominio.Filtro
import br.ufrn.musi.dominio.Obra
import br.ufrn.musi.dominio.Ordenacao
import br.ufrn.musi.dominio.buscar
import br.ufrn.musi.dominio.satisfaz
import org.koin.dsl.module
import kotlin.time.Clock

/**
 * Dublês (fakes) das portas, para testar casos de uso e rotas sem banco nem rede:
 * rodam em milissegundos e sem Docker. O comportamento com banco de verdade é conferido
 * em IntegracaoPostgresTest.
 */
class ObrasEmMemoria(vararg iniciais: Obra) : RepositorioDeObras {
    private val obras = iniciais.associateBy { it.id }.toMutableMap()
    private var proximo = 1

    override suspend fun listar(filtro: FiltroDeObras, ordenacao: Ordenacao, pedido: PedidoDePagina): Pagina<Obra> {
        val filtradas = obras.values.filter { o ->
            (filtro.artista == null || o.artista.contains(filtro.artista, ignoreCase = true)) &&
                (filtro.anoDe == null || o.ano >= filtro.anoDe) &&
                (filtro.anoAte == null || o.ano <= filtro.anoAte) &&
                (filtro.faceta == null || o.satisfaz(Filtro.Tem(filtro.faceta.dimensao, filtro.faceta.valor)))
        }.buscar(Filtro.E(emptyList()), ordenacao)
        return Pagina(filtradas.drop(pedido.deslocamento.toInt()).take(pedido.tamanho), pedido, filtradas.size.toLong())
    }

    override suspend fun buscar(id: String) = obras[id]
    override suspend fun existe(id: String) = id in obras

    override suspend fun criar(dados: DadosDaObra): Obra {
        exigirMbidLivre(dados.mbid, null)
        return dados.paraObra("mem-${proximo++}").also { obras[it.id] = it }
    }

    override suspend fun atualizar(id: String, dados: DadosDaObra): Obra? {
        if (id !in obras) return null
        exigirMbidLivre(dados.mbid, id)
        return dados.paraObra(id).also { obras[id] = it }
    }

    override suspend fun remover(id: String) = obras.remove(id) != null

    private fun exigirMbidLivre(mbid: String?, excetoId: String?) {
        if (mbid != null && obras.values.any { it.mbid == mbid && it.id != excetoId })
            throw Conflito("já existe obra com este mbid")
    }

    private fun DadosDaObra.paraObra(id: String) = Obra(id, titulo, artista, ano, facetas, mbid, mbidComposicao)
}

class AnotacoesEmMemoria : RepositorioDeAnotacoes {
    private val anotacoes = mutableListOf<Anotacao>()
    private var proximo = 1L

    override suspend fun listar(obraId: String, filtro: FiltroDeAnotacoes, pedido: PedidoDePagina): Pagina<Anotacao> {
        val filtradas = anotacoes.filter {
            it.obraId == obraId &&
                (filtro.curador == null || it.curador == filtro.curador) &&
                (filtro.dimensao == null || it.faceta.dimensao == filtro.dimensao)
        }
        return Pagina(filtradas.drop(pedido.deslocamento.toInt()).take(pedido.tamanho), pedido, filtradas.size.toLong())
    }

    override suspend fun buscar(obraId: String, id: Long) = anotacoes.find { it.id == id && it.obraId == obraId }

    override suspend fun criar(obraId: String, dados: DadosDaAnotacao): Anotacao {
        exigirUnica(obraId, dados, null)
        return Anotacao(proximo++, obraId, dados.faceta, dados.curador, Clock.System.now()).also { anotacoes += it }
    }

    override suspend fun atualizar(obraId: String, id: Long, dados: DadosDaAnotacao): Anotacao? {
        val atual = buscar(obraId, id) ?: return null
        exigirUnica(obraId, dados, id)
        return atual.copy(faceta = dados.faceta, curador = dados.curador).also { anotacoes[anotacoes.indexOf(atual)] = it }
    }

    override suspend fun remover(obraId: String, id: Long) = anotacoes.removeIf { it.id == id && it.obraId == obraId }

    private fun exigirUnica(obraId: String, d: DadosDaAnotacao, excetoId: Long?) {
        if (anotacoes.any { it.obraId == obraId && it.curador == d.curador && it.faceta == d.faceta && it.id != excetoId })
            throw Conflito("este curador já anotou esta faceta nesta obra")
    }
}

/** O serviço Go substituído por uma lista: a busca avalia a árvore com o domínio. */
class FonteEmMemoria(private val acervo: List<Obra>) : FonteDeObras {
    override suspend fun buscar(filtro: Filtro) = acervo.filter { it.satisfaz(filtro) }
}

val ASA_BRANCA = Obra("obra-03", "Asa Branca", "Luiz Gonzaga", 1947,
    listOf(Faceta("genero", "forro"), Faceta("ritmo", "baiao")))
val PONTEIO = Obra("obra-01", "Ponteio", "Edu Lobo", 1967,
    listOf(Faceta("genero", "mpb"), Faceta("ritmo", "ponteio")), mbid = "11111111-1111-4111-8111-111111111111")

/** Os módulos de Koin com as portas em memória, no lugar do Go e do PostgreSQL. */
fun modulosEmMemoria(vararg obras: Obra) = listOf(
    module {
        single<FonteDeObras> { FonteEmMemoria(obras.toList()) }
        single { BuscarObras(get()) }
        single<RepositorioDeObras> { ObrasEmMemoria(*obras) }
        single<RepositorioDeAnotacoes> { AnotacoesEmMemoria() }
    },
    casosDeUso,
)
