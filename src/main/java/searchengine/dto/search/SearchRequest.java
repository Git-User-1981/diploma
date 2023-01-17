package searchengine.dto.search;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class SearchRequest {
    @NotNull(message = "search_query_empty")
    @NotBlank(message = "search_query_empty")
    private String query;

    private String site = "";

    @Min(value = 0, message = "search_number_positive")
    private Integer offset = 0;

    @Min(value = 0, message = "search_number_positive")
    private Integer limit = 10;
}
