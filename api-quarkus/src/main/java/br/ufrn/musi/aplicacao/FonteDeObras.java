package br.ufrn.musi.aplicacao;

import br.ufrn.musi.dominio.Dominio.Filtro;
import br.ufrn.musi.dominio.Dominio.Obra;
import java.util.List;

/**
 * Define o que a aplicação precisa, sem dizer quem fornece.
 *
 * Compare com `FonteDeObras.kt` do lado Ktor: lá a função é `suspend`; aqui é
 * bloqueante, que é a diferença no modelo de concorrência.
 */
public interface FonteDeObras {
    List<Obra> buscar(Filtro filtro);
}
