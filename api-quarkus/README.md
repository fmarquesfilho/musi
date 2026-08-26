# `api-quarkus` — a API em Java

É a mesma API que existe em [`../api-ktor/`](../api-ktor/), em Java 25 com Quarkus.

Ver [ADR-0001](../docs/decisoes/0001-stacks-e-estrutura.md).

## O que comparar, arquivo a arquivo

| Conceito | Aqui | Do lado Ktor |
|---|---|---|
| Domínio | `dominio/Dominio.java` | `shared/.../Dominio.kt` |
| Porta | `aplicacao/FonteDeObras.java` | `aplicacao/FonteDeObras.kt` |
| Caso de uso | `aplicacao/BuscarObras.java` | `aplicacao/BuscarObras.kt` |
| Injeção de dependência | anotações CDI | `Modulos.kt`, explícito |
| Rotas | anotações em `ObraResource` | `Rotas.kt`, código |
| Cliente do serviço Go | `ClienteBusca`, interface | `BuscaHttp`, classe |
| Erro `problem+json` | `FiltroInvalidoMapper` | plugin `StatusPages` |

## Diferenças entre as abordagens

| | Quarkus + CDI | Ktor + Koin |
|---|---|---|
| Grafo de dependências | resolvido na **compilação** | resolvido na **execução** |
| Dependência faltando | o build falha | falha na inicialização |
| Onde ler o grafo | espalhado por anotações | reunido num arquivo |

## Rodar

```bash
./mvnw quarkus:dev        # modo dev, com hot reload
./mvnw test
```

| Endpoint | O que é |
|---|---|
| `/obras?dimensao=ritmo&valor=baiao` | Busca simples |
| `/q/health` | Estado da aplicação |
| `/q/openapi` · `/q/swagger-ui` | Contrato gerado |
