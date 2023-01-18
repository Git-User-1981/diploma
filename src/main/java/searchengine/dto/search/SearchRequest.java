package searchengine.dto.search;

import lombok.Data;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Data
public class SearchRequest {
    @NotBlank(message = "search_query_empty")
    private String query = "";

    @URL(message = "search_site_bad_url")
    private String site = "";

    @Min(value = 0, message = "search_number_positive")
    private Integer offset = 0;

    @Min(value = 0, message = "search_number_positive")
    private Integer limit = 10;
}
