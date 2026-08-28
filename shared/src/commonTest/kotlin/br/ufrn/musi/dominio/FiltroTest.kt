package br.ufrn.musi.dominio

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Testes de domínio que NÃO dependem do acervo compartilhado, e por isso vivem em
 * `commonTest` (rodam em todos os alvos).
 *
 * Os casos de busca de contratos/exemplos/ são carregados e verificados em
 * `jvmTest/.../CasosCompartilhadosTest.kt` — ali, porque ler arquivo é específico de
 * plataforma. Este arquivo cobre a lógica pura, como `descrever`.
 */
class FiltroTest {

    @Test
    fun descreverAninhado() {
        val f = Filtro.E(listOf(
            Filtro.Ou(listOf(Filtro.Tem("ritmo", "ijexa"), Filtro.Tem("ritmo", "baiao"))),
            Filtro.Exceto(Filtro.Tem("genero", "forro"))
        ))
        assertEquals("((ritmo=ijexa ou ritmo=baiao) e não genero=forro)", descrever(f))
    }
}
