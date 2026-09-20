# ADR-0004 — PostgreSQL com Flyway, o mesmo esquema nas duas APIs

**Estado:** Aceita
**Data:** 2026-09-19

## Contexto

Até a Sprint 0, as duas APIs só liam: repassavam a busca ao serviço Go, que avalia a árvore
de filtro sobre um acervo em memória. A proposta já previa mais do que isso: a seção 4.1 põe
"persistência e migrações" e o CRUD transacional na API, e o MVP inclui a anotação assinada
por curador.

A Sprint 1 de DIM0547 pede CRUD de duas entidades relacionadas, esquema versionado por
migrações, integração com banco real nos testes, rodando igual na máquina e no CI, e um
teste de arquitetura. Os exemplos da disciplina usam Exposed no Ktor, Panache no Quarkus,
Flyway e PostgreSQL 17.

Quatro restrições pesam:

- As duas APIs andam em paralelo, sem defasagem (ADR-0001).
- O deploy no Render fica sem banco até a Sprint 3, quando entra o Neon. O PostgreSQL
  gratuito do Render expira em 30 dias.
- A busca continua no Go (seção 4.1 da proposta), e o Go ainda não fala com banco: a
  integração por gRPC é da Sprint 2.
- A busca ocupava `GET /obras` e `POST /obras`, os mesmos endereços de que o CRUD precisa.

## Decisão

PostgreSQL 17, com esquema criado só por migrações Flyway. Nenhum ORM cria ou altera tabela
(`SchemaUtils.create` no Exposed e `schema-management.strategy` diferente de `none` no
Hibernate ficam proibidos).

| Migração | Conteúdo |
|---|---|
| `V1__cria_obras_e_anotacoes.sql` | `obras`, `facetas` (uma linha por faceta) e `anotacoes` (1:N com `obras`, chave estrangeira com `ON DELETE CASCADE`) |
| `V2__acervo_de_exemplo.sql` | as cinco obras de `contratos/exemplos/acervo.json`, as mesmas que o Go tem em memória |

As restrições que decidem conflito ficam no banco: `mbid` é único (duas obras com o mesmo
`mbid` são duplicata, ADR-0003), e o mesmo curador não repete a mesma faceta na mesma obra.
Curadores diferentes podem divergir.

As duas APIs têm os mesmos arquivos de migração, em
`api-ktor/src/main/resources/db/migration` e `api-quarkus/src/main/resources/db/migration`,
e podem usar o mesmo banco: o que uma grava, a outra lê.

| | `api-ktor` | `api-quarkus` |
|---|---|---|
| Acesso a dados | Exposed (DSL) + HikariCP | Hibernate ORM com Panache |
| Migração | `Flyway.migrate()` na subida | `quarkus.flyway.migrate-at-start` |
| Testes com banco | Testcontainers | `@QuarkusTest` com Dev Services |
| Sem banco | `DB_URL` ausente | `MUSI_COM_BANCO=false` |

Os repositórios são portas na camada de aplicação (`RepositorioDeObras`,
`RepositorioDeAnotacoes`), com implementação nos adaptadores. Um teste de arquitetura
(ArchUnit, nos dois stacks) falha se o domínio ou a aplicação depender de framework, banco
ou dos adaptadores.

Paginação e filtros vão para o SQL, com tamanho máximo de 100 itens por página.

Sem banco configurado, a API sobe mesmo assim: `/health` e a busca funcionam, e o CRUD
responde `503` em *problem details*. É o estado do Render até a Sprint 3.

A busca delegada ao Go muda de endereço e continua igual por dentro:
`GET /busca?dimensao=&valor=` e `POST /busca`. `/obras` passa a ser o recurso do CRUD, e
as anotações ficam aninhadas em `/obras/{id}/anotacoes`.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Esquema gerado pelo ORM | O esquema divergiria entre Exposed e Hibernate, sem histórico nem revisão. A rubrica recusa |
| Uma pasta única de migrações na raiz, lida pelos dois builds | O contexto do Docker da api-quarkus é a própria pasta, e o módulo deixaria de ser construído de forma independente (ADR-0001) |
| Um banco por API, com esquemas próprios | Dobraria o trabalho de modelagem e perderia a verificação de que as duas implementações leem o mesmo dado |
| Facetas numa coluna JSONB | A consulta por faceta passaria a depender de operadores de JSON; com linhas, basta índice e `EXISTS` |
| Anotação como faceta com uma coluna `curador` | Misturaria catálogo e afirmação assinada, e a obra teria facetas "de ninguém" e "de alguém" na mesma tabela |
| Curador como entidade, com conta e login | Contas de usuário estão fora do MVP; o curador é um identificador textual |
| H2 em memória nos testes | Outro dialeto de SQL: testaria um banco que não roda em produção |
| Banco no Neon já na Sprint 1 | O Neon é conteúdo da Sprint 3 de DIM0547; antes disso, o PostgreSQL do compose basta |
| CRUD só numa das APIs | ADR-0001: as duas andam juntas |
| Manter a busca em `/obras` e pôr o CRUD sob outro prefixo | O recurso "obra" teria dois endereços |
| Avaliar a busca em SQL | Contraria a divisão da seção 4.1 da proposta, que põe a avaliação da árvore no Go |

## Consequências

- ✅ Um esquema, duas implementações de acesso a dados, comparáveis lado a lado: DSL em
  Kotlin de um lado, entidades JPA do outro
- ✅ Dimensão nova continua sendo dado (ADR-0002): uma linha a mais em `facetas`
- ✅ A mesma suíte roda na máquina e no CI, sem configuração de banco nos testes
- ✅ O deploy sem banco continua no ar, com a busca funcionando
- ⚠️ Há duas fontes de obras até a Sprint 2: o banco, usado pelo CRUD, e o acervo em memória
  do Go, usado pela busca. Uma obra criada pela API não aparece em `/busca`, e anotações não
  mudam o resultado da busca
- ⚠️ As migrações existem em duas pastas. Toda migração nova é copiada para as duas, e o CI
  compara as pastas (`diff -r`)
- ⚠️ A `V2` põe dados de exemplo numa migração versionada: eles irão para qualquer banco
  migrado, inclusive o Neon na Sprint 3. Tirá-los exigirá uma migração nova
- ⚠️ As duas APIs desligam o banco de formas diferentes (`DB_URL` ausente × `MUSI_COM_BANCO`):
  o Quarkus desativa o datasource sem URL, mas o Hibernate e o Flyway precisam ser desligados
  à parte
- ⚠️ A mudança de `/obras` para `/busca` quebra quem chamava a busca pelo endereço antigo
- ⚠️ Os testes de integração precisam de Docker. Sem ele, o `verificar.sh` os pula e avisa;
  o CI roda todos

## Como verificar

- Job `docs` do CI: `diff -r` entre as duas pastas de migração.
- `IntegracaoPostgresTest` (api-ktor) e `IntegracaoTest` (api-quarkus) sobem um PostgreSQL 17
  e conferem a tabela `flyway_schema_history`, o CRUD, a paginação, os filtros, os conflitos
  (`409`) e o `ON DELETE CASCADE`.
- `ArquiteturaTest`, nos dois stacks, falha se `dominio` ou `aplicacao` importar framework ou
  banco.
- Nenhuma ocorrência de `SchemaUtils` no código, e `schema-management.strategy=none` no
  `application.properties`:

```bash
grep -rn "SchemaUtils" api-ktor/src; grep -n "schema-management" api-quarkus/src/main/resources/application.properties
```
