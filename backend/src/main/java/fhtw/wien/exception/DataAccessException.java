package fhtw.wien.exception;

/**
 * Exception for data access layer errors.
 * Thrown when database operations fail.
 */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
