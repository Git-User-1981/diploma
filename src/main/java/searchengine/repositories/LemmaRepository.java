package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    Integer countAllBySite(Site site);
    @Modifying
    @Transactional
    @Query(
        value = "delete from lemma " +
                 "where frequency < 1 " +
                   "and site_id = (select site_id from page where id = ?)",
        nativeQuery = true
    )
    void clearFrequencyByPageId(Integer pageId);

    @Modifying
    @Transactional
    @Query(
        value = "update lemma l " +
                  "join `index` i on l.id = i.lemma_id " +
                   "set l.frequency = l.frequency - 1 " +
                 "where i.page_id = ?",
        nativeQuery = true
    )
    void decrementFrequencyByPageId(Integer pageId);

    default void decrementAndClearFrequency(Integer pageId) {
        decrementFrequencyByPageId(pageId);
        clearFrequencyByPageId(pageId);
    }

    @Query(
        value = "with pc as (" +
                    "select count(p.id) cnt " +
                      "from page p " +
                     "where p.code = 200 " +
                       "and p.site_id in (?2)" +
                ") " +
                "select l.lemma " +
                  "from lemma l, pc " +
                 "where l.lemma in (?1) " +
                   "and l.site_id in (?2) " +
                 "group by l.lemma " +
                "having sum(l.frequency) / max(pc.cnt) < ?3 " +
                 "order by sum(l.frequency) / pc.cnt",
        nativeQuery = true
    )
    List<String> filterLemmasOverLimit(List<String> lemmas, List<Integer> sitesUrl, Double percentOver);
}
