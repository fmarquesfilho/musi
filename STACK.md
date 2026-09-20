# Stack do MUSI

O MUSI é um projeto poliglota desenhado para apoiar as disciplinas DIM0510, DIM0524 e DIM0547.
A stack foi escolhida para permitir o ensino focado em boas práticas, comparação de ecossistemas e integração de sistemas (mobile e backend).

## Backend (DIM0547 - Desenvolvimento Web II)

O backend possui três implementações que respondem ao mesmo contrato (JSON Schema / Protobuf):

1. **Go 1.27** (`services/`):
   - Microserviços performáticos sem dependência de frameworks externos (apenas standard library para HTTP).
   - Focado em ensino de concorrência, canais, e bibliotecas padrão.

2. **Kotlin + Ktor** (`api-ktor/`):
   - Framework assíncrono mantido pela JetBrains.
   - Injeção de dependência via **Koin** (resolução em tempo de execução).
   - Utiliza rotas declarativas baseadas em código.
   - Persistência com **Exposed** (SQL num DSL de Kotlin) sobre HikariCP.

3. **Java 25 + Quarkus** (`api-quarkus/`):
   - Framework cloud-native (Tailored for GraalVM/SubstrateVM).
   - Injeção de dependência via **CDI** (resolução em tempo de compilação).
   - Utiliza rotas baseadas em anotações (JAX-RS).
   - Persistência com **Hibernate ORM com Panache**.

As duas expõem o mesmo CRUD (obras e anotações, 1:N), sobre o mesmo esquema, criado pelas
mesmas migrações Flyway, e delegam a busca por árvore de filtro ao serviço Go — ver
[ADR-0004](docs/decisoes/0004-persistencia-postgresql-flyway.md).

## Frontend Mobile / Desktop (DIM0524 - Dispositivos Móveis)

1. **Kotlin Multiplatform (KMP) & Compose Multiplatform** (`app/` e `shared/`):
   - `shared/`: Contém a regra de negócios (Domínio). Compartilhado entre a `api-ktor` e o `app`, eliminando duplicação de validações (State of the art).
   - `app/`: Aplicação móvel utilizando **Jetpack Compose / Compose Multiplatform**. O código de UI (`commonMain`) serve tanto para Android e iOS quanto para Desktop (JVM). Alvos ligados: **Android** (a plataforma declarada na proposta) e Desktop; iOS fica para a Sprint 3.
   - Navegação com **Navigation Compose** multiplataforma, rotas tipadas (`@Serializable`) e deep link `musi://obra/{id}`; layout adaptativo por classe de tamanho de janela (**Material 3 Adaptive**).
   - Testes de interface em `commonTest`, executados no alvo desktop (`./gradlew :app:jvmTest`), sem emulador.
   - **Hot Reload e Cloud IDE**: O app roda em ambientes headless (como o GitHub Codespaces) via `./gradlew :app:run -Pheadless` (ou `:app:hotRunJvm` para hot reload), com noVNC (desktop-lite) para visualização. Ver [`docs/COMO-RODAR.md`](docs/COMO-RODAR.md).

## Infraestrutura e DevOps

- **Cloud IDE**: GitHub Codespaces via `.devcontainer`. Todas as dependências (Go 1.27, JDK 25, Python, Docker-in-Docker e Desktop-lite) já estão provisionadas.
- **Banco de Dados**: PostgreSQL 17, local no `docker compose` e no CI (Testcontainers no Ktor,
  Dev Services no Quarkus). Hospedado no Neon a partir da Sprint 3.
- **Migrações**: Flyway, com o esquema versionado em `src/main/resources/db/migration` — os
  mesmos arquivos nas duas APIs, comparados pelo CI.
- **Arquitetura verificada**: ArchUnit nos dois stacks, arch-go no Go.
- **Deploy Automático**: Render (via `render.yaml`).
- **Validação de Contratos**: Scripts em Python testando APIs contra os Schemas JSON locais (`contratos/`).
- **Build System**: Gradle com Version Catalogs (`libs.versions.toml`).

> Para ver as justificativas detalhadas das escolhas da stack, leia a [ADR-0001 (Stacks e Estrutura)](docs/decisoes/0001-stacks-e-estrutura.md).

> Footprint de memória medido de cada componente (Go, Ktor, Quarkus): [docs/BENCHMARK.md](docs/BENCHMARK.md). Reproduza com `./benchmark/medir-memoria.sh`.
