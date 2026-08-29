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

3. **Java 25 + Quarkus** (`api-quarkus/`):
   - Framework cloud-native (Tailored for GraalVM/SubstrateVM).
   - Injeção de dependência via **CDI** (resolução em tempo de compilação).
   - Utiliza rotas baseadas em anotações (JAX-RS).

## Frontend Mobile / Desktop (DIM0524 - Dispositivos Móveis)

1. **Kotlin Multiplatform (KMP) & Compose Multiplatform** (`app/` e `shared/`):
   - `shared/`: Contém a regra de negócios (Domínio). Compartilhado entre a `api-ktor` e o `app`, eliminando duplicação de validações (State of the art).
   - `app/`: Aplicação móvel utilizando **Jetpack Compose / Compose Multiplatform**. O código de UI (`commonMain`) serve tanto para Android e iOS quanto para Desktop (JVM).
   - **Hot Reload e Cloud IDE**: O app está configurado para rodar nativamente via `jvmRun` em ambientes headless (como o GitHub Codespaces) utilizando noVNC (desktop-lite) para visualização.

## Infraestrutura e DevOps

- **Cloud IDE**: GitHub Codespaces via `.devcontainer`. Todas as dependências (Go 1.27, JDK 25, Python, Docker-in-Docker e Desktop-lite) já estão provisionadas.
- **Banco de Dados**: PostgreSQL (Hospedado no Neon).
- **Deploy Automático**: Render (via `render.yaml`).
- **Validação de Contratos**: Scripts em Python testando APIs contra os Schemas JSON locais (`contratos/`).
- **Build System**: Gradle com Version Catalogs (`libs.versions.toml`).

> Para ver as justificativas detalhadas das escolhas da stack, leia a [ADR-0001 (Stacks e Estrutura)](docs/decisoes/0001-stacks-e-estrutura.md).
