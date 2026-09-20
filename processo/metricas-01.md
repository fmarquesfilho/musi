# Métricas de fluxo — Sprint 0 e início da Sprint 1

Dados do próprio MUSI, coletados em **19/09/2026**, do repositório
`github.com/fmarquesfilho/musi`. Cada número vem com o comando que o produz: quem duvidar
roda de novo. Nada aqui é estimativa.

Período coberto: do primeiro commit (**25/08**) a **19/09**. A Sprint 0 vai de 17/08 a 14/09
e a Sprint 1, de 14/09 a 02/10 (`CRONOGRAMA.md` das disciplinas).

Ferramentas: `git` e o [`gh`](https://cli.github.com), que lê os pull requests, o quadro e as
execuções do CI. Todos os comandos abaixo foram executados; as saídas estão resumidas nas
tabelas.

---

## 1. O que o acordo prometeu medir, e o que deu para medir

O [acordo de processo](acordo-de-processo.md) diz que as métricas viriam dos *Insights* do
GitHub Projects: itens concluídos por sprint, tempo em "Em revisão" e itens parados.

Nenhuma das três pôde ser calculada. O quadro tem 3 cartões, todos criados em 28/08 e nunca
movidos: sem item em "Pronto", não há vazão nem tempo de ciclo para medir.

| Métrica do acordo | Situação | Por quê |
|---|---|---|
| Itens concluídos por sprint | 0 | Nenhum cartão saiu de "Todo" |
| Tempo em "Em revisão" | não existe | O quadro não tem a coluna "Em revisão" |
| Itens parados > 1 semana | 3 de 3 | Os três cartões estão parados há 22 dias |

O que existe de fluxo real está no git, nos pull requests e no CI. É de lá que vêm os
números das seções seguintes — e essa troca de fonte é uma mudança do acordo, registrada na
[retrospectiva](retrospectiva-01.md).

---

## 2. O quadro

```bash
gh project item-list 3 --owner fmarquesfilho --format json
gh api graphql -f query='{ user(login:"fmarquesfilho"){ projectV2(number:3){
  createdAt updatedAt items(first:20){ totalCount nodes{ createdAt updatedAt
  fieldValueByName(name:"Status"){ ... on ProjectV2ItemFieldSingleSelectValue{ name updatedAt } } } } } } }'
```

| Fato | Valor |
|---|---|
| Quadro criado em | 28/08, 23:02 |
| Última alteração do quadro | 28/08, 23:13 |
| Cartões | 3, todos em `Todo` |
| Mudanças de status desde a criação | 0 |
| Colunas | `Todo`, `In Progress`, `Done` (as do modelo do GitHub) |
| Limites de WIP configurados | nenhum |

As colunas do acordo ("Em progresso", "Em revisão") e os limites (2 por pessoa, 2 em revisão)
não existem no quadro. Os campos `Tipo`, `Componente`, `Tamanho` e `Risco` existem e estão
preenchidos nos 3 cartões, como descreve o [`backlog.md`](backlog.md).

## 3. Trabalho integrado

```bash
git log --format='%ad %h %s' --date=short        # 43 commits
git log --format=%ad --date=short | sort | uniq -c
git log --oneline 1b6012f^2 --not 1b6012f^1 | wc -l   # commits da branch do PR #1
```

| Métrica | Valor |
|---|---|
| Commits | 43, de um único autor |
| Período com commits | 25/08 a 01/09 — 8 dias corridos, todos com commit |
| Dias sem commit desde então | 18 (01/09 a 19/09) |
| Commits por dia ativo | 5,4 |
| Commits direto na `main` | 32 (74%) |
| Commits que passaram por pull request | 10 (24%), todos no PR #1 |
| Mediana de linhas por commit | 22 |
| Commits acima de 500 linhas | 2 (o maior, o commit inicial, com 5.668) |

## 4. Pull requests e revisão

```bash
gh pr list --repo fmarquesfilho/musi --state all \
  --json number,title,createdAt,mergedAt,reviews,additions,deletions
```

| Métrica | Valor |
|---|---|
| Pull requests | 1 (#1, "Conciliação com o MusicBrainz") |
| Aberto em | 28/08, 22:26 |
| Integrado em | 28/08, 23:14 |
| Tempo até a integração | 48 min |
| Revisões registradas | 0 |
| Tamanho | 590 linhas acrescentadas, 198 removidas |

Esse é o único tempo de ciclo mensurável do projeto: 48 minutos, num item só.

## 5. Itens em aberto

```bash
gh issue list --repo fmarquesfilho/musi --state all \
  --json number,title,createdAt,closedAt,state
```

| Issue | Tipo | Aberta em | Idade em 19/09 |
|---|---|---|---|
| #2 Refatorar o cliente MusicBrainz | Débito | 28/08 | 22 dias |
| #3 Persistir o MBID escolhido na conciliação | Tarefa | 28/08 | 22 dias |
| #4 Fábricas por variante no `FiltroDto` | Débito | 28/08 | 22 dias |

Nenhuma fechada. As três nasceram da análise do PR #1, o que é bom sinal: a revisão gerou
trabalho registrado em vez de comentário perdido.

## 6. Integração contínua

```bash
gh run list --repo fmarquesfilho/musi --limit 60 \
  --json conclusion,createdAt,updatedAt,event,headBranch
```

| Métrica | Valor |
|---|---|
| Execuções | 18 |
| Verdes | 13 |
| Vermelhas | 4 |
| Canceladas | 1 |
| Duração mediana | 3,4 min (máximo 5,2) |
| Execuções disparadas por pull request | 1 |

Tempo até a `main` voltar ao verde, depois de cada falha:

| Falha | Verde de novo | Tempo parada |
|---|---|---|
| 26/08 01:35 | 26/08 01:48 | 13 min |
| 26/08 01:41 | 26/08 01:48 | 7 min |
| 26/08 17:46 | 27/08 19:37 | **25,8 h** |
| 29/08 15:27 | 29/08 15:38 | 11 min |

Três falhas foram corrigidas em minutos. Uma deixou a `main` vermelha por mais de um dia.

## 7. Decisões

```bash
python3 docs/verificar_adrs.py            # conta as ADRs, sem o modelo 0000
grep -H '^\*\*Data:\*\*' docs/decisoes/000[1-9]*.md
```

| Métrica | Valor |
|---|---|
| ADRs aceitas | 4 |
| Escritas na Sprint 0 | 3 (26/08) |
| Escritas na Sprint 1 | 1 (ADR-0004, 19/09) |
| ADRs contrariadas pelo código | nenhuma detectada; a 0002 é verificada pelo CI |

## 8. Sprint 1, até 19/09

A Sprint 1 começou em 14/09 e termina em 02/10. Até a data desta coleta:

| Fato | Valor |
|---|---|
| Commits no período 14/09–19/09 | 0 |
| Cartões criados para a Sprint 1 | 0 |
| Pull requests abertos | 0 |
| Trabalho feito | o incremento da Sprint 1 (CRUD de obras e anotações, persistência, testes de integração, ADR-0004) está pronto na árvore de trabalho, ainda não commitado |

O incremento existe e funciona, mas nasceu fora do fluxo que o acordo descreve: sem cartão,
sem branch e sem PR. Está tratado na [retrospectiva](retrospectiva-01.md).
