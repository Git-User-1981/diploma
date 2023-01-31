package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {
    @Query(
        value = "select i.* " +
                  "from `index` i " +
                  "join lemma l on l.id = i.lemma_id " +
                 "where l.lemma = ?1 " +
                   "and l.site_id in (?2)",
        nativeQuery = true
    )
    List<Index> findAllByLemmaAndSitesId(String lemmas, List<Integer> sitesId);

    @Query(
        value = "select i.* " +
                  "from `index` i " +
                  "join lemma l on l.id = i.lemma_id " +
                 "where l.lemma = ?1 " +
                   "and l.site_id in (?2) " +
                   "and i.page_id in (?3)",
        nativeQuery = true
    )
    List<Index> findAllByLemmaAndSitesIdAndPagesId(String lemmas, List<Integer> sitesId, List<Integer> pagesId);
}
