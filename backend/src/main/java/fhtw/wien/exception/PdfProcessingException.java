package fhtw.wien.exception;

/**
 * Exception for PDF processing errors.
 * Thrown when PDF rendering or manipulation operations fail.
 */
public class PdfProcessingException extends BusinessLogicException {
    public PdfProcessingException(String message) {
        super(message);
    }

    public PdfProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
