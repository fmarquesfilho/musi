# Exercício — escrever uma ADR

**DIM0510 · Sprint 0 · ~30 min · no próprio GitHub**

Sem instalar nada. Você vai ler decisões reais e escrever uma.

---

## Parte 1 — Ler (10 min)

Abra [`docs/decisoes/0002-modelo-de-dominio.md`](../../docs/decisoes/0002-modelo-de-dominio.md).

Responda, em dupla:

1. Qual **alternativa** foi descartada, e o argumento contra ela?
2. Qual **consequência negativa** a ADR declara? Ela é grave?
3. A seção "Como verificar" reconhece que a verificação é parcial. Por que registrar isso?

> A terceira pergunta é a mais relevante. Quando uma ADR reconhece os limites da própria
> verificação, oferece mais informação a quem a consultar depois.

## Parte 2 — Diagnosticar (5 min)

Leia esta ADR mal escrita:

> **ADR-0099 — Usar PostgreSQL**
>
> **Contexto:** precisamos de um banco de dados.
> **Decisão:** vamos usar PostgreSQL.
> **Consequências:** PostgreSQL é robusto, tem boa documentação e é amplamente usado no
> mercado. Vai atender bem às necessidades do projeto.

Liste o que poderia ser melhorado. Há pelo menos quatro pontos.

<details>
<summary>Confira depois de listar</summary>

1. **Contexto genérico.** "Precisamos de um banco" serve para qualquer projeto e não informa
   o que pressionava, que restrições existiam nem o que já estava em uso
2. **Sem alternativas.** Não há registro de que MySQL ou SQLite tenham sido considerados,
   então a ADR documenta um fato consumado em vez de uma decisão
3. **Consequências apenas positivas.** Faltam os custos: hospedagem, operação e migrações
4. **Sem "como verificar"** e sem data nem estado
5. A expressão "atende bem às necessidades" não é verificável

</details>

## Parte 3 — Escrever (15 min)

Escolha **uma** decisão em aberto do MUSI e escreva a ADR:

| Decisão | O que está em jogo |
|---|---|
| **Vocabulário controlado de dimensões** | A ADR-0002 admite que, sem ele, o corpus tende à entropia. Criar um fecha a extensibilidade? |
| **Limite de profundidade da árvore de filtro** | Árvore muito profunda gera consulta cara. Onde cortar, e o que acontece ao ultrapassar? |
| **Anotações conflitantes** | Dois curadores discordam sobre o ritmo de uma obra. O catálogo mostra as duas? Escolhe? |

Use [`docs/decisoes/0000-modelo.md`](../../docs/decisoes/0000-modelo.md).

**Requisitos mínimos:**

- [ ] Pelo menos **duas** alternativas descartadas, com argumento
- [ ] Pelo menos **uma** consequência negativa
- [ ] Uma seção "Como verificar"; se não houver verificação automática possível, registre
      isso explicitamente

## Fechamento

Troque a ADR com outra dupla. A pergunta de revisão é a seguinte:

> **Alguém que chegue daqui a um ano compreende por que a decisão foi tomada, sem precisar
> perguntar a ninguém?**

Quando a resposta é não, geralmente falta contexto, que é o ponto mais comum a melhorar.
