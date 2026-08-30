# Footprint em Kotlin: o Ktor é a melhor opção para o MUSI?

Análise a partir do [benchmark medido](BENCHMARK.md) e do estado da arte (2025–2026). A
pergunta tem duas respostas, porque depende do eixo: **valor didático** ou **footprint de
produção**. Este documento separa os dois e propõe um caminho.

## O dado, primeiro

Medido neste repositório (idle, após ficar pronto):

| Serviço | RSS idle | Startup |
|---|---:|---:|
| Go (`services/`) | ~11 MB | <1 s |
| **Quarkus nativo** | **~47 MB** | **0,028 s** |
| Quarkus JVM | ~113 MB | ~1 s |
| Ktor CIO | ~199 MB | ~2 s |
| Ktor Netty (atual) | ~231 MB | ~2 s |

O Ktor é o mais pesado dos três. Isso **não** o torna errado — mas a pergunta do título é
legítima, então vale entender por quê e o que o estado da arte oferece.

## Por que o Ktor pesa

Três camadas somam:

1. **Netty.** O engine padrão do Ktor no MUSI é o Netty, que mantém *event loops*, arenas de
   *direct buffers* e pools próprios. Boa parte do RSS está **fora do heap** — por isso
   limitar `-Xmx` quase não muda o número (231 → 211 MB). Trocar por **CIO** (engine em
   corrotinas puras) já tira ~30 MB e é o que medi no [Cenário D](BENCHMARK.md).
2. **DI em tempo de execução (Koin).** O grafo é montado e resolvido no arranque, com
   metadados vivos em memória. É o oposto do CDI do Quarkus, resolvido na compilação.
3. **A linha de base da JVM.** Metaspace, *code cache* do JIT, pilhas de thread. É um custo
   fixo que só o **modo nativo (AOT)** remove de verdade.

## O eixo do estado da arte: reflexão em runtime × AOT sem reflexão

A tendência de backend enxuto (2025–2026) gira em torno de **eliminar reflexão e mover
trabalho para o build**, o que também é o que viabiliza *native image*:

- **Micronaut** e **Quarkus** fazem **DI e AOP em tempo de compilação, sem reflexão** — por
  isso compilam para nativo com pouco atrito e chegam a ~50 ms de startup e dezenas de MB.
- **GraalVM Native Image** troca o JIT por AOT: startup 10–50× menor e memória 50–90% menor,
  ao custo de um build mais lento e de ter que declarar toda reflexão.
- Na JVM tradicional, **AppCDS / AOT cache** (JDK 24+, caminho do Project Leyden) e **CRaC**
  atacam sobretudo *startup*; ajudam menos no RSS em regime que o nativo.

Onde o Ktor se encaixa: ele **já usa kotlinx-serialization (sem reflexão)**, o que ajuda. Mas
o suporte a GraalVM tem duas restrições concretas: **exige o engine CIO (Netty não é
suportado)** e **exige configuração de reflexão para dependências Java de terceiros** — no
MUSI, a lib de OpenAPI (`smiley4`) é o candidato a dar trabalho. Ktor nativo é possível, mas
não é *"compila e pronto"*.

## Como reduzir o footprint em Kotlin — do mais barato ao mais radical

| # | Ação | Ganho esperado | Esforço | Mantém o MUSI como está? |
|---|---|---|---|---|
| 1 | **Netty → CIO** no `api-ktor` | ~30 MB (medido: 231→199) | baixo | sim — só troca o engine |
| 2 | `-XX:MaxRAMPercentage` / `-Xmx` ciente de contêiner | pouco (não-heap domina) | trivial | sim |
| 3 | **AppCDS / AOT cache** (JDK 25) | startup e um pouco de RSS | baixo/médio | sim |
| 4 | **Ktor nativo** (CIO + GraalVM + config de reflexão) | grande (dezenas de MB) | **alto** (atrito com libs Java) | muda o build |
| 5 | **Trocar de framework, mantendo Kotlin** | grande | alto | muda a arquitetura |

Detalhe da linha 5 — as três opções sérias, todas em Kotlin:

- **Quarkus com Kotlin.** O Quarkus tem suporte *first-class* a Kotlin, inclusive `suspend` /
  corrotinas nos handlers `quarkus-rest`. Daria para ter **a ergonomia Kotlin com o footprint
  que medi no nativo (~47 MB)**. É a opção "melhor dos dois mundos" em produção.
- **Micronaut.** DI em compilação sem reflexão, nativo por desenho, ~50 ms de startup. Perfil
  muito parecido com o do Quarkus, com boa integração Kotlin.
- **http4k.** *Toolkit* funcional em Kotlin puro: o core tem **zero dependências (~1 MB) e zero
  reflexão**, e **compila para GraalVM sem configuração**. É o mais leve para funções
  serverless — ao custo de ser mais "faça você mesmo" (menos baterias inclusas).

## Então, o Ktor é a melhor opção para o MUSI?

Depende do que o MUSI é. **Ele é um projeto de ensino**, e a razão de o `api-ktor` existir ao
lado do `api-quarkus` é o **contraste** (ADR-0001):

- **Como ferramenta didática: sim, o Ktor é uma boa escolha — justamente por ser o extremo
  oposto do Quarkus.** Explícito × convenção, DI em runtime × em compilação, Netty × servidor
  cloud-native. O footprint maior **é conteúdo**: este benchmark transforma "cloud-native" de
  adjetivo em número. Se os dois serviços tivessem o mesmo perfil, não haveria o que comparar.
- **Como escolha de produção sob a restrição de 512 MB: não, o Ktor-Netty não é a melhor.** Se
  o único critério fosse footprint, a ordem seria Go → Quarkus nativo → Micronaut/Quarkus JVM
  → http4k, com Ktor-Netty por último. E, mantendo Kotlin, **Kotlin-on-Quarkus dominaria o
  Ktor** nesse eixo.

Ou seja: a escolha do Ktor no MUSI se justifica pelo **propósito pedagógico**, não por
footprint. As duas coisas não competem se ficarem explícitas — e é isso que a Tarefa 4 pede ao
aluno: **justificar** a escolha, não achar a "vencedora".

## Recomendação

1. **Manter o Ktor no MUSI** pelo valor de contraste, e **usar este benchmark como parte da
   aula** — inclusive dizendo, com números, que ele é o mais pesado e por quê. Honestidade
   sobre o trade-off é a lição, não um defeito a esconder.
2. **Aplicar a troca Netty → CIO** (linha 1) se quiser reduzir sem perder nada: é barata,
   medida, mantém a ergonomia e ainda abre a porta para o nativo. *(Neste estudo a troca foi
   feita e medida num branch descartável; não está aplicada ao código.)*
3. **Não migrar de framework agora.** Trocar o `api-ktor` por Kotlin-on-Quarkus daria o melhor
   footprint mantendo Kotlin, mas **colapsaria a comparação Kotlin × Java** que é o motivo do
   projeto. É uma decisão de *escopo do curso*, não técnica.
4. **Se algum dia o footprint virar requisito real** (mais serviços no mesmo plano de 512 MB),
   as saídas, em ordem: Quarkus nativo (já provado aqui, ~47 MB), Ktor nativo via CIO (mais
   trabalho), ou http4k para um serviço serverless específico.

> Em uma frase: **para ensinar, o Ktor é uma boa escolha porque é o oposto do Quarkus; para
> caber barato em produção, ele é o último da fila — e o caminho mais leve mantendo Kotlin é o
> nativo (Quarkus/Micronaut/http4k), não o Ktor-Netty.**

## Fontes

- Kotlin no backend (KotlinConf 2025): <https://blog.jetbrains.com/kotlin/2025/08/kotlin-on-the-backend-what-s-new-from-kotlinconf-2025/>
- Parceria JetBrains–Spring: <https://blog.jetbrains.com/kotlin/2025/05/strategic-partnership-with-spring/>
- Benchmark Spring/Quarkus/Micronaut 2025 (RSS nativo): <https://medium.com/@optimzationking2/spring-boot-vs-quarkus-vs-micronaut-the-2025-microservice-benchmark-926e4a62922c>
- Native Image — startup e memória: <https://www.javacodegeeks.com/2025/10/native-image-for-java-microservices-faster-startup-times-and-smaller-memory-footprint.html>
- GraalVM no Ktor (documentação oficial, exige CIO): <https://ktor.io/docs/graalvm.html>
- Ktor nativo com GraalVM (relato prático, atrito com reflexão): <https://dev.to/viniciusccarvalho/building-a-native-ktor-application-with-graalvm-1hgh>
- Quarkus com Kotlin (suporte oficial, corrotinas): <https://quarkus.io/guides/kotlin>
- Micronaut + GraalVM: <https://www.javacodegeeks.com/2025/04/micronaut-graalvm-the-future-of-native-microservices.html>
- http4k nativo (zero-dep, zero-reflexão): <https://www.http4k.org/tutorial/going_native_with_graal_on_aws_lambda/>
- Reduzir startup na JVM (AppCDS, CRaC, Leyden) — Spring I/O 2025: <https://2025.springio.net/sessions/four-approaches-to-reducing-java-startup-time-appcds-native-image-project-leyden-crac/>
