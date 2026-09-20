# Retrospectiva 01 — Sprint 0 e primeira metade da Sprint 1

**Data:** 19/09/2026 · **Participantes:** Fernando (equipe de um) · **Período coberto:**
25/08 (primeiro commit) a 19/09.

Os números estão em [`metricas-01.md`](metricas-01.md), com o comando que produz cada um.
Esta retrospectiva não traz impressão sem fato: onde não havia dado, está escrito que não
havia.

> Nas disciplinas, o arquivo pedido é `docs/retrospectiva-01.md`. No MUSI, os artefatos de
> processo ficam em [`processo/`](README.md); é o mesmo documento, noutro diretório.

A pauta é a fixa dos [rituais](rituais.md): itens parados, itens que viraram "Pronto" sem PR,
ADRs contrariadas pelo código e uma mudança de processo.

---

## 1. Fatos

### 1.1 O quadro não foi usado

3 cartões, criados em 28/08, todos em `Todo`. Nenhuma mudança de status em 22 dias. O quadro
nasceu depois de 20 dos 43 commits, e sua última alteração é de 28/08, 23:13.

As colunas e os limites que o [acordo](acordo-de-processo.md) declara não existem no quadro:
ele tem `Todo`, `In Progress` e `Done`, do modelo do GitHub, sem limite de WIP e sem política
de coluna escrita. Os campos `Tipo`, `Componente`, `Tamanho` e `Risco` existem e estão
preenchidos.

Consequência direta: as três métricas que o acordo prometia (itens por sprint, tempo em
revisão, itens parados) não puderam ser calculadas a partir do quadro.

### 1.2 O trabalho não passou por pull request

32 dos 43 commits (74%) foram direto na `main`. Houve um pull request, o #1, com 10 commits,
590 linhas acrescentadas e 198 removidas. Ele ficou aberto 48 minutos e foi integrado sem
nenhuma revisão registrada.

A Definição de Pronto exige "revisado por outra pessoa, em pull request", e o acordo prevê,
para equipe de um, "revisão 24 h depois, com a lista da Definição de Pronto". Nem os 48
minutos nem os 32 commits diretos cumprem essas regras.

Sobre o item de pauta *itens que viraram "Pronto" sem PR*: no quadro, nenhum item chegou a
"Pronto". Fora do quadro, quase todo o trabalho chegou à `main` sem PR.

### 1.3 A `main` ficou vermelha por 25,8 horas

O CI rodou 18 vezes: 13 verdes, 4 vermelhas, 1 cancelada, com duração mediana de 3,4 min.
Três falhas foram corrigidas em 7, 11 e 13 minutos. A quarta, em 26/08 às 17:46, ficou 25,8
horas de pé.

A causa é pequena e verificável: o commit `807a496` acrescentou a ADR-0003 sem incluí-la no
índice, e o job `docs` reprovou em `verificar_adrs.py`. O `./verificar.sh`, que roda essa
mesma verificação em segundos, existe desde o primeiro commit e não foi executado antes do
push.

### 1.4 O débito registrado ficou parado

As 3 issues abertas em 28/08 (duas de débito técnico e uma tarefa), todas nascidas da análise
do PR #1, continuam abertas 22 dias depois, sem movimentação. Elas têm gatilho e custo
descritos, como a Definição de Pronto pede para débito — mas nenhuma entrou numa sprint.

### 1.5 Um hiato de 18 dias, e um incremento fora do fluxo

Entre 01/09 e 19/09 não houve commit. O incremento da Sprint 1 (CRUD de obras e anotações,
persistência com PostgreSQL e Flyway, testes de integração, teste de arquitetura e a
ADR-0004) foi construído em 19/09, numa sessão só, sem cartão, sem branch e sem PR — o mesmo
padrão descrito em 1.1 e 1.2, agora na sprint que avalia justamente o fluxo.

### 1.6 ADRs: nenhuma contrariada

Quatro ADRs aceitas, três da Sprint 0 e a ADR-0004 de 19/09. Não se encontrou código
contrariando decisão: a ADR-0002 é verificada pelo CI (nenhum escalar de qualidade), a
ADR-0001 exige as duas APIs em paralelo e as duas foram alteradas no mesmo passo, e a
ADR-0003 mantém o MusicBrainz fora do ciclo de leitura.

---

## 2. Causas

### 2.1 Por que o quadro não foi usado — cinco porquês

1. **Por que nenhum cartão se moveu?** Porque o trabalho não começou por cartão; começou no
   código.
2. **Por que começou no código?** Porque o quadro só foi criado em 28/08, quando 20 dos 43
   commits já existiam.
3. **Por que foi criado tão tarde?** Porque a urgência era ter o esqueleto do projeto de
   referência de pé (três stacks, contratos e CI) antes da primeira aula que o citaria.
4. **Por que o quadro não entrou junto?** Porque, para uma pessoa que tem o plano todo na
   cabeça, o quadro não devolvia informação nova: o custo de atualizar aparecia antes do
   benefício.
5. **Por que o custo aparece antes?** Porque o fluxo real — editar, commitar, empurrar para a
   `main` — não passa pelo quadro em nenhum momento. Não há gatilho nem obstáculo: nada
   lembra, nada impede.

**Causa raiz:** o quadro estava ao lado do trabalho, não no caminho dele. Enquanto commitar
direto na `main` for o caminho mais curto, qualquer quadro fica desatualizado — com uma
pessoa ou com quatro.

### 2.2 Por que o PR foi integrado em 48 minutos sem revisão

Mesma raiz, outro sintoma: a regra das 24 horas do acordo depende só de memória. A `main`
não tem proteção de branch, então o autor podia (e pôde) integrar sozinho, na hora, sem
nada barrar. Uma regra sem mecanismo é uma intenção.

### 2.3 Por que a `main` ficou um dia vermelha

O push foi no fim da tarde e a falha só foi vista no dia seguinte. A verificação local
existia e custa segundos, mas rodá-la também dependia de memória. Como o CI leva 3,4 min,
nem esperar o CI era hábito.

---

## 3. Ações

Três ações, com responsável e prazo. O acordo limita a **uma** mudança de processo por
retrospectiva, e ela é a A1; as outras duas são ajustes pontuais, não mudanças de política.

| # | Ação | Responsável | Prazo | Como verificar |
|---|---|---|---|---|
| A1 | Pôr o quadro no caminho do trabalho: renomear as colunas para as do acordo, criar "Em revisão", escrever a política de cada coluna, declarar os limites de WIP (2 e 2) e abrir um cartão para cada item da Sprint 1, ligado ao PR que o resolve | Fernando | 26/09 | `gh project item-list 3 --owner fmarquesfilho` mostra cartões fora de `Todo` e com PR vinculado |
| A2 | Proteger a `main`: exigir pull request e CI verde para integrar, desligando o push direto | Fernando | 22/09, antes de commitar o incremento da Sprint 1 | `gh api repos/fmarquesfilho/musi/branches/main/protection` responde com `required_status_checks`; nenhum commit direto na `main` depois da data |
| A3 | Tornar o `./verificar.sh` automático antes do push, com um hook versionado (`.githooks/pre-push` e `git config core.hooksPath .githooks`) | Fernando | 26/09 | Nenhuma falha de CI, até 02/10, causada por verificação que o script já cobre |

A A2 é a que sustenta as outras duas: sem ela, A1 e A3 voltam a depender de memória.

## 4. Revisão do acordo de processo

O [acordo](acordo-de-processo.md) muda em três pontos, todos por contradição com o que
aconteceu:

| O que o acordo diz | O que aconteceu | O que passa a valer |
|---|---|---|
| Métricas vêm dos *Insights* do GitHub Projects | Com 3 cartões parados, o Insights não produz nada | As métricas vêm do git, dos PRs e do CI, pelo `gh`, como em [`metricas-01.md`](metricas-01.md). O quadro volta a ser fonte quando a A1 estiver de pé |
| Limites de WIP de 2 e 2, em "Em progresso" e "Em revisão" | As colunas não existem no quadro | Os limites continuam, e a A1 os torna reais. Um limite que não está no quadro não é limite |
| Em equipe de um, a revisão é feita 24 h depois | O único PR foi integrado em 48 min | A regra continua, apoiada pela A2: com proteção de branch, integrar exige PR e CI verde, e a espera passa a ser deliberada |

Fica **sem mudança**, e registrado como pendência: a Definição de Pronto diz que a linha "no
ar em produção" seria acrescentada na Sprint 1. Ela não foi acrescentada, e agora depende da
[ADR-0004](../docs/decisoes/0004-persistencia-postgresql-flyway.md) — até a Sprint 3 o deploy
vai sem banco, então "no ar" significa a busca funcionando e o CRUD respondendo 503. A
decisão fica para a retrospectiva 02.

## 5. O que funcionou, e por quê

Vale registrar o que não precisa mudar:

- **O CI existe desde o primeiro commit** e roda em 3,4 min (mediana). Foi ele que pegou a
  ADR fora do índice — uma verificação de documentação, não de código.
- **A revisão do PR #1 virou trabalho registrado**: as 3 issues abertas naquele dia descrevem
  custo e gatilho. Estão paradas (1.4), mas existem, e não viraram comentário perdido.
- **Commits pequenos**: mediana de 22 linhas. Só 2 dos 43 passaram de 500, e o maior é o
  commit inicial.
- **As ADRs têm alternativas e consequências negativas**, e o `verificar_adrs.py` recusa as
  que não têm.

## 6. Próxima retrospectiva

A retrospectiva 02, ao fim da Sprint 2 (23/10), começa conferindo as três ações acima com os
comandos da coluna "como verificar". Ação sem verificação vira impressão, e impressão não
muda processo.
