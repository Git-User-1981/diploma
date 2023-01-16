package searchengine.dto.statistics;

import lombok.Data;
import lombok.EqualsAndHashCode;
import searchengine.dto.BasicResponse;

@Data
@EqualsAndHashCode(callSuper = true)
public class StatisticsResponse extends BasicResponse {
    private StatisticsData statistics;
}
