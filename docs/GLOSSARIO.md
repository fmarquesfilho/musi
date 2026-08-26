# Glossário

Uma palavra por conceito. Se dois documentos usarem termos diferentes para a mesma coisa,
vale abrir uma issue para alinhar o vocabulário.

| Termo | Significado no MUSI |
|---|---|
| **Obra** | Uma gravação catalogada |
| **Faceta** | Par `(dimensão, valor)` que caracteriza uma obra |
| **Dimensão** | O eixo da faceta: `ritmo`, `genero`, `movimento` |
| **Filtro** | Árvore de critérios que o usuário monta para buscar |
| **Anotação** | Faceta atribuída por um curador identificável, com data |
| **Curador** | Pessoa que assina uma anotação |
| **Acervo** | O conjunto de obras catalogadas |

## Dimensões conhecidas

Esta lista é uma convenção, não uma restrição: quem anota pode introduzir dimensões novas,
e isso faz parte do modelo. Ela existe para evitar que surjam termos como `ritmo` e
`padrao-ritmico` com o mesmo significado.

| Dimensão | Exemplos de valor |
|---|---|
| `genero` | `mpb`, `forro`, `samba`, `frevo`, `axe` |
| `ritmo` | `ijexa`, `baiao`, `ponteio`, `maracatu`, `coco`, `xote` |
| `movimento` | `tropicalia`, `jovem-guarda`, `manguebeat`, `clube-da-esquina` |
| `instrumentacao` | `sanfona`, `viola-caipira`, `berimbau`, `rabeca` |
| `regiao` | `recife`, `salvador`, `belo-horizonte` |

**Valores em minúsculas, sem acento e com hífen.** É uma convenção de escrita que garante a
correspondência em consultas como `Tem("ritmo", "ijexa")`.

## Termos que evitamos

| Não usar | Usar | Motivo |
|---|---|---|
| Tag | **Faceta** | Tag é livre e não tem dimensão |
| Categoria | **Dimensão** ou **valor** | Ambíguo entre os dois |
| Recomendação | **Busca** ou **seleção** | Os critérios são sempre de quem consulta |
| Score, nota, ranking | — | Não existem no modelo. ADR-0002 |
| Playlist automática | **Resultado de busca** | Os critérios partem de quem consulta |
