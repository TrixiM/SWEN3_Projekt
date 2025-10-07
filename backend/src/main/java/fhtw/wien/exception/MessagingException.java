package fhtw.wien.exception;

/**
 * Exception for messaging layer errors.
 * Thrown when message publishing or consuming operations fail.
 */
public class MessagingException extends RuntimeException {
    public MessagingException(String message) {
        super(message);
    }

    public MessagingException(String message, Throwable cause) {
        super(message, cause);
    }
}
