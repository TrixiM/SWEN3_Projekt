package fhtw.wien.exception;

/**
 * Exception for service layer errors.
 * Thrown when service orchestration operations fail.
 */
public class ServiceException extends RuntimeException {
    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
