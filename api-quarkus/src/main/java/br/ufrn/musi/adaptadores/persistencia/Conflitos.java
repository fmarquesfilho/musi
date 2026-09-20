package br.ufrn.musi.adaptadores.persistencia;

import br.ufrn.musi.aplicacao.ErroDeAplicacao.Conflito;
import org.hibernate.exception.ConstraintViolationException;

/**
 * Violação de `UNIQUE` vira `Conflito`, com o nome da restrição. A regra mora no banco, e
 * não num SELECT antes do INSERT: duas requisições simultâneas passariam as duas pela consulta.
 */
final class Conflitos {
    private Conflitos() {}

    static RuntimeException traduzir(ConstraintViolationException e) {
        String restricao = e.getConstraintName() == null ? "" : e.getConstraintName();
        return switch (restricao) {
            case "obras_mbid_key" -> new Conflito("já existe obra com este mbid (duplicata, ADR-0003)");
            case "anotacao_unica" -> new Conflito("este curador já anotou esta faceta nesta obra");
            default -> e;   // CHECK ou FK: não é conflito de estado, e não deveria passar da validação
        };
    }
}
