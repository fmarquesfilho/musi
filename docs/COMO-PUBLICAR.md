# Publicar

## 1. Criar o wrapper do Gradle

O wrapper não está no repositório, e sem ele ninguém consegue rodar `./gradlew`:

```bash
gradle wrapper --gradle-version 8.14.3
git add gradlew gradlew.bat gradle/wrapper/
```

O `gradle-wrapper.jar` deve ser commitado: é ele que garante que todo mundo usa a mesma
versão do Gradle sem precisar instalá-la. O `.gitignore` já abre exceção para ele.

## 2. Verificar

```bash
./verificar.sh
mise run test
```

## 3. Remover artefatos já commitados

Se `.gradle/`, `build/` ou `.idea/` estiverem versionados:

```bash
git rm -r --cached .gradle build .idea
```

Arquivo já rastreado continua sendo rastreado, mesmo depois de entrar no `.gitignore`.

## 4. Histórico limpo

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

Antes de rodar: guarde um clone do estado atual em outro diretório, e confirme que não há
nada no histórico a preservar. O `push -f` não tem volta pelo GitHub.

> Lockfile de dependência é outra coisa e deve ser commitado: `go.sum`, `gradle.lockfile`,
> `package-lock.json`. Aqui não há nenhum, porque `services/go.mod` não tem dependência
> externa.
