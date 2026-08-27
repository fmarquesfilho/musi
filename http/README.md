# Testar as APIs do MUSI

Coleções prontas para testar as três APIs rodando localmente. Suba tudo com:

```bash
docker compose up --build
```

| Serviço | Base | Swagger / OpenAPI |
|---|---|---|
| API Ktor (Kotlin) | `http://localhost:8080` | `/swagger` · spec em `/openapi.json` |
| API Quarkus (Java) | `http://localhost:8081` | `/q/swagger-ui` · spec em `/q/openapi` |
| Serviço de busca (Go) | `http://localhost:9090` | — (sem OpenAPI; use as coleções) |

## Qual arquivo usar

| Ferramenta | Arquivo | Como abrir |
|---|---|---|
| **IntelliJ / Android Studio**, **VS Code** (REST Client) | `musi.http` | Abra e clique em *Run* acima de cada requisição |
| **Bruno** (open-source, versionável em git) | `bruno/` | *Open Collection* → aponte para a pasta `bruno/` |
| **Postman**, **Insomnia**, **Hoppscotch** | `musi.postman_collection.json` | *Import* → selecione o arquivo |

O `musi.http` é o mais direto para quem já usa IntelliJ ou Android Studio: não instala nada.

## Equivalente ao Postman no navegador

**Hoppscotch** (`hoppscotch.io`) roda no navegador, sem cadastro, e faz o mesmo papel do
Postman. Importe o `musi.postman_collection.json` em *Collections → Import*, **ou** importe
direto o OpenAPI da API:

```
https://hoppscotch.io  →  Import  →  OpenAPI  →  cole  http://localhost:8080/openapi.json
```

Como o Ktor e o Quarkus já expõem OpenAPI, dá para gerar a coleção a partir do spec, sem
depender destes arquivos.

## As requisições

Cada coleção traz, por serviço:

- **Health** — `GET /health` (Quarkus: `/q/health`)
- **Busca simples** — `GET /obras?dimensao=ritmo&valor=baiao`
- **Busca composta** — `POST /obras` com a árvore de filtro no corpo (no Go: `POST /buscar`)

A árvore de filtro segue `contratos/filtro.schema.json`. Exemplo (mpb **ou** forró, **e** até 1970):

```json
{
  "tipo": "e",
  "exigencias": [
    { "tipo": "ou", "opcoes": [
      { "tipo": "tem", "dimensao": "genero", "valor": "mpb" },
      { "tipo": "tem", "dimensao": "genero", "valor": "forro" }
    ]},
    { "tipo": "ate", "ano": 1970 }
  ]
}
```

As bases ficam em variáveis (`ktor`, `quarkus`, `busca`) — no Bruno, no ambiente `Local`;
no `.http` e na coleção Postman, no topo do arquivo. Troque-as para apontar para produção.
