package searchengine.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "index_table")
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    @ManyToOne
    @JoinColumn(name = "lemma_id", nullable = false, foreignKey = @ForeignKey(name = "lemma_id_fk"))
    private Lemma lemma;

    @Column(nullable = false, name = "`rank`")
    private Double rank;
}
