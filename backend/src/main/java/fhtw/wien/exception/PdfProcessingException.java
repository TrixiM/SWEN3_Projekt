package fhtw.wien.exception;


public class PdfProcessingException extends BusinessLogicException {
    public PdfProcessingException(String message) {
        super(message);
    }

    public PdfProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
