package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;
import searchengine.shared.SiteStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {
    List<Site> findByNameIn(List<String> names);
    Optional<Site> findByName(String name);
    List<Site> findByNameInAndStatus(List<String> names, SiteStatus status);

    boolean existsByStatus(SiteStatus status);
    boolean existsByStatusIn(List<SiteStatus> status);
    boolean existsByStatusAndName(SiteStatus status, String name);
    boolean existsByStatusAndNameIn(SiteStatus status, List<String> name);
    boolean existsByUrl(String url);

}
