package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.BasicResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.indexing.IndexingService;
import searchengine.services.search.SearchService;
import searchengine.services.statistics.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    public ApiController(
            StatisticsService statisticsService,
            IndexingService indexingService,
            SearchService searchService
    ) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<BasicResponse> startIndexingAll() {
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<BasicResponse> startIndexingPage(
            @RequestParam(name = "url", required = false, defaultValue = "") String url
    ) {
        return ResponseEntity.ok(indexingService.startIndexingPage(url));
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<BasicResponse> stopIndexingAll() {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @RequestParam(name = "query", required = false, defaultValue = "") String query,
            @RequestParam(name = "site", required = false, defaultValue = "") String siteUrl,
            @RequestParam(name = "offset", required = false, defaultValue = "0") String inputOffset,
            @RequestParam(name = "limit", required = false, defaultValue = "20") String inputLimit
    ) {
        return ResponseEntity.ok(searchService.search(query, siteUrl, inputOffset, inputLimit));
    }
}
