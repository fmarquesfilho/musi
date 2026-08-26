# Backlog no GitHub Projects

O backlog do MUSI vive num **GitHub Project** único, com visões por disciplina. Um
projeto só, e não três, é deliberado: itens de disciplinas diferentes dependem uns dos
outros, e um quadro por turma esconderia justamente essas dependências.

## Estrutura

**Um Project**, chamado `MUSI`, com cinco visões:

| Visão | Filtro | Para quem |
|---|---|---|
| **Quadro** | agrupado por `Status` | Andamento geral |
| **Web II** | `Componente: api, services, contratos` | DIM0547 |
| **Móveis** | `Componente: app, contratos` | DIM0524 |
| **Roadmap** | linha do tempo por `Sprint` | Visão de processo |
| **Parados** | `Status: Em andamento`, ordenado por atualização | Retrospectiva |

A visão **Parados** costuma ser a mais útil das cinco, e é também a menos lembrada na hora
de configurar o quadro.

## Campos

São poucos campos, cada um com uma finalidade clara. Quadros com muitos campos tendem a
ficar desatualizados, porque preenchê-los deixa de compensar.

| Campo | Tipo | Valores | Por que existe |
|---|---|---|---|
| `Status` | seleção | A fazer · Em andamento · Em revisão · Pronto | Fluxo |
| `Tipo` | seleção | História · Tarefa · Defeito · Decisão · Débito | Muda a definição de pronto |
| `Sprint` | iteração | Sprint 0…3 | Alinha com o calendário |
| `Componente` | seleção | docs · contratos · api · services · app | Alimenta as visões |
| `Tamanho` | seleção | P · M · G | Relativo, nunca em horas |
| `Risco` | seleção | Baixo · Alto | Ver abaixo |

### Sobre `Tamanho`

A estimativa é relativa, não temporal. **P** cabe em uma sessão de trabalho e **M** cabe em
uma semana. Um item **G** indica que ainda falta dividi-lo, e por isso itens G não entram em
sprint.

Estimativas em horas tendem a variar muito entre pessoas e acabam medindo quem executa, e
não o tamanho do item.

### Sobre `Risco`

É um campo pouco comum e bastante útil. `Risco: Alto` indica que ainda não se sabe se a
abordagem funciona, e não que o item seja difícil de executar.

> **Regra do projeto:** em toda sprint, o primeiro item puxado é o de maior risco. Se ele
> falhar, falha cedo, e o resto da sprint ainda pode ser replanejado.

Um item de risco alto entrega principalmente informação, mesmo quando o resultado técnico
não é o esperado.

## Etiquetas

Três, além do que os campos já dizem:

| Etiqueta | Uso |
|---|---|
| `bloqueado` | Acompanhada de comentário indicando por quem ou por quê |
| `precisa-decisao` | Precisa de ADR antes de ser trabalhado |
| `boa-primeira-tarefa` | Para quem chega no meio do semestre |

## Rastreabilidade

- Toda **História** referencia a ADR que a justifica, ou declara que não há decisão em jogo
- Toda **Tarefa** pertence a uma História, ou é `Tipo: Débito` com justificativa
- Todo **commit** cita a issue: `#42`
- Todo **PR** fecha a issue: `Closes #42`
- Item que vira `Pronto` sem PR associado é auditado na retrospectiva

## Uma situação comum a evitar

Quadros em que quase tudo fica em "Em andamento" e pouca coisa avança. Duas práticas ajudam:

1. **Limite de trabalho em andamento:** no máximo um item `Em andamento` por pessoa
2. **Item parado há mais de uma semana** vai para a pauta da retrospectiva automaticamente

> O quadro serve menos para registrar o trabalho concluído e mais para tornar visível o
> trabalho parado, que raramente aparece de outra forma.
