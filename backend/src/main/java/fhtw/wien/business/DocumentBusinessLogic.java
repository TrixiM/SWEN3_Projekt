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

import java.io.InputStream;
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

    /**
     * Creates or updates a document. For new documents, uploads PDF to MinIO storage.
     * Uses InputStream to stream data directly to MinIO without loading into memory.
     *
     * @param doc the document metadata
     * @param pdfStream the PDF content as InputStream (null for updates without file changes)
     * @return the saved document with storage metadata
     */
    @Transactional
    public Document createOrUpdateDocument(Document doc, InputStream pdfStream) {
        validateDocument(doc);
        
        try {
            // For new documents, upload to MinIO first
            if (doc.getId() == null && pdfStream != null) {
                
                // Generate ID for new document
                doc.setId(UUID.randomUUID());
                
                // Upload to MinIO and get object key (streaming directly without intermediate byte[])
                String objectKey = minioStorageService.uploadDocument(
                    doc.getId(), 
                    doc.getOriginalFilename(), 
                    doc.getContentType(), 
                    pdfStream,
                    doc.getSizeBytes()
                );
                
                // Update document with MinIO info
                doc.setBucket(minioStorageService.getBucketName());
                doc.setObjectKey(objectKey);
                doc.setStorageUri(String.format("minio://%s/%s", doc.getBucket(), objectKey));
            }
            
            Document saved = repository.save(doc);
            log.info("Document saved: id={}, objectKey={}", saved.getId(), saved.getObjectKey());
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
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Document not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Document> getAllDocuments() {
        return repository.findAll();
    }

    @Transactional
    public void deleteDocument(UUID id) {
        validateId(id);
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
                } catch (Exception minioException) {
                    log.warn("Failed to delete document from MinIO (continuing with DB delete): {}", 
                            document.getObjectKey(), minioException);
                }
            }
            
            // Delete from database
            repository.deleteById(id);
            
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
        
        return minioStorageService.downloadDocument(document.getObjectKey());
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
