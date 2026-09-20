# O domínio

Quatro tipos. Eles aparecem em Java, Go e Kotlin, e a comparação entre as três versões é
parte do que o projeto ensina.

## Obra

Uma gravação catalogada.

| Campo | Tipo | Nota |
|---|---|---|
| `id` | texto | Identificador **local** (`obra-01`), sempre presente |
| `titulo` | texto | — |
| `artista` | texto | Nome principal creditado |
| `ano` | inteiro | Ano da gravação, não do lançamento |
| `facetas` | lista de Faceta | Pode ser vazia |
| `mbid` | texto? | MBID da **gravação** no MusicBrainz; nulo até conciliar — ADR-0003 |
| `mbidComposicao` | texto? | MBID da **composição** (work), compartilhado por regravações — ADR-0003 |

O `id` local e o `mbid` global são campos distintos: uma obra sempre tem `id`, e ganha `mbid`
quando é conciliada com o MusicBrainz. A conciliação acontece na importação, fora do ciclo de
leitura, e a escolha entre candidatos é humana — ver [ADR-0003](decisoes/0003-conciliacao-musicbrainz.md)
e o serviço `services/conciliacao`.

## Faceta

Um par `(dimensão, valor)`. A lista de dimensões não é fixa.

```
ritmo=ijexa
movimento=tropicalia
instrumentacao=sanfona
regiao=recife
```

### Por que um par, e não um campo por dimensão

Porque não é possível saber de antemão quais dimensões serão necessárias. Em algum momento
alguém vai querer anotar `afinacao`, `contexto-de-gravacao` ou `tipo-de-baqueta`. Com campos
fixos, cada dimensão nova exige mudança de schema, migração e nova versão da API. Com pares,
é apenas mais um dado.

**Há um custo associado:** o compilador não verifica os valores, então `Faceta("rítmo", ...)`
com acento ou `Faceta("genero", "MPB")` em maiúscula são aceitos e não correspondem a nada
nas buscas. Ganha-se extensibilidade e perde-se verificação. A resposta usual para isso é um
vocabulário controlado, que o MUSI não implementa nesta versão.

> As dimensões já em uso estão listadas em [`GLOSSARIO.md`](GLOSSARIO.md). São uma
> convenção de escrita, e não uma restrição técnica.

## Filtro

Uma **árvore**. Um filtro contém outros filtros.

| Construtor | Significa |
|---|---|
| `Tem(dimensao, valor)` | A obra tem esta faceta |
| `Ou(opcoes)` | Ao menos um dos filtros vale |
| `E(exigencias)` | Todos os filtros valem |
| `Exceto(filtro)` | O filtro **não** vale |
| `Ate(ano)` | A obra é daquele ano ou anterior |

Cinco construtores são suficientes para representar buscas de qualquer profundidade, sem
necessidade de casos especiais.

```
E(
  Ou(Tem(ritmo, ijexa), Tem(ritmo, baiao)),
  Ate(1969),
  Exceto(Tem(genero, axe))
)
```

**Esta é a ideia central do projeto:** a busca não é uma string com sintaxe própria nem um
conjunto fixo de campos de formulário. É uma estrutura de dados que a pessoa usuária monta e
que o sistema apenas avalia.

## Anotação

Uma faceta atribuída por alguém identificável.

| Campo | Nota |
|---|---|
| `obraId` | A obra anotada |
| `faceta` | O par atribuído |
| `curador` | Identificador de quem afirma |
| `criadoEm` | Data |

Uma obra pode ter anotações divergentes de curadores diferentes, e isso é previsto pelo
modelo. O catálogo registra quem afirma o quê, sem arbitrar entre as afirmações.

No banco, é uma relação 1:N com chave estrangeira, e a API a expõe como rota aninhada
(`/obras/{id}/anotacoes`). A única restrição é que o mesmo curador não repete a mesma faceta
na mesma obra; dois curadores, sim — ver
[ADR-0004](decisoes/0004-persistencia-postgresql-flyway.md).

---

## O que o modelo deliberadamente não tem

O modelo não tem campos de nota, relevância, popularidade ou qualidade, conforme a
[ADR-0002](decisoes/0002-modelo-de-dominio.md). A ordenação usa critérios declarados por quem
consulta.
