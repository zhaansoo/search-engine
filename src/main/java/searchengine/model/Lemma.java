package searchengine.model;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false, foreignKey = @ForeignKey(name = "site_id_fk"))
    private Site site;

    @Column(columnDefinition = "VARCHAR(255)")
    private String lemma;

    @Column(nullable = false)
    private Integer frequency;
}
