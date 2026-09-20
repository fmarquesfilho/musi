# `api-quarkus` — a API em Java

É a mesma API que existe em [`../api-ktor/`](../api-ktor/), em Java 25 com Quarkus.

Ver [ADR-0001](../docs/decisoes/0001-stacks-e-estrutura.md).

## O que comparar, arquivo a arquivo

| Conceito | Aqui | Do lado Ktor |
|---|---|---|
| Domínio | `dominio/Dominio.java` | `shared/.../Dominio.kt` |
| Porta | `aplicacao/FonteDeObras.java`, `RepositorioDeObras.java` | `aplicacao/FonteDeObras.kt`, `Repositorios.kt` |
| Caso de uso | `aplicacao/CatalogoDeObras.java` | `aplicacao/CatalogoDeObras.kt` |
| Injeção de dependência | anotações CDI e `persistencia/Repositorios` | `Modulos.kt`, explícito |
| Rotas | anotações em `ObraResource` | `RotasDeObras.kt`, código |
| Acesso a dados | Panache, em `ObrasPanache` | Exposed, em `ObrasPostgres.kt` |
| Migrações | `resources/db/migration` (iguais nos dois) | `resources/db/migration` |
| Cliente do serviço Go | `ClienteBusca`, interface | `BuscaHttp`, classe |
| Validação da forma | Bean Validation (`@NotNull`) | kotlinx.serialization |
| Erro `problem+json` | `ErrosMapper` | plugin `StatusPages` |
| Teste de arquitetura | `ArquiteturaTest.java` (ArchUnit) | `ArquiteturaTest.kt` (ArchUnit) |

## Diferenças entre as abordagens

| | Quarkus + CDI | Ktor + Koin |
|---|---|---|
| Grafo de dependências | resolvido na **compilação** | resolvido na **execução** |
| Dependência faltando | o build falha | falha na inicialização |
| Onde ler o grafo | espalhado por anotações | reunido num arquivo |

## Rodar

```bash
mvn quarkus:dev        # modo dev, com hot reload e PostgreSQL do Dev Services
mvn test               # sem Docker: mvn test -DexcludedGroups=integracao
```

| Endpoint | O que é |
|---|---|
| `/busca?dimensao=ritmo&valor=baiao` · `POST /busca` | Busca, delegada ao serviço Go |
| `/obras` · `/obras/{id}` | CRUD de obras, paginado e com filtros |
| `/obras/{id}/anotacoes` · `/obras/{id}/anotacoes/{anotacaoId}` | Anotações da obra (1:N) |
| `/q/health` | Estado da aplicação |
| `/q/openapi` · `/q/swagger-ui` | Contrato gerado |

O banco é o da [ADR-0004](../docs/decisoes/0004-persistencia-postgresql-flyway.md): em dev e
em teste, um PostgreSQL 17 sobe sozinho pelo Dev Services; em produção, a conexão vem de
`DB_URL`, `DB_USER` e `DB_PASSWORD`. Com `MUSI_COM_BANCO=false`, a aplicação sobe sem banco e
o CRUD responde `503`.
