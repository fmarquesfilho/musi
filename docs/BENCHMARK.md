# Benchmark inicial — footprint de memória

Medição do **RSS** (Resident Set Size, memória residente real) de cada componente de backend
do MUSI, em repouso após ficar pronto e após uma carga curta de requisições. Serve para dar
números concretos à comparação de stacks da [ADR-0001](decisoes/0001-stacks-e-estrutura.md) e
para a discussão de DIM0547. **É uma linha de base, não um veredito.**

Reproduza com:

```bash
./benchmark/medir-memoria.sh
```

## Ambiente da medição

| Item | Valor |
|---|---|
| Máquina | Apple Silicon (arm64), macOS |
| Java | OpenJDK 26 (projeto compila com toolchain 25) |
| Go | 1.26 (toolchain do módulo: 1.27) |
| Empacotamento JVM | Quarkus *fast-jar*; Ktor `installDist` (sem uber-jar) |
| Amostragem | mediana de 3 leituras de `ps -o rss`, em repouso e após 100 requisições |
| Dependências | cada serviço medido **isolado** (os demais não no ar) |

## Resultados

### Cenário A — flags padrão (out-of-the-box)

| Componente | Stack | RSS idle | RSS pós-carga | Startup |
|---|---|---:|---:|---:|
| `services/` (Go) | Go · net/http | **~11 MB** | ~12 MB | <1 s |
| `api-quarkus` | Java 25 · Quarkus · CDI | **~113 MB** | ~123 MB | ~1 s |
| `api-ktor` | Kotlin · Ktor · Koin · Netty | **~231 MB** | ~246 MB | ~2 s |

### Cenário B — JVM com `-Xmx128m` (contêiner apertado, ~512 MB)

| Componente | RSS idle | RSS pós-carga |
|---|---:|---:|
| `api-quarkus` | ~116 MB | ~125 MB |
| `api-ktor` | ~211 MB | ~216 MB |

### Cenário C — Quarkus em modo nativo (GraalVM/Mandrel)

Compilado com `-Dquarkus.native.enabled=true` no builder `ubi9-quarkus-mandrel-builder-image:jdk-25`
e medido rodando em contêiner Linux (RSS ≈ memória do cgroup via `docker stats`).

| Componente | RSS idle | RSS pós-carga | Startup |
|---|---:|---:|---:|
| `api-quarkus` **nativo** | **~47 MB** | ~60 MB | **0,028 s** |

*Referência JVM do mesmo serviço: ~113 MB e ~1 s.* O nativo usa **~40% da memória** e sobe
**~35× mais rápido**. O preço é o build: exige GraalVM/Mandrel e leva minutos, contra segundos
do fast-jar.

### Cenário D — Ktor: engine Netty vs. CIO

O engine é a maior alavanca de footprint dentro do próprio Ktor. Trocar Netty por **CIO** (o
engine em corrotinas puras do próprio Ktor) foi medido sem mudar mais nada:

| Engine | RSS idle | RSS pós-carga |
|---|---:|---:|
| Netty (atual) | ~228 MB | ~243 MB |
| **CIO** | **~199 MB** | ~208 MB |
| CIO + `-Xmx128m` | ~185 MB | ~194 MB |

CIO tira ~30 MB e cresce menos sob carga. **E é o único engine que o Ktor suporta para compilar
nativo com GraalVM** (Netty não é suportado). Ver [FOOTPRINT-KOTLIN.md](FOOTPRINT-KOTLIN.md).

### Tamanho dos artefatos

| Componente | Tamanho |
|---|---:|
| binário Go (estático) | ~9 MB |
| binário Quarkus **nativo** (self-contained) | ~59 MB |
| `api-quarkus` (quarkus-app, precisa de JVM) | ~22 MB |
| `api-ktor` (libs, precisa de JVM) | ~37 MB |

## Como ler estes números

- **Go pesa ~20× menos que o Ktor e ~10× menos que o Quarkus.** É código nativo, sem máquina
  virtual: o runtime é mínimo. É coerente com a decisão de usar Go para trabalho de sistema
  (a conciliação — [ADR-0003](decisoes/0003-conciliacao-musicbrainz.md)).
- **Quarkus usa cerca de metade do Ktor**, com o mesmo contrato e as mesmas extensões
  (OpenAPI + Swagger). É o efeito do desenho *cloud-native*: **DI resolvida em compilação**
  (CDI processa o grafo no build, com menos reflexão e metadados em runtime) e servidor
  próprio enxuto. Confirma na prática o motivo da [ADR-0001](decisoes/0001-stacks-e-estrutura.md)
  ter escolhido Quarkus e não Spring Boot para caber nos 512 MB do plano gratuito.
- **Limitar o heap quase não muda o Ktor** (231 → 211 MB). O peso não está no heap: é Netty,
  metadados de classe, *code cache* do JIT e pilhas de thread. A lição para a turma: **RSS não
  é heap** — cortar `-Xmx` tem limite.
- **Ambos cabem em 512 MB com folga** (idle 116–216 MB). O plano gratuito do Render é viável
  para as duas APIs JVM. É o número que sustenta a frase da ADR-0001.
- **O modo nativo muda a escala**: Quarkus nativo em ~47 MB e 28 ms aproxima o custo do Go, e
  cabe onde a JVM aperta. É o argumento *cloud-native* saindo do slide e virando número. O
  Ktor não tem equivalente pronto no MUSI hoje (só via CIO + configuração de reflexão) — ver a
  análise dedicada em [FOOTPRINT-KOTLIN.md](FOOTPRINT-KOTLIN.md).

## Ligações com a aula (DIM0547)

- **Runtime × compile-time** deixa de ser abstrato: a DI em compilação do Quarkus aparece como
  ~115 MB contra ~230 MB do Ktor. É o mesmo eixo dos slides de verbosidade — só que agora
  medido em memória, não em linhas.
- **"Go por característica"**: os 11 MB explicam por que o serviço de conciliação, limitado por
  taxa e tolerante a falha, roda barato como processo separado.
- **Trade-off, não ranking**: o Ktor troca memória por explicitude e ergonomia Kotlin; o
  Quarkus troca um build mais elaborado por leveza em runtime. A Tarefa 4 pede para **justificar**
  a escolha — este benchmark dá um eixo a mais para a justificativa.

## Limites desta medição

- **Uma amostra, uma máquina.** Apple Silicon/macOS; o alvo de deploy é Linux x86 em contêiner.
  Os números mudam de plataforma, e a JVM dimensiona ergonomia pela RAM visível.
- **Métricas não idênticas entre linhas.** Go/JVM foram medidos por `ps` (RSS de processo no
  host, arm64/macOS); o Quarkus nativo por `docker stats` (memória do cgroup, arm64/Linux).
  São grandezas próximas, mas não a mesma régua — o nativo estava em contêiner por ser um
  executável Linux. A ordem de grandeza da comparação se mantém.
- **Sem carga sustentada.** Mede footprint de partida e aquecimento leve, não throughput,
  latência sob concorrência, nem RSS em regime. Isso é trabalho de um benchmark de desempenho,
  não de footprint.
- **RSS inclui bibliotecas compartilhadas mapeadas**; comparar a mesma métrica entre processos
  é justo, mas o número absoluto não é "memória que só este processo consumiu".

> Próximos passos sugeridos: repetir em Linux/contêiner com a mesma régua para todos, medir sob
> carga concorrente com `hey`/`wrk` para separar footprint de throughput, e — se valer a pena —
> tentar o Ktor nativo (CIO + GraalVM), cujo caminho está descrito em
> [FOOTPRINT-KOTLIN.md](FOOTPRINT-KOTLIN.md).
