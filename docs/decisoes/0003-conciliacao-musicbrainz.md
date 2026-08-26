# ADR-0003 — Conciliação com o MusicBrainz, fora do ciclo de leitura

**Estado:** Aceita
**Data:** 2026-08-26

## Contexto

O MUSI identifica obras por `obra-01`, `obra-02` — identificadores locais, sem significado fora do projeto. Duas instalações que catalogassem a mesma gravação não teriam como saber disso.

O MusicBrainz mantém uma base aberta de identidade musical, com MBIDs estáveis. As duas bases são complementares: o MusicBrainz responde *quem gravou o quê, quando e em que lançamento*; o MUSI responde *que ritmo, que movimento, que instrumentação* — caracterização interpretativa, que o MusicBrainz evita por princípio editorial.

Três restrições moldam a integração:

O MusicBrainz limita o consumo a cerca de uma requisição por segundo. Chamá-lo duranteuma busca tornaria o MUSI mais lento que a API externa, e o derrubaria junto quando ela falhasse.

Boa parte da música regional brasileira não está lá: coco, maracatu de baque solto, gravações de selo pequeno.

Casamento automático por título produz falsos positivos em quantidade, sobretudo com regravações e homônimos.

## Decisão

O MusicBrainz distingue *work*, a composição, de *recording*, a gravação. `Obra` é uma gravação, então são dois campos:

```kotlin
data class Obra(
    val id: String,
    val mbid: String?,             // recording — único por obra
    val mbidComposicao: String?,   // work — compartilhado por regravações
    ...
)
```

Duas obras com o mesmo `mbid` são duplicata. Duas obras com o mesmo `mbidComposicao` são gravações diferentes da mesma composição, que é o caso normal.

A conciliação — casar uma obra local com um MBID — acontece na importação, nunca na leitura, e a escolha entre candidatos é humana. **Conciliação** é o termo consagrado em ciência da informação para casar registros locais com base de autoridade.

```
  Curador cadastra obra
        │
        ├── busca candidatos no MusicBrainz
        ├── escolhe entre eles
        └── grava o MBID junto com a obra

  Depois disso, toda busca é local.
```

O trabalho em lote vive em `services/conciliacao`, em Go: percorre obras sem MBID, consulta
uma por segundo e registra os candidatos para revisão.

O acesso ao MusicBrainz é uma porta na camada de aplicação:

```kotlin
interface CatalogoExterno {
    suspend fun buscarCandidatos(titulo: String, artista: String): List<Candidato>
}
```

O domínio não sabe que o MusicBrainz existe.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| MBID como chave primária | O MUSI só catalogaria o que já está no MusicBrainz, o que contradiz a razão do projeto existir |
| Um MBID só, sem distinguir composição de gravação | Original e regravação apontariam para o mesmo identificador, e não haveria como saber se é duplicata ou repertório compartilhado |
| Consultar o MusicBrainz durante a busca | Uma requisição por segundo torna a busca inviável, e acopla a disponibilidade do MUSI à de terceiro |
| Casar candidatos automaticamente por título | Falsos positivos com regravações e homônimos; a revisão humana é o que dá valor ao vínculo |
| Importar o dump completo | Dezenas de gigabytes, e o MUSI passaria a manter cópia desatualizada de base que não é dele, sem ganhar nada — a caracterização por facetas continua sendo trabalho do curador |
| Conciliação na `api`, sem serviço separado | O trabalho é limitado por taxa, tolerante a falha parcial e fora do ciclo de requisição: é o caso em que Go se justifica por característica |

## Consequências

- Obras ganham identidade global quando ela existe, sem excluir as que não têm
- A busca continua local, e a disponibilidade do MUSI não depende de terceiro
- A conciliação é um exemplo de trabalho que justifica o serviço em Go: limitado por taxa, assíncrono e tolerante a falha parcial
- A porta `CatalogoExterno` mantém os testes sem rede e deixa aberta a troca por outra base
- Original e regravação convivem, ligadas pelo `mbidComposicao`. "Todas as gravações de Asa Branca" passa a ser uma consulta
- A composição não é entidade: não há onde guardar autoria, ano de composição ou o nomecanônico. Isso exigiria `Composicao` como entidade própria, com relacionamento.
- O vínculo envelhece: MBIDs são estáveis, mas entidades podem ser mescladas no MusicBrainz. Não há reconciliação periódica prevista. Isso porém não é relevante para o MVP.
- A revisão humana é gargalo, mas por enquanto isso não será resolvido pois se trata de um MVP.

## Como verificar

Nenhum código em `api-ktor/` ou `api-quarkus/` chama o MusicBrainz durante uma requisição de
leitura. O acesso externo só aparece em `services/conciliacao` e no caso de uso de
importação.

Os testes do domínio e da aplicação continuam rodando sem rede, com uma implementação em
memória de `CatalogoExterno`.
