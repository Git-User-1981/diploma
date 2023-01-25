package searchengine.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.config.Config;
import searchengine.dto.ErrorResponse;

import java.util.Map;

@ControllerAdvice
public class DefaultAdvice {
    private final Map<String, String> messages;

    public DefaultAdvice(Config config) {
        this.messages = config.getMessages();
    }

    @ExceptionHandler(ApiCallException.class)
    public ResponseEntity<ErrorResponse> handleException(ApiCallException e) {
        return ResponseEntity.ok().body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleException(RuntimeException e) {
        return ResponseEntity.ok().body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleException(BindException e) {
        FieldError fieldError = e.getFieldError();
        String errorMessage;

        if (fieldError == null) {
            errorMessage = messages.get("search_unexpected_error");
        }
        else {
            String errMsgKey = fieldError.getDefaultMessage();
            if (errMsgKey == null) {
                errMsgKey = "search_message_key_not_set";
            }
            else if (errMsgKey.contains("NumberFormatException")) {
                errMsgKey = "search_number_positive";
            }

            if (!messages.containsKey(errMsgKey)) {
                errMsgKey = "search_unknown_message_key";
            }

            errorMessage = String.format(messages.get(errMsgKey), fieldError.getField());
        }

        return ResponseEntity.ok().body(new ErrorResponse(errorMessage));
    }
}
