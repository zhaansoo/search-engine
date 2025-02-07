package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    boolean existsByPath(String path);

    Optional<Page> findBySiteAndPath(Site site, String path);
    List<Page> findBySite(Site site);

}
