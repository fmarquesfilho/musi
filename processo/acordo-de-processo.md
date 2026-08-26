# Acordo de processo — MUSI

> Exemplo de referência para o item 5 de `docs/proposta.md` em DIM0510.
>
> Este acordo será **confrontado com a realidade** nas sprints seguintes. Ele pode
> mudar; o que não pode é não existir.

---

## Cadência

| | |
|---|---|
| Duração da sprint | 3 semanas, alinhada ao cronograma da disciplina |
| Planejamento | Primeira segunda-feira da sprint, 30 min |
| Fechamento | Sexta-feira da última semana, 23:59 |
| Revisão e retrospectiva | Segunda seguinte à entrega |

---

## Cerimônias

| Cerimônia | Frequência | Duração | Pergunta que responde |
|---|---|---|---|
| Planejamento | por sprint | 30 min | Por que esta sprint tem valor? |
| Acompanhamento assíncrono | 2× por semana | escrito | O que impede o objetivo? |
| Revisão | por sprint | 20 min | O incremento serve? |
| Retrospectiva | por sprint | 30 min | O que mudamos no processo? |

**Não adotamos reunião diária.** Com equipe pequena que se encontra em aula, ela
repetiria o que o quadro já mostra. O acompanhamento é escrito, no canal do projeto.

> Essa escolha é uma adaptação consciente do Scrum, e não um descuido.

---

## Definição de Pronto

Um item sai de "Em revisão" quando **todos** os itens abaixo são verdade:

- [ ] Código integrado na `main`
- [ ] Revisado por outra pessoa, em pull request
- [ ] Teste automatizado que **falharia** sem a mudança
- [ ] Pipeline de CI verde no job do componente tocado
- [ ] Critérios de aceitação do item verificados
- [ ] Se mudou `contratos/`, os componentes afetados atualizados no **mesmo PR**
- [ ] Uso de IA declarado no corpo do PR

**Ainda não exigimos** "no ar em produção", porque o deploy automático só entra na
Sprint 1. A definição evolui, e essa linha será acrescentada.

---

## Papéis

| Papel | Quem | Responsabilidade |
|---|---|---|
| Product Owner | Fernando | Ordena o backlog; aceita ou recusa itens |
| Facilitador | rotativo por sprint | Conduz cerimônias, remove impedimentos |
| Desenvolvimento | toda a equipe | Constrói o incremento |

**Quem revisa quem:** todo PR precisa de uma aprovação de alguém que não o escreveu.
Em equipe de um, a revisão é feita 24 h depois, com a lista da Definição de Pronto.

---

## Ferramentas

| Para quê | Onde |
|---|---|
| Backlog e quadro | GitHub Projects |
| Conversas | Canal do projeto no Discord |
| Decisões | `docs/decisoes/`, em ADRs |
| Métricas de fluxo | Insights do GitHub Projects |
| Código e revisão | GitHub, com pull request obrigatório |

---

## Limites de trabalho em progresso

| Coluna | WIP máximo |
|---|---|
| Em progresso | 2 por pessoa |
| Em revisão | 2 no total |

O limite em "Em revisão" é o mais baixo de propósito: revisão parada é a forma mais
comum de trabalho invisível.

**Se o limite for atingido**, ninguém puxa item novo — ajuda-se a destravar o que
está parado.

---

## Como mediremos

| Métrica | De onde vem | Para quê |
|---|---|---|
| Itens concluídos por sprint | Quadro | Previsão, a partir da terceira sprint |
| Tempo em "Em revisão" | Histórico do quadro | Detectar revisão como gargalo |
| Itens parados > 1 semana | Visão *Parados* | Pauta automática da retrospectiva |

**Nenhuma métrica compara pessoas.** Elas descrevem o fluxo, não quem trabalha.
