# ADR-0002 — Faceta extensível, filtro como árvore, sem ranking

**Estado:** Aceita
**Data:** 2026-08-26

## Contexto

O MUSI cataloga gravações e permite buscá-las por características que serviços de streaming
não expõem: ritmo, instrumentação, movimento, região.

Três questões de modelagem apareceram juntas, e se sustentam mutuamente.

**Como caracterizar uma obra.** O modelo direto seria um campo por dimensão — `genero`,
`ritmo`, `movimento`. É mais seguro em tipo e mais simples de consultar em SQL, e é um
conjunto fechado.

**Como o usuário monta a busca.** Formulário de campos fixos só expressa conjunção. String
com sintaxe própria exige escrever um analisador e ensinar a sintaxe.

**Como ordenar resultados.** A tentação é um campo de nota ou relevância, que resolve
ordenação e alimenta uma tela de destaques.

## Decisão

**Faceta é um par `(dimensão, valor)`**, e uma obra tem uma lista deles. Introduzir uma
dimensão nova é inserir dado, não alterar schema.

**Filtro é uma árvore**, com cinco construtores: `Tem`, `Ou`, `E`, `Exceto`, `Ate`. Os
compostos contêm outros filtros. A árvore trafega entre cliente e API, e o serviço a avalia.

**Nenhum escalar de qualidade** no modelo. Nenhuma struct, campo, coluna ou parâmetro
expressa qualidade, relevância, acurácia ou popularidade. A ordenação usa critérios
declarados por quem consulta.

Os dados de exemplo são inventados para o curso e publicados em CC0. Obras e artistas são
reais; as anotações sobre eles não são de ninguém.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Um campo por dimensão | Cada dimensão nova exigiria migração, nova versão da API e novo release do app |
| Enum de dimensões permitidas | Continua fechado, e precisaria ser sincronizado entre três linguagens |
| Campos fixos de formulário para a busca | Só expressa conjunção; não representa "ou" nem "exceto" |
| Linguagem de consulta textual | Exige analisador, tratamento de erro de sintaxe e ensino da sintaxe |
| Filtro plano, com operador por item | Não aninha; `(a ou b) e (c ou d)` fica inexprimível |
| Nota do curador de 1 a 5 | Reintroduz o escalar com outro nome |
| Ordenar por relevância | Relevância sem critério declarado é ranking anônimo |
| Importar dados do MusicBrainz | Cria dependência de rede em exercício de laboratório |

## Consequências

- ✅ Dimensão nova é dado, e o app antigo continua funcionando
- ✅ Cinco construtores representam buscas de qualquer profundidade, sem casos especiais
- ✅ Em Kotlin e em Java, `sealed` torna o `when` e o `switch` exaustivos: acrescentar um
  construtor quebra a compilação onde falta tratar
- ✅ O repositório pode ser copiado por qualquer estudante, sem obrigações inesperadas
- ⚠️ O compilador não verifica os valores das facetas: `Faceta("rítmo", …)` com acento é
  aceito e não corresponde a nada nas buscas
- ⚠️ Sem vocabulário controlado, o corpus tende a acumular variações do mesmo termo. Esta
  versão não trata essa questão
- ⚠️ Consulta em SQL exige junção com a tabela de facetas, não uma coluna
- ⚠️ Go não tem união etiquetada, e o `switch` sobre `Filtro` não é verificado pelo
  compilador. A comparação entre as três linguagens é conteúdo da disciplina
- ⚠️ A tela de listagem precisa de outro princípio de organização que não seja ranking

## Como verificar

Os exemplos de `contratos/exemplos/` incluem filtros aninhados com resultado esperado, e os
três componentes rodam os mesmos casos.

O job `docs` do CI recusa os termos `score`, `rating`, `relevance`, `popularity` e `ranking`
no código. É verificação simples, que cobre os casos comuns.

Todo arquivo em `contratos/exemplos/` declara `"exemploDidatico": true`.
