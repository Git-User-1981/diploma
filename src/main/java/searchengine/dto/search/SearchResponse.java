package searchengine.dto.search;

import lombok.Data;
import lombok.EqualsAndHashCode;
import searchengine.dto.BasicResponse;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SearchResponse extends BasicResponse {
    private Integer count = 0;
    private List<SearchData> data = List.of();
}
