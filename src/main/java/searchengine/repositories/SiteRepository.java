package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {
    Optional<Site> findByUrl(String url);
    @Query(value = "select id from site where url in (?1)", nativeQuery = true)
    List<Integer> findAllIdByUrls(List<String> urls);
}
