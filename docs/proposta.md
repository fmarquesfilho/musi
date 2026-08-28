# Proposta — MUSI

> **Exemplo de referência.** Este documento segue a estrutura pedida em
> `docs/SPRINT-0.md` das três disciplinas ao mesmo tempo. O de vocês precisa
> atender só à sua, e cabe em **5 páginas**.
>
> Este é mais longo porque cobre as três e explica as escolhas. Copiem a estrutura,
> não a extensão.

---

## 1. Visão do produto

```
Para  ouvintes que querem entender uma gravação além do gênero
Que   não encontram como buscar por ritmo, instrumentação ou movimento
O     MUSI é uma enciclopédia musical com busca por facetas
Que   permite combinar critérios livremente, com E, OU e exceto
Diferente de  catálogos de streaming, organizados por gênero comercial
Nosso produto deixa quem busca definir o que é relevante, não o catálogo
```

**Hipótese de valor:** acreditamos que ouvintes com interesse em música brasileira
vão montar buscas compostas — como *"ijexá ou baião, até 1969, exceto axé"* —
porque hoje precisam saber de antemão o nome do artista para chegar a esse recorte.

**Como saberemos:** na Sprint 2, medimos quantas buscas usam mais de um critério.
Se quase todas usarem um só, a hipótese está errada e o produto precisa mudar.

---

## 2. MVP

| No MVP | Fora do MVP |
|---|---|
| Catálogo de obras com facetas extensíveis | Reprodução de áudio |
| Busca composta com `E`, `Ou`, `Exceto`, `Ate` | Recomendação automática |
| Anotação assinada por curador | Contas de usuário e login social |
| Importação de acervo em JSON | Importação de MusicBrainz ao vivo |
| App móvel com construtor visual de filtro | Área administrativa web |

**O que fica de fora é decisão de engenharia.** Reprodução de áudio exigiria
licenciamento; recomendação automática contradiz a ADR-0002, que recusa qualquer
escalar de qualidade no modelo.

---

## 3. Backlog inicial

No GitHub Projects, com os campos descritos em
[`processo/backlog.md`](../processo/backlog.md).

Quadro: `github.com/fmarquesfilho/musi/projects`

| Prio | História | Critérios de aceitação | Sprint |
|---|---|---|---|
| P1 | Como ouvinte, quero buscar obras por ritmo para achar o que gênero não descreve | Busca por uma faceta devolve só quem a tem; sem resultado mostra mensagem | 1 |
| P1 | Como ouvinte, quero combinar critérios para chegar a um recorte preciso | `E`, `Ou` e `Exceto` aninhados em três níveis; casos de `contratos/exemplos/` passam | 1 |
| P1 | Como curador, quero anotar uma obra para registrar o que sei | Anotação com autor e data; duas anotações divergentes coexistem | 2 |
| P2 | Como ouvinte, quero que a busca seja rápida para não esperar | `ETag` nos `GET`; `304` na requisição condicional | 2 |
| P2 | Como ouvinte, quero usar sem sinal para consultar em qualquer lugar | Acervo em cache local; indicador de dado desatualizado | 3 |

Backlog completo: 12 itens, todos priorizados, 8 estimados.

---

## 4. Decisões técnicas, por disciplina

### 4.1 Divisão Kotlin × Go — *DIM0547*

| Vai para a `api/` em Kotlin | Vai para `services/` em Go |
|---|---|
| Entidades de domínio e regras de composição de filtro | Avaliação da busca sobre o acervo |
| Persistência e migrações | Trabalho concorrente de E/S |
| Orquestração dos casos de uso | Cache e pré-computação de consultas frequentes |
| Fronteira HTTP pública e tratamento de erro | — |

**A justificativa parte da característica do trabalho, não da preferência.**

A avaliação de um filtro composto sobre milhares de obras é **paralelizável e
intensiva em E/S**: cada ramo da árvore pode ser avaliado independentemente. Go
resolve isso com goroutines, sem cerimônia, e com consumo de memória que cabe nos
512 MB.

O CRUD e as regras de composição são **transacionais e cheios de invariantes** — um
`Filtro.Ou` vazio precisa se comportar de forma definida, um `Ate` precisa validar o
ano. É onde `sealed interface` e exaustividade de `when` pagam por si.

> Contraexemplo do que **não** justifica: *"queríamos aprender Go"*. A rúbrica
> avalia se a característica do trabalho sustenta a escolha.

### 4.2 Plataforma-alvo e backend — *DIM0524*

**Plataforma-alvo: Android.** O público de referência usa majoritariamente Android
no Brasil, e a equipe não tem acesso garantido a Mac. Interface em Compose
Multiplatform, com o alvo desktop para o ciclo rápido de desenvolvimento.

**Backend: API própria**, a mesma `api/` de Web II. É a opção que exercita o contrato
entre app e servidor, e dá direito ao bônus de integração.

### 4.3 Acordo de processo — *DIM0510*

Em [`processo/acordo-de-processo.md`](../processo/acordo-de-processo.md).

---

## 5. Entidades principais do domínio

```
  Obra ──< Faceta          (dimensão, valor) — lista aberta, ADR-0002
   │
   └──< Anotação           faceta atribuída por um Curador, com data

  Filtro                   árvore: Tem · Ou · E · Exceto · Ate — ADR-0002
```

O domínio vive em [`shared/`](../shared/), módulo Kotlin **importado por `api/` e por
`app/`** — existe uma vez, não duas.

---

## 6. Equipe

| Nome | Matrícula | Papel |
|---|---|---|
| Fernando Figueira Filho | — | Product Owner e desenvolvedor |

> Projeto de exemplo, mantido por uma pessoa. O de vocês tem de 1 a 4 integrantes,
> com papéis declarados.

---

## 7. Coorte e integração

**Coorte:** A, presencial.

**Integração entre disciplinas:** este mesmo produto atende às três, com entregáveis
distintos — `api/` e `services/` em Web II, `app/` em Móveis, e o processo em
DIM0510. Quem fizer o mesmo tem direito ao bônus de integração descrito em
`docs/AVALIACAO.md`.
