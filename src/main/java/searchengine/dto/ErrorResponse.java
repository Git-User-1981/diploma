package searchengine.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ErrorResponse extends BasicResponse {
    private String error;

    public ErrorResponse(String error) {
        this.setResult(false);
        this.error = error;
    }
}
