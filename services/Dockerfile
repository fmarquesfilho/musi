# MUSI - services/busca  |  Go
#
# Imagem final baseada em `scratch`: nao contem shell, gerenciador de pacotes
# nem biblioteca C. So o binario estatico. Alguns megabytes, e superficie de
# ataque minima.

# --------------------------------------------------------------------------
FROM golang:1.27-alpine AS build
WORKDIR /build

# Sem dependencia externa na Sprint 0, entao nao ha go.sum. Quando houver,
# esta camada passa a ser reaproveitada entre builds.
COPY go.mod ./
RUN go mod download

COPY . .

# CGO_ENABLED=0 produz binario estatico, requisito para rodar em `scratch`.
# -trimpath remove caminhos absolutos; -ldflags "-s -w" tira simbolos de debug.
RUN CGO_ENABLED=0 GOOS=linux go build \
      -trimpath -ldflags="-s -w" \
      -o /servidor ./cmd/servidor

# --------------------------------------------------------------------------
FROM scratch

# Certificados raiz, caso o servico venha a fazer chamadas HTTPS.
COPY --from=build /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/
COPY --from=build /servidor /servidor

# Numerico porque `scratch` nao tem /etc/passwd para resolver nomes.
USER 65534:65534

EXPOSE 9090
ENTRYPOINT ["/servidor"]
