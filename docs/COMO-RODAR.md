# Como rodar o projeto

| Caminho | Instala algo? | Para quê |
|---|---|---|
| **1. Codespaces** | não | Aula, prova, laboratório sem admin |
| **2. Docker Desktop** | Docker | Desenvolvimento no seu computador |
| **3. Ferramentas locais** | JDK, Go, Gradle | Ciclo mais rápido, hot reload |

E, no fim, **como publicar no Render**.

---

## 1. Codespaces — nada instalado

No repositório: botão **Code** → aba **Codespaces** → **Create codespace on main**.

O `.devcontainer/devcontainer.json` monta o ambiente sozinho: JDK 21, Gradle, Maven, Go 1.27,
Python e Docker. Todo mundo recebe exatamente as mesmas versões.

**Para testar a interface gráfica (Compose Desktop) no Codespaces:**
1. Na aba **Ports** (Portas) do terminal, clique no endereço da porta **6080**.
2. Uma nova aba se abrirá com a Área de Trabalho (noVNC). A senha padrão é `vscode`.
3. No terminal do Codespaces, rode o comando: 
   `DISPLAY=:1 ./gradlew :app:jvmRun -Dskiko.renderApi=SOFTWARE`
4. A janela do MUSI aparecerá na área de trabalho virtual! Para reabrir automaticamente ao salvar, adicione `--continuous` ao comando Gradle.

```bash
mise run verificar          # confere tudo
mise run test               # os três stacks
```

Estudantes têm 120 core-hours por mês pelo GitHub Student Pack. Um codespace de 2 núcleos
consome 2 por hora, o que dá cerca de 60 horas mensais. Pare o codespace ao terminar, em
**Codespaces → Stop**, para economizar a cota.

---

## 2. Docker Desktop — só o Docker instalado

```bash
git clone https://github.com/fmarquesfilho/musi
cd musi
docker compose up --build
```

Sobem **três** containers: as duas APIs e o serviço Go.

| Serviço | Porta | Teste |
|---|---|---|
| `api-ktor` (Kotlin) | 8080 | `curl -s "localhost:8080/obras?dimensao=ritmo&valor=baiao"` |
| `api-quarkus` (Java) | 8081 | `curl -s "localhost:8081/obras?dimensao=ritmo&valor=baiao"` |
| `busca` (Go) | 9090 | `curl -s localhost:9090/health` |

As duas APIs respondem a mesma coisa, contra o mesmo serviço Go.

Para explorar os endpoints sem `curl`, a pasta [`http/`](../http/) traz coleções prontas
(Bruno, Postman/Insomnia/Hoppscotch e um `.http` para IntelliJ/VS Code) e aponta para o
Swagger de cada API. Ver [`http/README.md`](../http/README.md).

Se quiser só uma delas:

```bash
docker compose up musi-api-ktor musi-busca
docker compose up musi-api-quarkus musi-busca
```

O `mem_limit: 512m` reproduz o limite da instância gratuita do Render. Container que morre
sem erro na aplicação costuma ser OOM killer, que é o que o `JAVA_TOOL_OPTIONS` evita.

---

## 3. Ferramentas locais — ciclo mais rápido

```bash
curl https://mise.run | sh      # https://mise.jdx.dev
mise install                    # instala JDK 21, Gradle, Maven, Go e Python
mise run setup                  # dependências Python dos scripts
mise run verificar
```

Em três terminais:

```bash
mise run run:busca              # Go, porta 9090
mise run run:api-ktor           # Kotlin, porta 8080
mise run run:api-quarkus        # Java, porta 8081, com hot reload
```

`mise tasks` lista tudo. As mais usadas:

| Tarefa | O que faz |
|---|---|
| `mise run verificar` | Tudo que roda sem configuração |
| `mise run test` | Testes dos três stacks |
| `mise run ci` | O pipeline inteiro, como no GitHub Actions |
| `mise run demo` | Uma busca de exemplo contra a api local |
| `mise run docker:tamanhos` | Tamanho das três imagens |

---

## Publicar no Render

Plano gratuito, **sem cartão de crédito**.

### Uma vez

1. Crie a conta em `render.com` e conecte o GitHub
2. **New** → **Blueprint** → escolha o repositório

O Render lê o `render.yaml` e cria os três serviços. Cada grupo publica só a sua API: apague
do `render.yaml` a que não vai usar.

### A ordem importa

O `musi-busca` sobe primeiro, porque as APIs dependem da URL dele.

```
1. Deixe o musi-busca terminar o deploy
2. Copie a URL pública: https://musi-busca-XXXX.onrender.com
3. Na API: Environment → MUSI_BUSCA_URL → cole a URL → Save
4. A API reimplanta sozinha
```

### Variáveis de ambiente

| Chave | Valor | Onde |
|---|---|---|
| `MUSI_BUSCA_URL` | a URL pública do serviço Go | painel |
| `JAVA_TOOL_OPTIONS` | `-Xmx300m -Xms150m` | já vem do `render.yaml` |
| `DATABASE_URL` | string de conexão do Neon | painel, na Sprint 2 |

### Banco: Neon, não Render

O Postgres gratuito do Render **expira em 30 dias** e apaga os dados. Use o Neon:

1. Conta gratuita em `neon.tech`, sem cartão
2. Crie um projeto, copie a *connection string*
3. Cole em `DATABASE_URL`, no painel do Render

O Neon limita conexões concorrentes no plano gratuito, então o pool fica em 5, já
configurado nos dois `application.properties`.
