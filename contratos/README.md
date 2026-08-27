# Contratos

Este diretório é a referência do domínio. Os três componentes validam contra o que está
aqui, e nenhum define o modelo por conta própria.

| Arquivo | Fronteira | Quem usa |
|---|---|---|
| `obra.schema.json` | HTTP, entre app e API | DIM0524 · DIM0547 |

| `exemplos/*.json` | Casos de teste comuns | Os três |
| `musi/busca/v1/busca.proto` | Contrato gRPC — hospedado no BSR; **runtime na Sprint 2** | DIM0547 |

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
| Protocol Buffers | Interna, entre serviços | contrato agora (BSR); runtime **Sprint 2** |

Estão juntos de propósito: é o mesmo domínio, descrito para dois consumidores
diferentes. Separar em pastas sugeriria que são contratos independentes, e eles não
são — quando `Filtro` ganha um construtor, os dois mudam.

O `busca.proto` já é tratado como contrato de verdade: `buf lint` e `buf build`
rodam no CI (job `proto`), o módulo é hospedado no BSR, e `buf breaking` protege a
compatibilidade. O que fica para a Sprint 2 é o **runtime** gRPC — os serviços
ainda conversam por HTTP e JSON até lá.

## BSR — Buf Schema Registry

O contrato é publicado como o módulo **`buf.build/fmarquesfilho/busca`**. O layout
segue o buf: o arquivo fica em `musi/busca/v1/` para casar com o `package`.

Localmente (o `buf` vem fixado pelo `mise`):

```bash
buf lint                                   # estilo e consistência
buf breaking --against '.git#branch=main'  # não quebrou o contrato
mise run test:proto                        # o que o CI roda (lint + build)
```

Para publicar, uma vez, autentique com um token de
[buf.build/settings/user](https://buf.build/settings/user):

```bash
buf registry login      # cola o token
buf push                # ou: mise run proto:push
```

No CI, o `buf push` acontece sozinho a cada merge no `main` — basta cadastrar o
token como o segredo **`BUF_TOKEN`** do repositório. Enquanto o módulo não é
publicado, o passo de `buf breaking` no CI passa com um aviso, sem falhar.
