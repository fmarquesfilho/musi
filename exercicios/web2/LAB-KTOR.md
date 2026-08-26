# Laboratório — arquitetura poliglota com Ktor e Go

**DIM0547 · Sprints 1 e 2 · ~4 h, distribuídas**

Ao final, você terá uma `api/` em Ktor que atende HTTP e conversa com um serviço em Go, com
o domínio num módulo compartilhado que o app móvel também importa.

> Pré-requisitos: `mise`, e depois `mise install` — ele instala JDK 21, Gradle e Go nas
> versões corretas. Docker é necessário para a Parte 5.

---

## Antes de começar: o que já existe

```bash
cd musi && ./verificar.sh
mise tasks
```

```
musi/
 ├── shared/      o domínio — importado por api E app
 ├── api/         Ktor + Koin
 ├── app/         Compose Multiplatform
 └── services/    Go
```

Guarde esta observação, porque ela é o critério de sucesso do laboratório: **o domínio existe
uma vez só.** Antes da [ADR-0001](../../docs/decisoes/0001-stacks-e-estrutura.md), ele existia em
Java e em Kotlin, mantido em sincronia por testes compartilhados.

---

## Parte 1 — Injeção de dependência reunida num arquivo

### 1.1 Leia o grafo

Abra `api/src/main/kotlin/br/ufrn/musi/Modulos.kt`. Ele cabe numa tela:

```kotlin
single { HttpClient(CIO) { install(ContentNegotiation) { json() } } }
single<FonteDeObras> { BuscaHttp(cliente = get(), urlBase = urlBusca) }
single { BuscarObras(fonte = get()) }
```

**Para o relatório:** quem fornece `FonteDeObras`? Responda lendo só este arquivo.

Compare com procurar a resposta num projeto com anotações espalhadas por vinte classes.

### 1.2 O experimento que vale a aula

Comente a linha do `BuscarObras`:

```kotlin
// single { BuscarObras(fonte = get()) }
```

Rode `./gradlew :api:run` e peça `GET /obras?dimensao=ritmo&valor=baiao`.

| O que aconteceu | Quando você descobriu |
|---|---|
| Koin | Erro **na inicialização**, ou na primeira injeção |
| DI em tempo de compilação | Erro de **compilação** |

Registre a mensagem exata. Este é o custo declarado da ADR-0001, e vale entendê-lo:
ganhamos uma pilha unificada e perdemos a verificação em tempo de compilação.

### 1.3 A mitigação

```bash
./gradlew :api:test --tests '*ModulosTest*'
```

`verify()` percorre o grafo e falha se algo não resolver. Não é compilação — mas move a
descoberta do deploy para o CI.

**Para o relatório:** o teste pegou o `single` comentado? Descomente e confirme que volta a
passar.

---

## Parte 2 — O serviço Go

### 2.1 Suba

```bash
mise run run:busca
curl -s localhost:9090/buscar \
  -d '{"tipo":"tem","dimensao":"ritmo","valor":"baiao"}' | jq
```

### 2.2 Compare as duas traduções

Abra lado a lado:

| Arquivo | O que faz |
|---|---|
| `services/cmd/servidor/main.go` — `filtroDTO.paraDominio` | JSON → domínio, em Go |
| `api/.../adaptadores/busca/Dtos.kt` — `FiltroDto.paraDominio` | JSON → domínio, em Kotlin |

Os dois resolvem o mesmo problema: `Filtro` é uma união, e JSON não sabe instanciar uniões.

**Para o relatório:** acrescente um construtor `Perto` ao `Filtro` **só do lado Kotlin** e
rode `./gradlew :shared:jvmTest`. Quantos lugares o compilador aponta? Agora faça o mesmo do
lado Go. Quantos ele aponta?


---

## Parte 3 — Cache, sem escrever cache

Suba a api e observe os cabeçalhos:

```bash
mise run run:api
curl -si "localhost:8080/obras?dimensao=ritmo&valor=baiao" | head -20
```

Em `Aplicacao.kt`, duas linhas produzem isso:

```kotlin
install(CachingHeaders) { options { _, _ -> CachingOptions(CacheControl.MaxAge(60)) } }
install(ConditionalHeaders)
```

**Para o relatório:**

1. Qual `Cache-Control` veio? E o `ETag`?
2. Repita com `If-None-Match`. Que status voltou, e o corpo veio?
3. Compare com o que vocês observaram na API pública, na aula 02

---

## Parte 4 — Observabilidade

```bash
curl -s localhost:8080/health | jq
```

O Ktor não traz `/health` pronto: a rota está em `Rotas.kt`, escrita à mão.

**Para o relatório:** o `/health` detecta que o serviço Go caiu? Derrube o processo Go e
verifique. Se não detectar, o que faltaria? Escreva a versão que detecta.

> Fator IX do 12-Factor App: processos descartáveis precisam declarar o próprio estado.

---

## Parte 5 — Container em 512 MB

### 5.1 Construa

```bash
mise run docker:tamanhos
```

Repare no `Dockerfile` da api: o contexto do build é a **raiz** do repositório, e não `api/`,
porque o módulo `:api` depende de `:shared`. É uma consequência direta da decisão de
compartilhar o domínio.

### 5.2 Rode com o limite do plano gratuito

```bash
docker run --rm -p 8080:8080 -m 512m \
  -e MUSI_BUSCA_URL=http://host.docker.internal:9090 musi-api
docker stats --no-stream
```

**Para o relatório**, três números: tamanho da imagem, memória em repouso, e tempo até o
primeiro `/health` responder.

### 5.3 Experimente

Remova `-XX:MaxRAMPercentage=70` do `Dockerfile` e repita. Sem essa opção, uma JVM antiga
assumiria a memória da **máquina**, e não a do container.

---

## Entrega

No `docs/lab-ktor.md` do repositório do grupo:

- [ ] A mensagem de erro do experimento 1.2, e se o `ModulosTest` a pegou
- [ ] Quantos lugares o compilador apontou em Kotlin, e quantos em Go, na Parte 2
- [ ] Os cabeçalhos de cache observados, e o resultado do `If-None-Match`
- [ ] Sua versão do `/health` que detecta o serviço Go fora do ar
- [ ] Os três números da Parte 5
- [ ] **Uma pergunta em aberto**


---

## Uma observação honesta sobre o mercado

**Java lidera as vagas de backend no Brasil**, com folga, e Spring Boot é o framework
predominante. Este curso usa Kotlin porque ele unifica as duas disciplinas numa pilha só, e
porque os conceitos aparecem com menos cerimônia.

Os conceitos transferem: injeção de dependência, portas e adaptadores, contrato separado de
implementação, cache no protocolo. Quem os entende aqui aprende Spring depois sem
dificuldade.

O caminho inverso costuma ser mais difícil, porque é fácil decorar anotações sem perceber
qual padrão elas implementam.

Vale ter as duas coisas no currículo, e saber explicar a diferença numa entrevista.
