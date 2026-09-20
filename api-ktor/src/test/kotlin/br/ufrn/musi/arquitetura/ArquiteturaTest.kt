package br.ufrn.musi.arquitetura

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * A regra de dependência, como teste (ArchUnit, sobre o bytecode).
 *
 *     adaptadores  ──►  aplicacao  ──►  dominio
 *     (Ktor, Exposed,   (casos de uso,  (Kotlin puro,
 *      JDBC, Koin)       portas)          em shared/)
 *
 * As setas só apontam para dentro. O domínio vem de `shared/`, e o compilador
 * multiplataforma já impede `commonMain` de importar Ktor; este teste cobre também a
 * camada de aplicação, que o compilador não protege, e roda no CI com os demais.
 *
 * O mesmo conjunto de regras existe em Java, em api-quarkus/.../ArquiteturaTest.java.
 */
class ArquiteturaTest {

    private val classes: JavaClasses = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .importPackages("br.ufrn.musi")

    @Test
    fun dominioSoDependeDaBibliotecaPadrao() = regraDoDominio("br.ufrn.musi.dominio..").check(classes)

    @Test
    fun aplicacaoNaoConheceFrameworkNemInfraestrutura() = regraDaAplicacao("br.ufrn.musi.aplicacao..").check(classes)

    @Test
    fun aplicacaoNaoConheceAdaptadores() =
        noClasses().that().resideInAPackage("br.ufrn.musi.aplicacao..")
            .should().dependOnClassesThat().resideInAPackage("br.ufrn.musi.adaptadores..")
            .check(classes)

    @Test
    fun webNaoFalaComOBanco() =
        noClasses().that().resideInAPackage("br.ufrn.musi.adaptadores.web..")
            .should().dependOnClassesThat().resideInAnyPackage("br.ufrn.musi.adaptadores.persistencia..", "org.jetbrains.exposed..", "java.sql..")
            .check(classes)

    /**
     * A regra falha quando deve? `violacao.dominio.Contaminado` (só nos testes) importa
     * Ktor de propósito. Se um dia a regra passar com ela, a regra deixou de proteger.
     */
    @Test
    fun aRegraDoDominioPegaUmaViolacao() {
        val fixture = ClassFileImporter().importPackages("br.ufrn.musi.arquitetura.violacao")
        val resultado = regraDoDominio("br.ufrn.musi.arquitetura.violacao.dominio..").evaluate(fixture)
        assertTrue(resultado.hasViolation(), "a regra do domínio deveria acusar o import de Ktor")
    }

    companion object {
        fun regraDoDominio(pacote: String): ArchRule =
            classes().that().resideInAPackage(pacote)
                .should().onlyDependOnClassesThat().resideInAnyPackage(pacote, "kotlin..", "java..", "org.jetbrains.annotations..")
                .because("o domínio é Kotlin puro: compila para Android, iOS e desktop (ADR-0001)")

        fun regraDaAplicacao(pacote: String): ArchRule =
            noClasses().that().resideInAPackage(pacote)
                .should().dependOnClassesThat().resideInAnyPackage(
                    "io.ktor..", "org.koin..", "org.jetbrains.exposed..", "java.sql..", "javax.sql..",
                    "com.zaxxer..", "org.flywaydb..", "org.postgresql..", "kotlinx.serialization..",
                )
                .because("casos de uso e portas não sabem de HTTP, banco nem injeção de dependência")
    }
}
