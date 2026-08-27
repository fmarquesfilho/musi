# MUSI

Enciclopédia musical com metadados extensíveis e padrões abertos.

## Comece por aqui, conforme a sua disciplina

| Se você cursa | Vá para | Ignore o resto |
|---|---|---|
| **DIM0510 — Processos de Software** | [`processo/`](processo/) e [`docs/decisoes/`](docs/decisoes/) | A leitura do código é opcional |
| **DIM0547 — Desenvolvimento Web II** | [`api-ktor/`](api-ktor/) **ou** [`api-quarkus/`](api-quarkus/), [`services/`](services/), [`contratos/`](contratos/) | — |
| **DIM0524 — Dispositivos Móveis** | [`app/`](app/), [`shared/`](shared/), [`contratos/`](contratos/) | — |

Cada componente pode ser lido e construído de forma independente, conforme a
[ADR-0001](docs/decisoes/0001-stacks-e-estrutura.md).

---

## O que o MUSI responde

Serviços de streaming costumam organizar o acervo por gênero comercial e por comportamento
de escuta. São critérios simples de medir, mas deixam de fora boa parte do que caracteriza
uma gravação.

Entre as dimensões que o MUSI cataloga estão:

- Padrão rítmico — ijexá, baião, ponteio, maracatu, coco
- Instrumentação e prática de execução
- Movimento e inserção social — tropicália, jovem guarda, manguebeat, clube da esquina
- Região, linhagem, contexto de gravação

A proposta do MUSI é que o catálogo não defina previamente o que é relevante: quem busca é
quem monta os critérios.

---

## Este projeto é o exemplo da Sprint 0

O MUSI implementa o que `docs/SPRINT-0.md` das três disciplinas pede:

| Artefato | Onde | Disciplina |
|---|---|---|
| Visão do produto, MVP, hipótese de valor | [`docs/proposta.md`](docs/proposta.md) | as três |
| Backlog priorizado e estimado | GitHub Projects, com os campos de [`processo/backlog.md`](processo/backlog.md) | as três |
| Divisão Kotlin × Go, com justificativa | `docs/proposta.md` §4.1 | DIM0547 |
| Plataforma-alvo e backend | `docs/proposta.md` §4.2 | DIM0524 |
| Acordo de processo | [`processo/acordo-de-processo.md`](processo/acordo-de-processo.md) | DIM0510 |
| Decisões registradas | [`docs/decisoes/`](docs/decisoes/) — 3 ADRs | as três |

Copiem a estrutura, não a extensão: a proposta de vocês cabe em 3 páginas.

## Como rodar

```bash
docker compose up --build     # as duas APIs e o serviço Go
```

Codespaces, Docker e Render em [`docs/COMO-RODAR.md`](docs/COMO-RODAR.md). Versões e
atualização em [`docs/VERSOES.md`](docs/VERSOES.md). Publicação em
[`docs/COMO-PUBLICAR.md`](docs/COMO-PUBLICAR.md).

## Mapa do repositório

```
musi/
├── docs/            domínio, glossário e ADRs         → todas as disciplinas
│   └── decisoes/    registro de decisões (ADR)
├── processo/        backlog, definição de pronto, rituais → DIM0510
├── contratos/       o contrato compartilhado           → DIM0547 + DIM0524
│   ├── musi/busca/v1/busca.proto  Protocol Buffers (BSR), entre serviços
│   ├── obra.schema.json      JSON Schema, na fronteira HTTP
│   └── exemplos/             casos de teste comuns aos três componentes
├── buf.yaml         módulo do contrato gRPC no BSR      → DIM0547
├── shared/          o domínio, em Kotlin — usado por api E app
├── api-ktor/        Kotlin · Ktor · Koin                → DIM0547
├── api-quarkus/     Java 25 · Quarkus · CDI             → DIM0547
├── services/        Go · gRPC                          → DIM0547
├── app/             Kotlin Multiplatform · Compose     → DIM0524
├── exercicios/      exercícios de aula, por disciplina
├── .github/workflows/ci.yml    pipeline único, jobs independentes
└── mise.toml        versões e tasks
```

## Duas APIs, de propósito

DIM0547 é comparativa: o mesmo conceito em Java e em Kotlin, **simultaneamente**, e cada
grupo escolhe uma. Ver [ADR-0001](docs/decisoes/0001-stacks-e-estrutura.md).

| | `api-ktor/` | `api-quarkus/` |
|---|---|---|
| Linguagem | Kotlin | Java 25 |
| Framework | Ktor | Quarkus |
| Injeção de dependência | Koin, em execução | CDI, **na compilação** |
| Rotas | código, numa árvore | anotações |
| Cliente do Go | classe com `HttpClient` | interface declarativa |
| Domínio | vem de `shared/` | reescrito em `dominio/` |

A duplicação do domínio é proposital: importar o módulo Kotlin no lado Java anularia a
comparação. O que impede as duas de divergirem são os mesmos casos de teste, em
`contratos/exemplos/`.

## O domínio compartilhado

Desde a [ADR-0001](docs/decisoes/0001-stacks-e-estrutura.md), `api/` e `app/` importam o mesmo
módulo `shared/`. O domínio deixou de existir duas vezes, em Java e em Kotlin, mantido em
sincronia por testes: agora é um arquivo.

```
  shared/src/commonMain/kotlin/br/ufrn/musi/dominio/Dominio.kt
        ↑                                   ↑
      api/  (Ktor)                        app/  (Compose)
```

`commonMain` não tem acesso a Ktor, Koin nem Compose: o compilador do módulo
multiplataforma garante a regra de dependência sem teste de arquitetura.

## O domínio, em quatro tipos

O mesmo modelo aparece nas três linguagens. Compará-los é parte do valor do projeto.

| Conceito | O que é |
|---|---|
| **Obra** | Uma gravação. Identificada por MBID, do MusicBrainz |
| **Faceta** | Um par `(dimensão, valor)` — `ritmo=ijexa`, `movimento=tropicalia` |
| **Filtro** | Uma **árvore**: `Tem`, `Ou`, `E`, `Exceto` |
| **Anotação** | Uma faceta atribuída por alguém identificável, com data |

A lista de dimensões não é fixa: quem anota pode introduzir dimensões novas sem alteração
de schema. É o que a expressão "metadado extensível" descreve, e explica por que `Faceta` é
um par em vez de um campo por dimensão.

---

## Como rodar

Não é necessário ter uma máquina configurada para começar. Ambientes online recomendados
por disciplina:

| Disciplina | Ambiente | Cadastro |
|---|---|---|
| DIM0524 | [`play.kotlinlang.org`](https://play.kotlinlang.org) | não |
| DIM0547 | [`go.dev/play`](https://go.dev/play) para Go · Cloud Shell Editor para Java | não / conta Google |
| DIM0510 | O próprio GitHub | conta GitHub |

### Em produção

| Componente | Onde | Custo | Cartão de crédito |
|---|---|---|---|
| `api/` e `services/` | Render, instância `free` | R$ 0 | **não** |
| Postgres | Neon, plano gratuito | R$ 0 | **não** |

Descrito em [`render.yaml`](render.yaml): um `git push` na `main` reimplanta o que mudou.

O banco fica no **Neon**, e não no Render, porque o Postgres gratuito do Render expira 30
dias após a criação e os dados são apagados.

> Duas limitações a conhecer: no Render, o serviço hiberna após 15 minutos sem tráfego e a
> primeira requisição depois disso leva cerca de um minuto; no Neon, a computação também
> hiberna, com alguns décimos de segundo para acordar.

Localmente, com [`mise`](https://mise.jdx.dev):

```bash
mise run verificar     # tudo que roda sem configuração
mise run ci            # o pipeline inteiro, como no GitHub Actions

mise run run:busca     # sobe o serviço Go na 9090
mise run run:api       # sobe a api Micronaut na 8080
mise run demo          # uma busca de exemplo

mise tasks             # a lista completa
```

## Licenças

- Documentação e processo: **CC BY-NC-SA 4.0**, como o material das disciplinas
- Código: **Apache-2.0**
- Dados de exemplo: **CC0**, e são **inventados para o curso** —
  [ADR-0002](docs/decisoes/0002-modelo-de-dominio.md)

## Uso de inteligência artificial

Parte deste material foi produzida com assistência de IA generativa, e isso está declarado.
A autoria e a responsabilidade permanecem humanas. A mesma orientação vale para as entregas
dos estudantes — ver
[`processo/definicao-de-pronto.md`](processo/definicao-de-pronto.md).
