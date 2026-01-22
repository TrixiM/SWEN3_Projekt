package fhtw.wien.domain;


public enum DocumentStatus {

    NEW("New", "Document created"),
    

    UPLOADED("Uploaded", "Document uploaded successfully"),
    

    OCR_PENDING("OCR Pending", "Waiting for OCR processing"),
    

    OCR_IN_PROGRESS("OCR In Progress", "OCR processing is running"),
    

    OCR_COMPLETED("OCR Completed", "OCR processing completed successfully"),
    

    OCR_FAILED("OCR Failed", "OCR processing failed"),
    

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
    

    public boolean isCompleted() {
        return this == OCR_COMPLETED || this == INDEXED;
    }
    

    public boolean isProcessing() {
        return this == OCR_IN_PROGRESS || this == OCR_PENDING;
    }
    

    public boolean isFailed() {
        return this == OCR_FAILED;
    }
    

    public boolean canStartOcr() {
        return this == NEW || this == UPLOADED;
    }
}
