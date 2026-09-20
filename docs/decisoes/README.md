# Decisões de arquitetura

Uma ADR registra uma decisão e o contexto em que ela foi tomada. Não descreve o que o
sistema faz, mas por que funciona assim e o que foi descartado.

| # | Título | Estado |
|---|---|---|
| [0001](0001-stacks-e-estrutura.md) | Monorepo, três linguagens, Java 25 por toolchain | Aceita |
| [0002](0002-modelo-de-dominio.md) | Faceta extensível, filtro como árvore, sem ranking | Aceita |
| [0003](0003-conciliacao-musicbrainz.md) | Conciliação com o MusicBrainz, fora do ciclo de leitura | Aceita |
| [0004](0004-persistencia-postgresql-flyway.md) | PostgreSQL com Flyway, o mesmo esquema nas duas APIs | Aceita |

## Modelo

Copie [`0000-modelo.md`](0000-modelo.md). Numere em sequência.

Uma ADR aceita não é editada. Quando a decisão muda, escreve-se outra que a substitua e
marca-se a anterior. O artefato é o histórico de decisões, não o estado atual.

## Estados

| Estado | Significa |
|---|---|
| Proposta | Escrita, em discussão |
| Aceita | Está em vigor; código que a contraria é defeito |
| Substituída | Não vale mais; aponta para quem a substituiu |
| Recusada | Considerada e descartada; permanece no repositório |

Uma ADR recusada é tão útil quanto uma aceita: preserva o raciocínio e evita que a mesma
discussão precise ser refeita meses depois.
