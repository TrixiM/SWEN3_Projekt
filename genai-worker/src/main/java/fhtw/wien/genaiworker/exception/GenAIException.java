package fhtw.wien.genaiworker.exception;

/**
 * Exception thrown when GenAI API operations fail.
 */
public class GenAIException extends RuntimeException {
    
    public GenAIException(String message) {
        super(message);
    }
    
    public GenAIException(String message, Throwable cause) {
        super(message, cause);
    }
}
