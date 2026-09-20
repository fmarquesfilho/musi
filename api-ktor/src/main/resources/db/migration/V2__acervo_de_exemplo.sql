-- MUSI - acervo de exemplo, o mesmo de contratos/exemplos/acervo.json (CC0, inventado
-- para o curso; ver ADR-0002). O serviço Go tem as mesmas obras em memória, então a busca
-- e o CRUD começam iguais; as obras criadas depois só existem aqui (ADR-0004). Nenhuma anotação: as facetas de exemplo não são de curador.
--
-- IDÊNTICO em api-ktor/ e api-quarkus/.

INSERT INTO obras (id, titulo, artista, ano, mbid, mbid_composicao) VALUES
    ('obra-01', 'Ponteio', 'Edu Lobo', 1967, '11111111-1111-4111-8111-111111111111', '22222222-2222-4222-8222-222222222222'),
    ('obra-02', 'Beira Mar', 'Gilberto Gil', 1969, NULL, NULL),
    ('obra-03', 'Asa Branca', 'Luiz Gonzaga', 1947, NULL, NULL),
    ('obra-04', 'Rio Grande', 'Chico Science & Nacao Zumbi', 1994, NULL, NULL),
    ('obra-05', 'Refazenda', 'Gilberto Gil', 1975, NULL, NULL);

INSERT INTO facetas (obra_id, posicao, dimensao, valor) VALUES
    ('obra-01', 0, 'genero', 'mpb'),
    ('obra-01', 1, 'ritmo', 'ponteio'),
    ('obra-01', 2, 'movimento', 'festivais-da-cancao'),
    ('obra-02', 0, 'genero', 'mpb'),
    ('obra-02', 1, 'ritmo', 'ijexa'),
    ('obra-02', 2, 'movimento', 'tropicalia'),
    ('obra-03', 0, 'genero', 'forro'),
    ('obra-03', 1, 'ritmo', 'baiao'),
    ('obra-03', 2, 'instrumentacao', 'sanfona'),
    ('obra-04', 0, 'ritmo', 'maracatu'),
    ('obra-04', 1, 'movimento', 'manguebeat'),
    ('obra-04', 2, 'regiao', 'recife'),
    ('obra-05', 0, 'genero', 'mpb'),
    ('obra-05', 1, 'ritmo', 'baiao');
