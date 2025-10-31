package fhtw.wien.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder pattern for Document entity.
 * Provides a fluent API for constructing Document objects with validation.
 */
public class DocumentBuilder {
    
    private String title;
    private String originalFilename;
    private String contentType;
    private long sizeBytes;
    private String bucket;
    private String objectKey;
    private String storageUri;
    private String checksumSha256;
    private byte[] pdfData;
    private DocumentStatus status = DocumentStatus.NEW;
    private List<String> tags = new ArrayList<>();
    
    /**
     * Creates a new DocumentBuilder instance.
     */
    public static DocumentBuilder builder() {
        return new DocumentBuilder();
    }
    
    /**
     * Sets the document title.
     * @param title the document title (required)
     * @return this builder
     */
    public DocumentBuilder title(String title) {
        this.title = title;
        return this;
    }
    
    /**
     * Sets the original filename.
     * @param originalFilename the original filename (required)
     * @return this builder
     */
    public DocumentBuilder originalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
        return this;
    }
    
    /**
     * Sets the content type.
     * @param contentType the MIME content type (required)
     * @return this builder
     */
    public DocumentBuilder contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }
    
    /**
     * Sets the file size in bytes.
     * @param sizeBytes the file size (required, must be positive)
     * @return this builder
     */
    public DocumentBuilder sizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
        return this;
    }
    
    /**
     * Sets the storage bucket.
     * @param bucket the storage bucket name (required)
     * @return this builder
     */
    public DocumentBuilder bucket(String bucket) {
        this.bucket = bucket;
        return this;
    }
    
    /**
     * Sets the object key for storage.
     * @param objectKey the storage object key (required)
     * @return this builder
     */
    public DocumentBuilder objectKey(String objectKey) {
        this.objectKey = objectKey;
        return this;
    }
    
    /**
     * Sets the storage URI.
     * @param storageUri the storage URI (required)
     * @return this builder
     */
    public DocumentBuilder storageUri(String storageUri) {
        this.storageUri = storageUri;
        return this;
    }
    
    /**
     * Sets the SHA-256 checksum.
     * @param checksumSha256 the SHA-256 checksum (optional)
     * @return this builder
     */
    public DocumentBuilder checksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
        return this;
    }
    
    /**
     * Sets the PDF data.
     * @param pdfData the PDF data as byte array (optional)
     * @return this builder
     */
    public DocumentBuilder pdfData(byte[] pdfData) {
        this.pdfData = pdfData != null ? pdfData.clone() : null;
        return this;
    }
    
    /**
     * Sets the document status.
     * @param status the document status (optional, defaults to NEW)
     * @return this builder
     */
    public DocumentBuilder status(DocumentStatus status) {
        this.status = status;
        return this;
    }
    
    /**
     * Sets the document tags.
     * @param tags the list of tags (optional)
     * @return this builder
     */
    public DocumentBuilder tags(List<String> tags) {
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
        return this;
    }
    
    /**
     * Adds a single tag.
     * @param tag the tag to add
     * @return this builder
     */
    public DocumentBuilder addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty()) {
            if (this.tags == null) {
                this.tags = new ArrayList<>();
            }
            this.tags.add(tag.trim());
        }
        return this;
    }
    
    /**
     * Builds and validates the Document object.
     * @return the constructed Document
     * @throws IllegalArgumentException if required fields are missing or invalid
     */
    public Document build() {
        validateRequired();
        validateConstraints();
        
        Document document = new Document(
                title, originalFilename, contentType, sizeBytes,
                bucket, objectKey, storageUri, checksumSha256
        );
        
        if (pdfData != null) {
            document.setPdfData(pdfData.clone());
        }
        
        if (status != null) {
            document.setStatus(status);
        }
        
        if (tags != null && !tags.isEmpty()) {
            document.setTags(new ArrayList<>(tags));
        }
        
        return document;
    }
    
    /**
     * Validates that all required fields are present.
     */
    private void validateRequired() {
        if (isBlank(title)) {
            throw new IllegalArgumentException("Title is required");
        }
        if (isBlank(originalFilename)) {
            throw new IllegalArgumentException("Original filename is required");
        }
        if (isBlank(contentType)) {
            throw new IllegalArgumentException("Content type is required");
        }
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("Size bytes must be positive");
        }
        if (isBlank(bucket)) {
            throw new IllegalArgumentException("Bucket is required");
        }
        if (isBlank(objectKey)) {
            throw new IllegalArgumentException("Object key is required");
        }
        if (isBlank(storageUri)) {
            throw new IllegalArgumentException("Storage URI is required");
        }
    }
    
    /**
     * Validates field constraints and business rules.
     */
    private void validateConstraints() {
        if (title.length() > 255) {
            throw new IllegalArgumentException("Title cannot exceed 255 characters");
        }
        if (originalFilename.length() > 255) {
            throw new IllegalArgumentException("Original filename cannot exceed 255 characters");
        }
        if (contentType.length() > 127) {
            throw new IllegalArgumentException("Content type cannot exceed 127 characters");
        }
        if (bucket.length() > 63) {
            throw new IllegalArgumentException("Bucket name cannot exceed 63 characters");
        }
        if (checksumSha256 != null && checksumSha256.length() > 64) {
            throw new IllegalArgumentException("SHA-256 checksum cannot exceed 64 characters");
        }
        
        // Validate checksum format if provided
        if (checksumSha256 != null && !checksumSha256.matches("^[a-fA-F0-9]{64}$")) {
            throw new IllegalArgumentException("Invalid SHA-256 checksum format");
        }
    }
    
    /**
     * Checks if a string is null, empty, or contains only whitespace.
     */
    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}