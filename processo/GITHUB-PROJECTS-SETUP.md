# Montagem do GitHub Project — passo a passo

Roteiro para criar o quadro descrito em [`backlog.md`](backlog.md). Leva ~15 min.

## 1. Criar o Project

No perfil ou organização: **Projects → New project → Board**. Nome: `MUSI`.

Torne-o **público**, para que sirva de exemplo em aula.

## 2. Campos

`Settings → Custom fields`. Crie nesta ordem:

| Campo | Tipo | Opções |
|---|---|---|
| `Tipo` | Single select | História · Tarefa · Defeito · Decisão · Débito |
| `Componente` | Single select | docs · contratos · api · services · app |
| `Tamanho` | Single select | P · M · G |
| `Risco` | Single select | Baixo · Alto |
| `Sprint` | Iteration | 4 iterações, alinhadas ao cronograma |

`Status` já vem pronto. Ajuste as colunas para: **A fazer · Em andamento · Em revisão ·
Pronto**.

## 3. Visões

`+ New view` para cada uma:

| Nome | Layout | Filtro | Agrupar por |
|---|---|---|---|
| Quadro | Board | — | `Status` |
| Web II | Table | `Componente:api,services,contratos` | `Sprint` |
| Móveis | Table | `Componente:app,contratos` | `Sprint` |
| Roadmap | Roadmap | — | `Sprint` |
| **Parados** | Table | `Status:"Em andamento"`, ordenado por atualização crescente | — |

## 4. Automação

`Settings → Workflows`. Ative apenas:

- **Item added to project** → `Status: A fazer`
- **Pull request merged** → `Status: Pronto`
- **Item closed** → `Status: Pronto`

Convém não automatizar além disso. Automações que movem cartões em excesso fazem o quadro
se distanciar da realidade, e sua utilidade depende de ser confiável.

## 5. Etiquetas

`Issues → Labels`. Só três, além das que já vêm:

| Etiqueta | Cor | Descrição |
|---|---|---|
| `bloqueado` | vermelho | Acompanhada de comentário indicando por quem ou por quê |
| `precisa-decisao` | amarelo | Precisa de ADR antes de ser trabalhado |
| `boa-primeira-tarefa` | verde | Para quem chega no meio do semestre |

## 6. Modelo de issue

Crie `.github/ISSUE_TEMPLATE/historia.yml`:

```yaml
name: História
description: Um resultado observável para alguém
body:
  - type: textarea
    id: resultado
    attributes:
      label: Resultado
      description: Escreva como resultado, não como atividade.
      placeholder: O leitor encontra obras combinando ritmo e movimento.
    validations: { required: true }
  - type: textarea
    id: aceite
    attributes:
      label: Critérios de aceitação
      description: Verificáveis por alguém que não escreveu este item.
    validations: { required: true }
  - type: input
    id: adr
    attributes:
      label: ADR relacionada
      description: Número da ADR, ou "nenhuma decisão em jogo".
    validations: { required: true }
  - type: textarea
    id: risco
    attributes:
      label: Se Risco Alto — o que se aprende caso falhe?
```

O último campo é especialmente útil: leva a declarar a hipótese antes de dedicar a sprint ao
item.

## 7. Primeiros itens

Vale popular o quadro com 8 a 10 itens antes da primeira aula. Um quadro vazio dá pouca
referência, e itens genéricos demais não representam bem o trabalho real.

Inclua intencionalmente:

- Um item `Risco: Alto` com hipótese declarada
- Um item `precisa-decisao` sem ADR ainda
- Um `Débito` com gatilho
- **Um item parado**, para a visão *Parados* ter o que mostrar

### Sugestões de itens reais do MUSI

| Item | Tipo | Componente | Tamanho | Risco |
|---|---|---|---|---|
| Leitor encontra obras por ritmo | História | app | M | Baixo |
| Traduzir árvore de filtro para SQL | História | api | M | **Alto** |
| Vocabulário controlado de dimensões | Decisão | contratos | P | Baixo |
| `buf breaking` no CI a partir da Sprint 2 | Tarefa | contratos | P | Baixo |
| Limite de profundidade da árvore de filtro | Débito | services | P | Baixo |

O segundo item ilustra bem o `Risco: Alto`: ainda não se sabe se a tradução recursiva para
SQL gera uma consulta com desempenho aceitável, e é justamente essa informação que o item
entrega.
