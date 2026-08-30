# Publicar

Duas coisas diferentes: **subir a aplicação no Render** (o que os grupos entregam) e
**divulgar o repositório** (empacotar o projeto para distribuição). Para apenas *rodar* — nas
aulas, localmente ou no Codespaces — veja [COMO-RODAR.md](COMO-RODAR.md).

---

## Deploy no Render

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

O Neon limita conexões concorrentes no plano gratuito, então o pool fica em 5, já configurado
nos dois `application.properties`.

---

## Divulgar o repositório

Passos para empacotar o projeto e distribuir com um histórico limpo.

### 1. Criar o wrapper do Gradle

O wrapper não está no repositório, e sem ele ninguém consegue rodar `./gradlew`:

```bash
gradle wrapper --gradle-version 9.7.1
git add gradlew gradlew.bat gradle/wrapper/
```

O `gradle-wrapper.jar` deve ser commitado: é ele que garante que todo mundo usa a mesma versão
do Gradle sem precisar instalá-la. O `.gitignore` já abre exceção para ele.

### 2. Verificar

```bash
./verificar.sh
mise run test
```

### 3. Remover artefatos já commitados

Se `.gradle/`, `build/` ou `.idea/` estiverem versionados:

```bash
git rm -r --cached .gradle build .idea
```

Arquivo já rastreado continua sendo rastreado, mesmo depois de entrar no `.gitignore`.

### 4. Histórico limpo

Para divulgar com um commit só:

```bash
git checkout --orphan divulgacao
rm -rf .gradle build .idea
git add -A
git commit -m "MUSI — projeto de exemplo das disciplinas DIM0510, DIM0524 e DIM0547"

git branch -D main
git branch -m main
git push -f origin main
```

Antes de rodar: guarde um clone do estado atual em outro diretório, e confirme que não há nada
no histórico a preservar. O `push -f` não tem volta pelo GitHub.

> Lockfile de dependência é outra coisa e deve ser commitado: `go.sum`, `gradle.lockfile`,
> `package-lock.json`. Aqui não há nenhum, porque `services/go.mod` não tem dependência
> externa.
