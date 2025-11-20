package fhtw.wien.business;

import fhtw.wien.domain.Document;
import fhtw.wien.exception.BusinessLogicException;
import fhtw.wien.exception.DataAccessException;
import fhtw.wien.exception.InvalidRequestException;
import fhtw.wien.exception.NotFoundException;
import fhtw.wien.repo.DocumentRepo;
import fhtw.wien.service.MinIOStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class DocumentBusinessLogic {

    private static final Logger log = LoggerFactory.getLogger(DocumentBusinessLogic.class);

    private final DocumentRepo repository;
    private final MinIOStorageService minioStorageService;

    public DocumentBusinessLogic(DocumentRepo repository, MinIOStorageService minioStorageService) {
        this.repository = repository;
        this.minioStorageService = minioStorageService;
    }

    @Transactional
    public Document createOrUpdateDocument(Document doc, byte[] pdfData) {
        validateDocument(doc);
        
        try {
            // For new documents, upload to MinIO first
            if (doc.getId() == null && pdfData != null && pdfData.length > 0) {
                log.debug("Uploading new document to MinIO storage");
                
                // Generate ID for new document
                doc.setId(UUID.randomUUID());
                
                // Upload to MinIO and get object key
                String objectKey = minioStorageService.uploadDocument(
                    doc.getId(), 
                    doc.getOriginalFilename(), 
                    doc.getContentType(), 
                    pdfData
                );
                
                // Update document with MinIO info
                doc.setBucket(minioStorageService.getBucketName());
                doc.setObjectKey(objectKey);
                doc.setStorageUri(String.format("minio://%s/%s", doc.getBucket(), objectKey));
                
                log.debug("Document uploaded to MinIO: bucket={}, key={}", doc.getBucket(), objectKey);
            }
            
            log.debug("Saving document metadata to repository: {}", doc.getId());
            Document saved = repository.save(doc);
            log.debug("Document saved successfully: {}", saved.getId());
            return saved;
            
        } catch (Exception e) {
            log.error("Failed to save document", e);
            // If MinIO upload succeeded but DB save failed, clean up MinIO
            if (doc.getObjectKey() != null) {
                try {
                    minioStorageService.deleteDocument(doc.getObjectKey());
                    log.debug("Cleaned up MinIO object after DB failure: {}", doc.getObjectKey());
                } catch (Exception cleanupException) {
                    log.warn("Failed to cleanup MinIO object after DB failure: {}", doc.getObjectKey(), cleanupException);
                }
            }
            throw new DataAccessException("Failed to save document", e);
        }
    }

    @Transactional(readOnly = true)
    public Document getDocumentById(UUID id) {
        validateId(id);
        log.debug("Fetching document by ID: {}", id);
        try {
            return repository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("Document not found with ID: {}", id);
                        return new NotFoundException("Document not found: " + id);
                    });
        } catch (NotFoundException e) {
            throw e; // Re-throw NotFoundException as-is
        } catch (Exception e) {
            log.error("Error fetching document with ID: {}", id, e);
            throw new DataAccessException("Failed to fetch document", e);
        }
    }

    @Transactional(readOnly = true)
    public List<Document> getAllDocuments() {
        log.debug("Fetching all documents from repository");
        try {
            List<Document> documents = repository.findAll();
            log.debug("Fetched {} documents from repository", documents.size());
            return documents;
        } catch (Exception e) {
            log.error("Failed to fetch all documents", e);
            throw new DataAccessException("Failed to fetch documents", e);
        }
    }

    @Transactional
    public void deleteDocument(UUID id) {
        validateId(id);
        log.info("Deleting document with ID: {}", id);
        try {
            // Get document first to obtain MinIO object key
            Document document = repository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("Cannot delete - document not found with ID: {}", id);
                        return new NotFoundException("Document not found: " + id);
                    });
            
            // Delete from MinIO first
            if (document.getObjectKey() != null) {
                try {
                    minioStorageService.deleteDocument(document.getObjectKey());
                    log.debug("Document deleted from MinIO: {}", document.getObjectKey());
                } catch (Exception minioException) {
                    log.warn("Failed to delete document from MinIO (continuing with DB delete): {}", 
                            document.getObjectKey(), minioException);
                }
            }
            
            // Delete from database
            repository.deleteById(id);
            log.info("Document deleted from repository: {}", id);
            
        } catch (NotFoundException e) {
            throw e; // Re-throw NotFoundException as-is
        } catch (Exception e) {
            log.error("Failed to delete document with ID: {}", id, e);
            throw new DataAccessException("Failed to delete document", e);
        }
    }
    
    /**
     * Retrieves document content (PDF data) from MinIO storage.
     *
     * @param document the document entity containing MinIO reference
     * @return the PDF data as byte array
     * @throws DataAccessException if document content cannot be retrieved
     */
    @Transactional(readOnly = true)
    public byte[] getDocumentContent(Document document) {
        if (document.getObjectKey() == null) {
            log.error("Document has no MinIO object key: {}", document.getId());
            throw new DataAccessException("Document content not available - missing storage reference");
        }
        
        try {
            log.debug("Retrieving document content from MinIO: {}", document.getObjectKey());
            byte[] content = minioStorageService.downloadDocument(document.getObjectKey());
            log.debug("Retrieved document content: {} bytes", content.length);
            return content;
        } catch (Exception e) {
            log.error("Failed to retrieve document content from MinIO: {}", document.getObjectKey(), e);
            throw new DataAccessException("Failed to retrieve document content", e);
        }
    }
    
    /**
     * Validates document input.
     */
    private void validateDocument(Document doc) {
        if (doc == null) {
            throw new InvalidRequestException("Document cannot be null");
        }
        
        if (isBlank(doc.getTitle())) {
            throw new InvalidRequestException("Document title is required");
        }
        
        if (isBlank(doc.getOriginalFilename())) {
            throw new InvalidRequestException("Original filename is required");
        }
        
        if (isBlank(doc.getContentType())) {
            throw new InvalidRequestException("Content type is required");
        }
        
        if (doc.getSizeBytes() <= 0) {
            throw new InvalidRequestException("File size must be positive");
        }
        
        // Validate file size limits (e.g., max 100MB)
        if (doc.getSizeBytes() > 100 * 1024 * 1024) {
            throw new InvalidRequestException("File size cannot exceed 100MB");
        }
    }
    
    /**
     * Validates UUID input.
     */
    private void validateId(UUID id) {
        if (id == null) {
            throw new InvalidRequestException("Document ID cannot be null");
        }
    }
    
    /**
     * Checks if a string is null, empty, or contains only whitespace.
     */
    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
