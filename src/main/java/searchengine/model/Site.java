package searchengine.model;

import lombok.Data;
import searchengine.shared.SiteStatus;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
public class Site {
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Id
    private Integer id;

    @Enumerated(EnumType.STRING)
    private SiteStatus status;

    @Column(name = "status_time")
    private LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String error;

    @Column(columnDefinition = "VARCHAR(255)")
    private String url;

    @Column(columnDefinition = "VARCHAR(255)")
    private String name;

//    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<Page> pages;
//
//    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<Lemma> lemmas;


}
