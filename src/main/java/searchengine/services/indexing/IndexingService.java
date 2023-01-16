package searchengine.services.indexing;

import searchengine.dto.BasicResponse;

public interface IndexingService {
    BasicResponse startIndexing();
    BasicResponse startIndexingPage(String url);
    BasicResponse stopIndexing();
}
