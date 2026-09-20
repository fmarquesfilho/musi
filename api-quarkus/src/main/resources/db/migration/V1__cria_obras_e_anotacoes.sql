-- MUSI - esquema inicial (ADR-0004).
--
-- Este arquivo é IDÊNTICO em api-ktor/ e api-quarkus/: as duas APIs usam o mesmo banco.
-- O CI compara as duas pastas. Migração aplicada não se edita: cria-se a próxima.

-- Uma gravação. O id é texto porque as obras de exemplo usam `obra-01`; as novas recebem
-- um UUID gerado pela API.
CREATE TABLE obras (
    id              VARCHAR(64)  PRIMARY KEY,
    titulo          VARCHAR(300) NOT NULL CHECK (btrim(titulo) <> ''),
    artista         VARCHAR(300) NOT NULL CHECK (btrim(artista) <> ''),
    ano             INTEGER      NOT NULL CHECK (ano BETWEEN 1877 AND 2100),
    -- ADR-0003: a gravação no MusicBrainz. Duas obras com o mesmo mbid são duplicata.
    mbid            UUID         UNIQUE,
    -- A composição. Regravações compartilham, então NÃO é única.
    mbid_composicao UUID
);

-- Facetas da obra: (dimensão, valor), lista aberta (ADR-0002). Uma linha por faceta, e
-- não uma coluna por dimensão: dimensão nova é dado, não migração.
CREATE TABLE facetas (
    obra_id  VARCHAR(64) NOT NULL REFERENCES obras (id) ON DELETE CASCADE,
    posicao  INTEGER     NOT NULL,   -- preserva a ordem em que foram informadas
    dimensao VARCHAR(60) NOT NULL,
    valor    VARCHAR(60) NOT NULL,
    PRIMARY KEY (obra_id, dimensao, valor)
);

-- O filtro "tem a faceta" da listagem procura por (dimensão, valor).
CREATE INDEX facetas_dimensao_valor ON facetas (dimensao, valor);

-- Anotação: uma faceta atribuída por um curador, com data. Relação 1:N com obras.
CREATE TABLE anotacoes (
    id        BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    obra_id   VARCHAR(64)  NOT NULL REFERENCES obras (id) ON DELETE CASCADE,
    dimensao  VARCHAR(60)  NOT NULL,
    valor     VARCHAR(60)  NOT NULL,
    curador   VARCHAR(120) NOT NULL CHECK (btrim(curador) <> ''),
    criado_em TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- Curadores diferentes podem divergir; o mesmo curador não repete a mesma afirmação.
    CONSTRAINT anotacao_unica UNIQUE (obra_id, curador, dimensao, valor)
);

-- A chave estrangeira não cria índice sozinha no PostgreSQL; a rota aninhada consulta por ela.
CREATE INDEX anotacoes_obra ON anotacoes (obra_id);
