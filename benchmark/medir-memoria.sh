#!/usr/bin/env bash
#
# Benchmark inicial de footprint de memoria dos componentes do MUSI.
# Mede o RSS (Resident Set Size) de cada servico: em idle apos ficar pronto,
# e apos uma carga curta de requisicoes. Ver docs/BENCHMARK.md para a leitura.
#
# Uso:   ./benchmark/medir-memoria.sh
# Requer: java, go, mvn (ou ./mvnw), curl, lsof. Constroi os artefatos se faltarem.
#
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/benchmark"
GOBIN="$OUT/.servidor-go"

log() { printf '%s\n' "$*"; }

# --- construir o que faltar -------------------------------------------------
build() {
  if [ ! -x "$GOBIN" ]; then
    log ">> build Go"; (cd "$ROOT/services" && go build -o "$GOBIN" ./cmd/servidor) || exit 1
  fi
  if [ ! -x "$ROOT/api-ktor/build/install/api-ktor/bin/api-ktor" ]; then
    log ">> build Ktor (installDist)"; (cd "$ROOT" && ./gradlew -q :api-ktor:installDist) || exit 1
  fi
  if [ ! -f "$ROOT/api-quarkus/target/quarkus-app/quarkus-run.jar" ]; then
    log ">> build Quarkus (package)"
    local MVN; MVN=$(command -v mvn || echo "$ROOT/api-quarkus/mvnw")
    (cd "$ROOT" && "$MVN" -q -f api-quarkus/pom.xml -DskipTests package) || exit 1
  fi
}

# --- utilitarios de medicao -------------------------------------------------
rss_mb() { local kb; kb=$(ps -o rss= -p "$1" 2>/dev/null | tr -d ' '); [ -n "$kb" ] && awk "BEGIN{printf \"%.0f\",$kb/1024}" || echo "-"; }
rss_median() { local p=$1 a b c; a=$(rss_mb "$p"); sleep 0.7; b=$(rss_mb "$p"); sleep 0.7; c=$(rss_mb "$p"); printf '%s\n%s\n%s\n' "$a" "$b" "$c" | sort -n | sed -n '2p'; }
wait_health() { local url=$1 t0; t0=$(date +%s); for _ in $(seq 1 120); do curl -fs -o /dev/null "$url" && { echo $(( $(date +%s)-t0 )); return 0; }; sleep 0.5; done; echo TIMEOUT; return 1; }

run_case() { # nome porta health load_cmd -- start_cmd...
  local nome=$1 porta=$2 health=$3 load=$4; shift 4
  "$@" > "$OUT/.$nome.log" 2>&1 & local pid=$!
  local ready; ready=$(wait_health "$health") || { log "  $nome: TIMEOUT (ver benchmark/.$nome.log)"; kill "$pid" 2>/dev/null; return; }
  local lpid; lpid=$(lsof -nP -iTCP:"$porta" -sTCP:LISTEN -t 2>/dev/null | head -1); [ -z "$lpid" ] && lpid=$pid
  sleep 2; local idle; idle=$(rss_median "$lpid")
  eval "$load" >/dev/null 2>&1; sleep 1; local loaded; loaded=$(rss_median "$lpid")
  printf '  %-16s ready=%ss  idle=%sMB  pos-carga=%sMB\n' "$nome" "$ready" "$idle" "$loaded"
  kill "$lpid" "$pid" 2>/dev/null; pkill -P "$pid" 2>/dev/null; sleep 2
}

FILTRO='{"tipo":"tem","dimensao":"ritmo","valor":"baiao"}'
loadhttp() { for _ in $(seq 1 100); do curl -fs -o /dev/null "$1/busca?dimensao=ritmo&valor=baiao" 2>/dev/null; done; }
loadgo()   { for _ in $(seq 1 100); do curl -fs -o /dev/null -XPOST http://localhost:9090/buscar -H 'content-type: application/json' -d "$FILTRO"; done; }

# --- execucao ---------------------------------------------------------------
build
log "== $(java -version 2>&1 | head -1)"
log "== $(go version)"
log ""
log "CENARIO A - flags padrao"
run_case go              9090 http://localhost:9090/health   "loadgo"                       env PORT=9090 "$GOBIN"
run_case ktor-default    8080 http://localhost:8080/health   "loadhttp http://localhost:8080" env PORT=8080 "$ROOT/api-ktor/build/install/api-ktor/bin/api-ktor"
run_case quarkus-default 8080 http://localhost:8080/q/health "loadhttp http://localhost:8080" env PORT=8080 java -jar "$ROOT/api-quarkus/target/quarkus-app/quarkus-run.jar"
log ""
log "CENARIO B - JVM com -Xmx128m (contexto de contêiner apertado)"
run_case ktor-128m    8080 http://localhost:8080/health   "loadhttp http://localhost:8080" env PORT=8080 JAVA_OPTS="-Xmx128m -Xms32m" "$ROOT/api-ktor/build/install/api-ktor/bin/api-ktor"
run_case quarkus-128m 8080 http://localhost:8080/q/health "loadhttp http://localhost:8080" env PORT=8080 java -Xmx128m -Xms32m -jar "$ROOT/api-quarkus/target/quarkus-app/quarkus-run.jar"
log ""
log "Artefatos:"
log "  go binario : $(du -h "$GOBIN" | cut -f1)"
log "  ktor libs  : $(du -sh "$ROOT/api-ktor/build/install/api-ktor/lib" | cut -f1)"
log "  quarkus app: $(du -sh "$ROOT/api-quarkus/target/quarkus-app" | cut -f1)"
