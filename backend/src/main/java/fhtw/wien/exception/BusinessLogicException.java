package fhtw.wien.exception;

/**
 * Exception for business logic layer errors.
 * Thrown when business rules are violated or business operations fail.
 */
public class BusinessLogicException extends RuntimeException {
    public BusinessLogicException(String message) {
        super(message);
    }

    public BusinessLogicException(String message, Throwable cause) {
        super(message, cause);
    }
}
