# MUSI - api  |  Kotlin + Ktor
#
# Build em multiplos estagios: a imagem final nao contem Gradle, nem codigo-fonte,
# nem cache de dependencias. So a JRE e a distribuicao.
#
# Dimensionado para os 512 MB da instancia gratuita do Render.
#
# O contexto do build e a RAIZ do repositorio, e nao `api/`, porque o modulo
# :api depende do modulo :shared. Ver o `dockerContext` em render.yaml.

# --------------------------------------------------------------------------
FROM gradle:8.14-jdk25 AS build
WORKDIR /build

# Copiar so os arquivos de build primeiro faz o Docker reaproveitar a camada de
# dependencias quando apenas o codigo muda.
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY shared/build.gradle.kts ./shared/
COPY api/build.gradle.kts ./api/
COPY app/build.gradle.kts ./app/
RUN gradle --no-daemon :api:dependencies || true

COPY shared ./shared
COPY api ./api
RUN gradle --no-daemon :api:installDist -x test

# --------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Container que roda como root e superficie de ataque desnecessaria.
RUN addgroup -S musi && adduser -S musi -G musi
USER musi

COPY --from=build /build/api/build/install/api/ ./

# Ajuste para 512 MB e 0.1 CPU:
#   MaxRAMPercentage   - a JVM enxerga o limite do container
#   TieredStopAtLevel  - compilacao JIT mais barata
#   UseSerialGC        - menos memoria e menos threads que o G1 em 1 vCPU
# JAVA_TOOL_OPTIONS e lido pela JVM automaticamente, e o Render permite
# sobrescreve-lo pelo painel sem reconstruir a imagem.
#   -Xmx300m  teto de heap, deixando folga para o container dentro dos 512 MB
#   -Xms150m  heap inicial, evita crescer durante a inicializacao
ENV JAVA_TOOL_OPTIONS="-Xmx300m -Xms150m -XX:TieredStopAtLevel=1 -XX:+UseSerialGC"

EXPOSE 8080
ENTRYPOINT ["./bin/api"]
