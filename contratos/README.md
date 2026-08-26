# Contratos

Este diretório é a referência do domínio. Os três componentes validam contra o que está
aqui, e nenhum define o modelo por conta própria.

| Arquivo | Fronteira | Quem usa |
|---|---|---|
| `obra.schema.json` | HTTP, entre app e API | DIM0524 · DIM0547 |

| `exemplos/*.json` | Casos de teste comuns | Os três |
| `busca.proto` | Contrato gRPC — **entra em vigor na Sprint 2** | DIM0547 |

## Duas fronteiras, dois formatos

| | JSON Schema | Protocol Buffers |
|---|---|---|
| Onde | App ↔ API (HTTP) | API ↔ serviços Go (gRPC) |
| Por quê | Legível no navegador; um `curl` mostra o modelo | Contrato verificado, stubs gerados, `buf breaking` |
| Custo | Verificação em tempo de execução | Ferramental a mais |

**Uma pergunta natural:** por que não usar um formato só? Porque as duas fronteiras têm
exigências diferentes. Na borda pública, legibilidade e tolerância pesam mais; entre serviços
internos, a verificação de compatibilidade pesa mais.

Essa justificativa vale ser discutida em aula.

## Os exemplos são o teste compartilhado

`exemplos/casos-de-busca.json` traz filtros com **resultado esperado**. Os três componentes
rodam os mesmos casos:

- `app` — teste de Kotlin em `commonTest`
- `api` — teste de JUnit
- `services` — `go test`

Caso as três implementações produzam resultados diferentes para o mesmo caso, há algo a
corrigir em uma delas. É esse mecanismo que mantém o domínio consistente no monorepo.

## Versionamento

Campo `versao` na raiz do schema, em SemVer. Mudanças maiores exigem ADR e alcançam os três
componentes no mesmo commit — ver [ADR-0001](../docs/decisoes/0001-stacks-e-estrutura.md).

## Uma pasta, duas fronteiras, dois momentos

| Formato | Fronteira | A partir da |
|---|---|---|
| JSON Schema e exemplos | Pública, HTTP | **Sprint 0** |
| Protocol Buffers | Interna, entre serviços | **Sprint 2** |

Estão juntos de propósito: é o mesmo domínio, descrito para dois consumidores
diferentes. Separar em pastas sugeriria que são contratos independentes, e eles não
são — quando `Filtro` ganha um construtor, os dois mudam.

O `busca.proto` está aqui desde já para que dê para ver **onde o contrato vai
chegar**. Nada no pipeline o verifica antes da Sprint 2, quando `buf lint` e
`buf breaking` entram.
