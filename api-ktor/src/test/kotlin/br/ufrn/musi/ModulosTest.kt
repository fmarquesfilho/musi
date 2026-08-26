package br.ufrn.musi

import org.koin.test.verify.verify
import kotlin.test.Test

/**
 * A mitigação do custo do Koin.
 *
 * Koin resolve dependências em tempo de execução: um `single` faltando não quebra a
 * compilação, e apareceria só na inicialização do servidor. `verify()` percorre o
 * grafo e falha se algo não puder ser resolvido.
 *
 * Não é verificação em tempo de compilação — mas move a descoberta do deploy para o
 * CI, que é onde ela custa menos.
 *
 * Ver docs/decisoes/0001-stacks-e-estrutura.md, seção Consequências.
 */
class ModulosTest {

    @Test
    fun grafoDeDependenciasResolve() {
        modulosDaAplicacao("http://localhost:9090").verify(
            extraTypes = listOf(io.ktor.client.engine.HttpClientEngine::class)
        )
    }
}
