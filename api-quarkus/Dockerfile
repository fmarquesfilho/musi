# MUSI - api-quarkus  |  Java 25 + Quarkus
#
# Build em multiplos estagios: a imagem final nao contem Maven nem codigo-fonte.
# Dimensionado para os 512 MB da instancia gratuita do Render.

# --------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

# Copiar so o pom primeiro faz o Docker reaproveitar a camada de dependencias
# quando apenas o codigo muda.
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp package -DskipTests

# --------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Container que roda como root e superficie de ataque desnecessaria.
RUN addgroup -S musi && adduser -S musi -G musi
USER musi

# O layout `fast-jar` do Quarkus separa as dependencias do codigo da aplicacao,
# o que faz o Docker reaproveitar camadas entre builds.
COPY --from=build /build/target/quarkus-app/lib/      ./lib/
COPY --from=build /build/target/quarkus-app/*.jar     ./
COPY --from=build /build/target/quarkus-app/app/      ./app/
COPY --from=build /build/target/quarkus-app/quarkus/  ./quarkus/

# Mesmo teto do lado Kotlin, pelo mesmo motivo: 512 MB de RAM e 0.1 CPU.
ENV JAVA_TOOL_OPTIONS="-Xmx300m -Xms150m -XX:TieredStopAtLevel=1 -XX:+UseSerialGC"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]

# Sprint 3: imagem nativa com GraalVM inicia em milissegundos e usa uma fracao da
# memoria, ao custo de um build bem mais lento:
#   mvn package -Dnative -Dquarkus.native.container-build=true
