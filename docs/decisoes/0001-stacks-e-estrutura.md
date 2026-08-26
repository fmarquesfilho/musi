# ADR-0001 — Monorepo, três linguagens, Java 25 por toolchain

**Estado:** Aceita
**Data:** 2026-08-26

## Contexto

O MUSI é projeto de exemplo de três disciplinas com públicos distintos: DIM0510 avalia
processo, DIM0547 avalia serviços HTTP, DIM0524 avalia um cliente móvel. A maioria dos alunos
cursa apenas uma.

Em DIM0547 houve empate numa enquete entre Java e Kotlin: 11 votos para cada, em 41 alunos.
Quem votou em Java citou a grade curricular do BTI, que usa Java em disciplinas anteriores, e
os requisitos de bolsas e estágios do IMD. A disciplina passou a ser comparativa, com as duas
linguagens apresentadas ao mesmo tempo.

A versão do JDK, por sua vez, aparecia em cinco arquivos diferentes, e atualizar exigia
lembrar de todos.

## Decisão

Um repositório, com componentes em diretórios de primeiro nível:

| Componente | Stack | Disciplina |
|---|---|---|
| `shared/` | Kotlin, KMP | domínio de `api-ktor` e `app` |
| `api-ktor/` | Kotlin · Ktor · Koin | DIM0547 |
| `api-quarkus/` | Java 25 · Quarkus · CDI | DIM0547 |
| `services/` | Go | DIM0547 |
| `app/` | Kotlin · Compose | DIM0524 |
| `processo/`, `docs/` | — | DIM0510 |

As duas APIs são mantidas em paralelo, sem defasagem. `api-quarkus` reescreve o domínio em
vez de importar `shared/`: importar o módulo Kotlin anularia a comparação. O que impede as
duas de divergirem são os casos de teste de `contratos/exemplos/`.

A versão do Java é declarada uma vez, em `gradle/libs.versions.toml`, e lida por toolchain:

```kotlin
kotlin { jvmToolchain(libs.versions.java.get().toInt()) }
```

O CI é um arquivo só, com um job por componente e um job de build sem filtro de caminho.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Repositórios separados por disciplina | O domínio divergiria em semanas, e a comparação entre linguagens deixaria de existir |
| Só Kotlin, ou só Java | Metade da turma justificou a preferência oposta com argumentos de grade e mercado |
| Java uma sprint atrás do Kotlin | Cria alunos de segunda classe, e a defasagem se acumula |
| `api-quarkus` importando `shared/` | Tecnicamente possível, já que Kotlin gera bytecode. Anularia a comparação |
| Java 21 | Também é LTS e mais comum em ambiente corporativo. Java 25 traz arquivos-fonte compactos, que aproximam os exercícios de Java dos de Kotlin |
| `sourceCompatibility` no lugar de toolchain | Não garante qual JDK compila, só o nível do bytecode |
| Spring Boot no lugar de Quarkus | Consumo de memória inviável nos 512 MB do plano gratuito |

## Consequências

- ✅ Um domínio, três implementações, comparáveis lado a lado
- ✅ Cada grupo escolhe a linguagem com que aprende melhor, sem prejuízo
- ✅ Uma linha muda a versão do Java em todos os módulos Gradle, e o build não depende do JDK
  da máquina
- ✅ Quarkus resolve o grafo de dependências na compilação e Koin em execução, o que dá dois
  modelos para comparar
- ⚠️ O domínio existe duas vezes. É proposital, e o custo é real: todo item novo do backlog
  vira dois
- ⚠️ O `pom.xml` não lê o catálogo do Gradle, então `maven.compiler.release` continua
  duplicado, assim como a tag das imagens nos Dockerfiles
- ⚠️ O repositório é maior do que cada disciplina isoladamente requer
- ⚠️ As rubricas precisam aceitar qualquer uma das duas linguagens, sem preferência

## Como verificar

Os jobs `kotlin` e `java` do CI rodam os mesmos casos de `contratos/exemplos/`. Um teste que
passe em um e falhe no outro reprova o pipeline.

`api-quarkus/pom.xml` não pode declarar dependência de nenhum módulo Gradle deste
repositório.

```bash
./gradlew :api-ktor:compileKotlinJvm --info | grep -i toolchain
```
