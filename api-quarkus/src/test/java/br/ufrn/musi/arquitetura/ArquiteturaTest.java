package br.ufrn.musi.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/**
 * A regra de dependência, como teste (ArchUnit, sobre o bytecode).
 *
 *     adaptadores  ──►  aplicacao  ──►  dominio
 *     (Quarkus REST,    (casos de uso,  (Java puro)
 *      Panache, JDBC)    portas)
 *
 * As setas só apontam para dentro. Mesmas regras de ArquiteturaTest.kt, do lado Ktor.
 * Diferença: aqui a aplicação pode usar `jakarta.inject` e `jakarta.enterprise` (CDI, uma
 * especificação, não o Quarkus); em Kotlin, o Koin fica todo fora, em Modulos.kt.
 */
@AnalyzeClasses(packages = "br.ufrn.musi", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

    @ArchTest
    static final ArchRule dominioSoDependeDoJava = regraDoDominio("br.ufrn.musi.dominio..");

    @ArchTest
    static final ArchRule aplicacaoNaoConheceFrameworkNemInfraestrutura = regraDaAplicacao("br.ufrn.musi.aplicacao..");

    @ArchTest
    static final ArchRule aplicacaoNaoConheceAdaptadores =
        noClasses().that().resideInAPackage("br.ufrn.musi.aplicacao..")
            .should().dependOnClassesThat().resideInAPackage("br.ufrn.musi.adaptadores..");

    @ArchTest
    static final ArchRule webNaoFalaComOBanco =
        noClasses().that().resideInAPackage("br.ufrn.musi.adaptadores.web..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "br.ufrn.musi.adaptadores.persistencia..", "jakarta.persistence..", "org.hibernate.orm..",
                "io.quarkus.hibernate..", "java.sql..");

    /**
     * A regra falha quando deve? `violacao.dominio.Contaminado` (só nos testes) importa
     * Jakarta REST de propósito. Se um dia a regra passar com ela, deixou de proteger.
     */
    @Test
    void aRegraDoDominioPegaUmaViolacao() {
        var fixture = new ClassFileImporter().importPackages("br.ufrn.musi.arquitetura.violacao");
        var resultado = regraDoDominio("br.ufrn.musi.arquitetura.violacao.dominio..").evaluate(fixture);
        assertTrue(resultado.hasViolation(), "a regra do domínio deveria acusar o import de Jakarta REST");
    }

    static ArchRule regraDoDominio(String pacote) {
        return classes().that().resideInAPackage(pacote)
            .should().onlyDependOnClassesThat().resideInAnyPackage(pacote, "java..")
            .because("o domínio é Java puro, sem framework nem infraestrutura");
    }

    static ArchRule regraDaAplicacao(String pacote) {
        return noClasses().that().resideInAPackage(pacote)
            .should().dependOnClassesThat().resideInAnyPackage(
                "io.quarkus..", "jakarta.ws.rs..", "jakarta.persistence..", "jakarta.transaction..",
                "org.hibernate..", "io.agroal..", "org.flywaydb..", "java.sql..", "javax.sql..",
                "com.fasterxml.jackson..", "org.eclipse.microprofile..")
            .because("casos de uso e portas não sabem de HTTP, banco nem JSON");
    }
}
