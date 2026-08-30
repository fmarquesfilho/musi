# Como rodar o projeto

Três caminhos. **O das aulas desta semana é o Codespaces** — comece por ele.

| Caminho | Instala algo? | Para quê |
|---|---|---|
| **1. Codespaces** | não | Aula, prova, laboratório sem admin |
| **2. Docker Desktop** | Docker | No seu computador, sem instalar JDK/Go |
| **3. Ferramentas locais** | JDK, Go, Gradle | Ciclo mais rápido, hot reload |

> Para **publicar** — subir a aplicação no Render ou divulgar o repositório — veja
> [COMO-PUBLICAR.md](COMO-PUBLICAR.md). Este arquivo é só sobre rodar.

Cada comando aqui vem com o **atalho `mise run`** equivalente (as tarefas estão no `mise.toml`,
e são as mesmas que o CI usa). No Codespace o `mise` **já vem instalado** — a feature do `mise`
o instala e o `postCreateCommand` provisiona as ferramentas do `mise.toml` na criação do
contêiner — então os atalhos funcionam de fábrica; os comandos completos valem como alternativa.

---

## 1. Codespaces — o ambiente das aulas

No repositório: botão **Code** → aba **Codespaces** → **Create codespace on main**.

O `.devcontainer/devcontainer.json` monta o ambiente sozinho: instala o `mise` e provisiona as
ferramentas declaradas no `mise.toml` (JDK 25, Go 1.27, Maven, Python, buf, arch-go), além do
Docker e da área de trabalho noVNC. Todo mundo recebe exatamente as mesmas versões, de uma fonte
só. Nada precisa ser instalado à mão.

### Rodar os testes

```bash
./verificar.sh                    # atalho: mise run verificar
```

Roda tudo que funciona sem configuração: contratos, documentação (DIM0510) e os testes de
Kotlin (`shared`, `api-ktor`, `app`), Java (`api-quarkus`) e Go. Para rodar um stack só:

```bash
./gradlew :api-ktor:test          # atalho: mise run test:api-ktor     — Kotlin (DIM0547)
cd api-quarkus && mvn -q test     # atalho: mise run test:api-quarkus  — Java (DIM0547)
cd services && go test ./...      # atalho: mise run test:go           — Go (DIM0547)
./gradlew :app:jvmTest            # atalho: mise run test:app          — app Compose (DIM0524)
```

### Subir as APIs (DIM0547)

O serviço de busca em Go é a base das duas APIs. Suba-o **uma vez**, num terminal, e deixe
rodando:

```bash
cd services && go run ./cmd/servidor      # atalho: mise run run:busca   — porta 9090
```

Depois, em **outro** terminal, suba **só a API que você usa**:

```bash
# Só a api-ktor (Kotlin) — porta 8080
./gradlew :api-ktor:run                   # atalho: mise run run:api-ktor
```

```bash
# Só a api-quarkus (Java) — porta 8081, com hot reload
cd api-quarkus && mvn quarkus:dev -Dquarkus.http.port=8081   # atalho: mise run run:api-quarkus
```

Para subir **as duas ao mesmo tempo**, rode os dois comandos acima em terminais separados
(Ktor na 8080, Quarkus na 8081), com o Go já no ar. As duas respondem a mesma coisa, contra o
mesmo serviço de busca.

> **Alunos de Web II não precisam do Compose.** A API sobe sozinha; a tela do app (seção
> seguinte) é conteúdo da disciplina de Móveis.

Para testar: na aba **Ports** (Portas), abra a porta **8080** (ou **8081**) e acrescente o
caminho do Swagger — `/swagger` na api-ktor, `/q/swagger-ui` na api-quarkus. Ou, no terminal:

```bash
curl -s "localhost:8080/obras?dimensao=ritmo&valor=baiao" | python -m json.tool   # atalho: mise run demo
```

A pasta [`http/`](../http/) traz coleções prontas (Bruno, Postman/Insomnia/Hoppscotch e um
`.http` para VS Code) apontando para cada API. Ver [`http/README.md`](../http/README.md).

### Ver a tela do Compose (DIM0524)

O app desktop usa dados de exemplo em memória — **não precisa das APIs no ar**.

1. Na aba **Ports**, abra a porta **6080**. Uma nova aba mostra a Área de Trabalho (noVNC). Se
   pedir senha, é `vscode`.
2. No terminal do Codespace:
   ```bash
   DISPLAY=:1 ./gradlew :app:run -Pheadless    # atalho: mise run run:app
   ```
   O `-Pheadless` faz o app renderizar por software, já que o noVNC não tem GPU. A janela do
   MUSI aparece na aba do noVNC e fica aberta até você parar com `Ctrl-C`. O atalho `mise run
   run:app` detecta o Codespace e já acrescenta o `DISPLAY=:1` e o `-Pheadless` por você.
3. Para **hot reload** (recompila e recarrega a tela ao salvar):
   ```bash
   DISPLAY=:1 ./gradlew :app:hotRunJvm -Pheadless    # atalho: mise run run:app-hot
   ```

### Quando o MUSI muda: atualizar o seu Codespace

Se o professor anunciar uma mudança no projeto, atualize antes de continuar:

- **Mudou só o código** (código-fonte, testes): no terminal do Codespace, rode `git pull`.
- **Mudou o ambiente** (`.devcontainer/`, versões de ferramentas, dependências): o contêiner
  precisa ser **refeito**. Abra a paleta de comandos (`F1`, ou `Ctrl/Cmd+Shift+P`) e escolha
  **"Codespaces: Rebuild Container"**. Se o erro persistir, use **"Codespaces: Full Rebuild
  Container"**, que ignora o cache.
- Alternativa infalível: apague o Codespace atual e **crie um novo** (Code → Codespaces).

> Regra prática: erro de ferramenta ou de versão logo depois de um `git pull` = refaça o
> contêiner. Erro só de código = `git pull` resolve.

### Cota

Estudantes têm 120 core-hours por mês pelo GitHub Student Pack. Um codespace de 2 núcleos
consome 2 por hora, o que dá cerca de 60 horas mensais. **Pare o codespace ao terminar**, em
**Codespaces → Stop**, para economizar a cota.

---

## 2. Docker Desktop — só o Docker instalado

```bash
git clone https://github.com/fmarquesfilho/musi
cd musi
docker compose up --build
```

Sobem **três** containers: as duas APIs e o serviço Go. Para subir **só uma API** (com o
serviço de busca, do qual ela depende):

```bash
docker compose up musi-api-ktor musi-busca       # só Ktor (8080)
docker compose up musi-api-quarkus musi-busca    # só Quarkus (8081)
```

| Serviço | Porta | Teste |
|---|---|---|
| `api-ktor` (Kotlin) | 8080 | `curl -s "localhost:8080/obras?dimensao=ritmo&valor=baiao"` |
| `api-quarkus` (Java) | 8081 | `curl -s "localhost:8081/obras?dimensao=ritmo&valor=baiao"` |
| `busca` (Go) | 9090 | `curl -s localhost:9090/health` |

O `mem_limit: 512m` reproduz o limite da instância gratuita do Render. Container que morre sem
erro na aplicação costuma ser o OOM killer — que é o que o `JAVA_TOOL_OPTIONS` evita.

---

## 3. Ferramentas locais — ciclo mais rápido

Aqui os atalhos `mise run` são o caminho principal. Cada um mostra, no comentário, o comando
completo que ele executa.

```bash
curl https://mise.run | sh      # https://mise.jdx.dev
mise install                    # instala JDK 25, Go, Maven, Python, buf (do mise.toml)
mise run setup                  # == pip install -r requirements-dev.txt
mise run verificar              # == ./verificar.sh
```

Só uma API, ou as duas, em terminais separados:

```bash
mise run run:busca              # == cd services && go run ./cmd/servidor        (Go, 9090)
mise run run:api-ktor           # == ./gradlew :api-ktor:run                     (Kotlin, 8080)
mise run run:api-quarkus        # == mvn quarkus:dev -Dquarkus.http.port=8081    (Java, 8081)
```

A tela do Compose (com GPU local, o `run:app` não precisa do `-Pheadless`):

```bash
mise run run:app                # == ./gradlew :app:run         (a tela do Compose)
mise run run:app-hot            # == ./gradlew :app:hotRunJvm   (hot reload)
```

`mise tasks` lista tudo. As mais usadas:

| Tarefa | O que faz | Equivale a |
|---|---|---|
| `mise run verificar` | Tudo que roda sem configuração | `./verificar.sh` |
| `mise run test` | Testes dos três stacks | `./gradlew … :app:jvmTest` + `go test` |
| `mise run ci` | O pipeline inteiro, como no GitHub Actions | os `test:*` em sequência |
| `mise run run:app` | Sobe a tela do Compose (noVNC no Codespace) | `./gradlew :app:run` |
| `mise run demo` | Uma busca de exemplo contra a api local | `curl … /obras…` |
| `mise run docker:tamanhos` | Tamanho das três imagens | `docker build` + `docker images` |
