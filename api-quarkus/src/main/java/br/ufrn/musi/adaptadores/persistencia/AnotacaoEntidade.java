package br.ufrn.musi.adaptadores.persistencia;

import br.ufrn.musi.dominio.Dominio.Anotacao;
import br.ufrn.musi.dominio.Dominio.Faceta;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * Mapeamento da tabela `anotacoes`. O relacionamento N:1 com a obra é `@ManyToOne` sobre a
 * chave estrangeira `obra_id`, preguiçoso: ler `obra.id` não carrega a obra.
 */
@Entity
@Table(name = "anotacoes")
public class AnotacaoEntidade extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "obra_id")
    public ObraEntidade obra;

    public String dimensao;
    public String valor;
    public String curador;

    // A data é do banco (DEFAULT now()); o Hibernate a lê de volta depois do INSERT.
    @Generated(event = EventType.INSERT)
    @Column(name = "criado_em", insertable = false, updatable = false)
    public Instant criadoEm;

    Anotacao paraAnotacao() {
        return new Anotacao(id, obra.id, new Faceta(dimensao, valor), curador, criadoEm);
    }
}
