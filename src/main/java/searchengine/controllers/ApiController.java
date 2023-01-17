package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.BasicResponse;
import searchengine.dto.search.SearchRequest;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.indexing.IndexingService;
import searchengine.services.search.SearchService;
import searchengine.services.statistics.StatisticsService;

import javax.validation.Valid;

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
    public ResponseEntity<SearchResponse> search(@Valid SearchRequest request) {
        return ResponseEntity.ok(searchService.search(request));
    }
}
