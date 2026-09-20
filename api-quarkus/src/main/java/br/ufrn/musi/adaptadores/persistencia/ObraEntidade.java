package br.ufrn.musi.adaptadores.persistencia;

import br.ufrn.musi.dominio.Dominio.Faceta;
import br.ufrn.musi.dominio.Dominio.Obra;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;

/**
 * Mapeamento da tabela `obras` (criada pela migração V1) para o Hibernate. O esquema é
 * da migração; `schema-management.strategy=none` impede o Hibernate de mexer nele.
 *
 * A entidade é do adaptador, não do domínio: tem campos públicos e anotações JPA, e é
 * convertida em `Obra` (record, imutável) antes de sair daqui.
 */
@Entity
@Table(name = "obras")
public class ObraEntidade extends PanacheEntityBase {

    @Id
    public String id;

    public String titulo;
    public String artista;
    public int ano;
    public UUID mbid;

    @Column(name = "mbid_composicao")
    public UUID mbidComposicao;

    // Facetas são valores, não entidades: não têm id próprio nem vida fora da obra.
    // @OrderColumn preserva a ordem (coluna `posicao`). @BatchSize carrega as facetas de
    // uma página inteira numa consulta só, em vez de uma por obra (N+1).
    @ElementCollection
    @CollectionTable(name = "facetas", joinColumns = @JoinColumn(name = "obra_id"))
    @OrderColumn(name = "posicao")
    @BatchSize(size = 100)
    public List<FacetaEmbutida> facetas = new ArrayList<>();

    @Embeddable
    public static class FacetaEmbutida {
        public String dimensao;
        public String valor;

        public FacetaEmbutida() {}

        FacetaEmbutida(Faceta f) {
            this.dimensao = f.dimensao();
            this.valor = f.valor();
        }
    }

    Obra paraObra() {
        return new Obra(id, titulo, artista, ano,
            facetas.stream().map(f -> new Faceta(f.dimensao, f.valor)).toList(),
            mbid == null ? null : mbid.toString(),
            mbidComposicao == null ? null : mbidComposicao.toString());
    }
}
