package searchengine.exceptions;

public class ApiCallException extends RuntimeException {
    public ApiCallException(String errorMessage) {
        super(errorMessage);
    }
}
