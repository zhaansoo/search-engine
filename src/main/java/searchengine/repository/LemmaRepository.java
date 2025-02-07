package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    Optional<Lemma> findByLemmaAndSite(String lemma, Site site);
    List<Lemma> findByLemmaInAndSite(List<String> lemmas, Site site);
    List<Lemma> findBySite(Site site);

    List<Lemma> findBySiteInAndLemmaInAndFrequencyLessThanOrderByFrequencyAsc(List<Site> sites, List<String> lemmas, int frequency);

    @Query("SELECT MAX(i.frequency) FROM Lemma i WHERE i.site IN :sites")
    Integer findMaxFrequencyBySiteIn(@Param("sites") List<Site> sites);

}
