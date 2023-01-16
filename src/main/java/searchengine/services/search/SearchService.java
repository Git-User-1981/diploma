package searchengine.services.search;

import searchengine.dto.search.SearchResponse;

public interface SearchService {
    SearchResponse search(String query, String siteUrl, String inputOffset, String inputLimit);
}
