package fhtw.wien.business;

import fhtw.wien.domain.Document;
import fhtw.wien.exception.BusinessLogicException;
import fhtw.wien.exception.DataAccessException;
import fhtw.wien.exception.InvalidRequestException;
import fhtw.wien.exception.NotFoundException;
import fhtw.wien.repo.DocumentRepo;
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

    public DocumentBusinessLogic(DocumentRepo repository) {
        this.repository = repository;
    }

    @Transactional
    public Document createOrUpdateDocument(Document doc) {
        validateDocument(doc);
        log.debug("Saving document to repository: {}", doc.getId());
        try {
            Document saved = repository.save(doc);
            log.debug("Document saved successfully: {}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("Failed to save document to repository", e);
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
            // Optimized: Use existsById first, then deleteById directly
            if (!repository.existsById(id)) {
                log.warn("Cannot delete - document not found with ID: {}", id);
                throw new NotFoundException("Document not found: " + id);
            }
            repository.deleteById(id);
            log.info("Document deleted from repository: {}", id);
        } catch (NotFoundException e) {
            throw e; // Re-throw NotFoundException as-is
        } catch (Exception e) {
            log.error("Failed to delete document with ID: {}", id, e);
            throw new DataAccessException("Failed to delete document", e);
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
    }
}
