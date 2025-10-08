package fhtw.wien.exception;

/**
 * Exception for invalid request errors.
 * Thrown when request validation fails or invalid parameters are provided.
 */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }

    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
