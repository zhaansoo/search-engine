package searchengine.model;

import lombok.Data;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    private Integer code;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String path;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Index> indices;
}
