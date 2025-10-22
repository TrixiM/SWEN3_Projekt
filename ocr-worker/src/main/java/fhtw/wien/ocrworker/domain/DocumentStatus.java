package fhtw.wien.ocrworker.domain;

/**
 * Document processing status for OCR worker.
 * This enum represents the various states a document can be in during OCR processing.
 */
public enum DocumentStatus {
    /**
     * Document is newly created and hasn't been processed yet
     */
    NEW,
    
    /**
     * Document is waiting in the processing queue
     */
    PENDING,
    
    /**
     * Document is currently being processed
     */
    PROCESSING,
    
    /**
     * Document processing completed successfully
     */
    COMPLETED,
    
    /**
     * Document processing failed
     */
    FAILED;
    
    /**
     * Checks if the status represents a completion state (successful or failed).
     * @return true if the document processing is finished
     */
    public boolean isFinished() {
        return this == COMPLETED || this == FAILED;
    }
    
    /**
     * Checks if the status represents an active processing state.
     * @return true if the document is currently being processed
     */
    public boolean isActive() {
        return this == PROCESSING || this == PENDING;
    }
}
