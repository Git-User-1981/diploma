package searchengine.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.ErrorResponse;

@ControllerAdvice
public class DefaultAdvice {
    @ExceptionHandler(ApiCallException.class)
    public ResponseEntity<ErrorResponse> handleException(ApiCallException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }
}
