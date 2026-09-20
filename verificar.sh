#!/usr/bin/env bash
# MUSI - verifica tudo que roda sem configuração.
#
#     ./verificar.sh
#
# Cada componente é pulado quando a ferramenta correspondente não está
# instalada. Os testes de integração (PostgreSQL via Testcontainers e Dev
# Services) precisam de Docker: sem ele, rodam só os demais, com aviso.

set -uo pipefail
cd "$(dirname "$0")"

falhas=0
titulo() { printf '\n\033[1m== %s\033[0m\n' "$1"; }
pular()  { printf '   (pulado: %s)\n' "$1"; }

# Sem Docker, os testes marcados `integracao` ficam de fora (ADR-0004).
if docker info >/dev/null 2>&1; then
    sem_docker_gradle=""; sem_docker_maven=""
else
    sem_docker_gradle="-PsemDocker"; sem_docker_maven="-DexcludedGroups=integracao"
    printf '\033[33mDocker indisponível: os testes de integração com PostgreSQL serão pulados.\033[0m\n'
fi

titulo "Contratos - schemas e casos de busca compartilhados"
if command -v python3 >/dev/null && python3 -c 'import jsonschema' 2>/dev/null; then
    python3 contratos/validar.py || falhas=$((falhas+1))
else
    pular "requer: pip install -r requirements-dev.txt"
fi

titulo "Documentação e processo (DIM0510)"
if command -v python3 >/dev/null; then
    python3 docs/verificar_adrs.py  || falhas=$((falhas+1))
    python3 docs/verificar_links.py || falhas=$((falhas+1))
    diff -r api-ktor/src/main/resources/db/migration api-quarkus/src/main/resources/db/migration \
        && echo "Migrações iguais nas duas APIs." || falhas=$((falhas+1))
else
    pular "requer python3"
fi

titulo "Kotlin - domínio, api-ktor e app (DIM0547 e DIM0524)"
if [ -x ./gradlew ]; then
    ./gradlew --quiet $sem_docker_gradle :shared:jvmTest :api-ktor:test :app:jvmTest || falhas=$((falhas+1))
elif command -v gradle >/dev/null; then
    gradle --quiet $sem_docker_gradle :shared:jvmTest :api-ktor:test :app:jvmTest || falhas=$((falhas+1))
else
    pular "requer Gradle. Rode: gradle wrapper --gradle-version 8.14.3"
fi

titulo "Java - api-quarkus (DIM0547)"
if [ -x api-quarkus/mvnw ]; then
    (cd api-quarkus && ./mvnw -B -ntp -q $sem_docker_maven test) || falhas=$((falhas+1))
elif command -v mvn >/dev/null; then
    (cd api-quarkus && mvn -B -ntp -q $sem_docker_maven test) || falhas=$((falhas+1))
else
    pular "requer Maven ou o wrapper em api-quarkus/mvnw"
fi

titulo "Services - domínio Go (DIM0547)"
if command -v go >/dev/null; then
    (cd services && go test ./...) || falhas=$((falhas+1))
else
    pular "requer Go 1.23"
fi

printf '\n'
if [ "$falhas" -gt 0 ]; then
    printf '\033[31m%d componente(s) com falha.\033[0m\n' "$falhas"; exit 1
fi
printf '\033[32mTudo certo.\033[0m\n'
