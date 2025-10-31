package fhtw.wien.domain;

/**
 * Enhanced enumeration for document processing status.
 * Provides better naming, descriptions, and utility methods.
 */
public enum DocumentStatus {
    /**
     * Document has been created but not yet processed
     */
    NEW("New", "Document created"),
    
    /**
     * Document has been uploaded and is ready for processing
     */
    UPLOADED("Uploaded", "Document uploaded successfully"),
    
    /**
     * Document is waiting in OCR processing queue
     */
    OCR_PENDING("OCR Pending", "Waiting for OCR processing"),
    
    /**
     * OCR processing is currently in progress
     */
    OCR_IN_PROGRESS("OCR In Progress", "OCR processing is running"),
    
    /**
     * OCR processing completed successfully
     */
    OCR_COMPLETED("OCR Completed", "OCR processing completed successfully"),
    
    /**
     * OCR processing failed
     */
    OCR_FAILED("OCR Failed", "OCR processing failed"),
    
    /**
     * Document has been indexed and is searchable
     */
    INDEXED("Indexed", "Document indexed and searchable");
    
    private final String displayName;
    private final String description;
    
    DocumentStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if the status represents a successful completion state.
     */
    public boolean isCompleted() {
        return this == OCR_COMPLETED || this == INDEXED;
    }
    
    /**
     * Checks if the status represents a processing state.
     */
    public boolean isProcessing() {
        return this == OCR_IN_PROGRESS || this == OCR_PENDING;
    }
    
    /**
     * Checks if the status represents a failure state.
     */
    public boolean isFailed() {
        return this == OCR_FAILED;
    }
    
    /**
     * Checks if OCR processing can be started for this status.
     */
    public boolean canStartOcr() {
        return this == NEW || this == UPLOADED;
    }
}
