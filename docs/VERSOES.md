# Versões e atualizações

Três linguagens, três gerenciadores. O objetivo é que ninguém precise instalar a versão
certa à mão, e que atualizar não dependa de serviço externo.

## Onde cada versão é declarada

| O quê | Onde | Quem lê |
|---|---|---|
| Java | `gradle/libs.versions.toml`, chave `java` | toolchain do Gradle e o `pom.xml` |
| Kotlin, Ktor, Koin, Compose | `gradle/libs.versions.toml` | os módulos Gradle |
| Quarkus | `api-quarkus/pom.xml`, `quarkus.platform.version` | Maven |
| Go | `services/go.mod` | Go |
| Ferramentas de linha de comando | `mise.toml` | mise |
| Gradle | `gradle/wrapper/gradle-wrapper.properties` | o wrapper |

## Toolchain: o JDK não depende da sua máquina

```kotlin
kotlin { jvmToolchain(libs.versions.java.get().toInt()) }
```

O Gradle compila com o JDK declarado, não com o que estiver no `PATH`. Se ele não existir na
máquina, o plugin `foojay-resolver` baixa. Quem tem Java 17 instalado consegue construir o
projeto sem trocar nada.

No Maven, `maven.compiler.release` faz o mesmo para o bytecode gerado.

Isso resolve o problema clássico de "compila na minha máquina": o JDK passa a ser parte da
declaração do projeto.

## Verificar o que está atrasado

```bash
mise run desatualizado
```

Roda quatro comandos nativos, sem alterar nada:

| Comando | O que cobre |
|---|---|
| `mise outdated` | Java, Go, Gradle, Maven, Python |
| `./gradlew dependencyUpdates` | catálogo de versões do Kotlin |
| `mvn versions:display-dependency-updates` | Quarkus e dependências Java |
| `go list -m -u all` | módulos Go |

## Aplicar

```bash
mise run atualizar
```

Aplica e roda os testes no fim. O que quebrar aparece antes do commit.

> Atualização de **linguagem** — Java, Kotlin, Go — não deveria entrar por aqui sem
> discussão: mexe no material de aula. Edite `libs.versions.toml` à mão e abra um PR.
