package searchengine.services.search;

import searchengine.dto.search.SearchRequest;
import searchengine.dto.search.SearchResponse;

public interface SearchService {
    SearchResponse search(SearchRequest req);
}
