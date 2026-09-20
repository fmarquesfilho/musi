package br.ufrn.musi.aplicacao;

import java.util.List;

/**
 * Os erros que a aplicação sabe nomear. Nenhum fala de HTTP: quem decide que
 * `NaoEncontrado` vira `404` é a borda (ErrosMapper, em adaptadores/web). Espelha
 * aplicacao/Erros.kt do lado Kotlin; `sealed` lista os casos, como lá.
 */
public abstract sealed class ErroDeAplicacao extends RuntimeException
        permits ErroDeAplicacao.EntradaInvalida, ErroDeAplicacao.NaoEncontrado,
                ErroDeAplicacao.Conflito, ErroDeAplicacao.PersistenciaIndisponivel {

    ErroDeAplicacao(String mensagem) {
        super(mensagem);
    }

    /** A entrada viola uma regra de forma. Traz todas as violações, não só a primeira. */
    public static final class EntradaInvalida extends ErroDeAplicacao {
        private final List<String> violacoes;

        public EntradaInvalida(List<String> violacoes) {
            super(String.join("; ", violacoes));
            this.violacoes = List.copyOf(violacoes);
        }

        public List<String> violacoes() {
            return violacoes;
        }
    }

    public static final class NaoEncontrado extends ErroDeAplicacao {
        public NaoEncontrado(String recurso, Object id) {
            super(recurso + " `" + id + "` não existe");
        }
    }

    /** A operação contraria o estado atual: MBID já usado, anotação repetida. */
    public static final class Conflito extends ErroDeAplicacao {
        public Conflito(String mensagem) {
            super(mensagem);
        }
    }

    /** Não há banco configurado nesta instância (o deploy sem banco, até a Sprint 3 — ADR-0004). */
    public static final class PersistenciaIndisponivel extends ErroDeAplicacao {
        public PersistenciaIndisponivel() {
            super("persistência não configurada nesta instância");
        }
    }
}
