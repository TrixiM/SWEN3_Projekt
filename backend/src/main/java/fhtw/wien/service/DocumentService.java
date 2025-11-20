package fhtw.wien.service;

import fhtw.wien.business.DocumentBusinessLogic;
import fhtw.wien.business.PdfRenderingBusinessLogic;
import fhtw.wien.domain.Document;
import fhtw.wien.dto.DocumentResponse;
import fhtw.wien.exception.ServiceException;
import fhtw.wien.messaging.DocumentMessageProducer;
import fhtw.wien.util.DocumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for document operations.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Delegating business operations to DocumentBusinessLogic</li>
 *   <li>Publishing RabbitMQ messages after successful operations</li>
 *   <li>Exception translation and logging</li>
 * </ul>
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentBusinessLogic documentBusinessLogic;
    private final PdfRenderingBusinessLogic pdfRenderingBusinessLogic;
    private final DocumentMessageProducer messageProducer;

    public DocumentService(DocumentBusinessLogic documentBusinessLogic,
                          PdfRenderingBusinessLogic pdfRenderingBusinessLogic,
                          DocumentMessageProducer messageProducer) {
        this.documentBusinessLogic = documentBusinessLogic;
        this.pdfRenderingBusinessLogic = pdfRenderingBusinessLogic;
        this.messageProducer = messageProducer;
    }

    /**
     * Creates a new document by uploading PDF to MinIO and saving metadata to database.
     * Uses InputStream to avoid loading entire PDF into memory.
     */
    public Document create(Document doc, InputStream pdfStream) {
        try {
            Document created = documentBusinessLogic.createOrUpdateDocument(doc, pdfStream);
            
            // Publish message after document is created
            DocumentResponse response = DocumentMapper.toResponse(created);
            messageProducer.publishDocumentCreated(response);
            log.info("Document created and message published: id={}, title={}", created.getId(), created.getTitle());
            
            return created;
        } catch (Exception e) {
            log.error("Failed to create document: {}", doc.getTitle(), e);
            throw new ServiceException("Failed to create document", e);
        }
    }

    public Document update(Document doc) {
        try {
            Document updated = documentBusinessLogic.createOrUpdateDocument(doc, null);
            
            // Publish message after document is updated
            DocumentResponse response = DocumentMapper.toResponse(updated);
            log.info("Document updated: id={}, title={}", updated.getId(), updated.getTitle());
            
            return updated;
        } catch (Exception e) {
            log.error("Failed to update document with ID: {}", doc.getId(), e);
            throw new ServiceException("Failed to update document", e);
        }
    }

    public Document get(UUID id) {
        log.debug("Retrieving document with ID: {}", id);
        try {
            return documentBusinessLogic.getDocumentById(id);
        } catch (Exception e) {
            log.error("Failed to retrieve document with ID: {}", id, e);
            throw e; // Re-throw to preserve original exception type (e.g., NotFoundException)
        }
    }

    public List<Document> getAll() {
        log.debug("Retrieving all documents");
        try {
            List<Document> documents = documentBusinessLogic.getAllDocuments();
            log.debug("Retrieved {} documents", documents.size());
            return documents;
        } catch (Exception e) {
            log.error("Failed to retrieve documents", e);
            throw new ServiceException("Failed to retrieve documents", e);
        }
    }

    public void delete(UUID id) {
        try {
            documentBusinessLogic.deleteDocument(id);
            
            // Publish message after document is deleted
            messageProducer.publishDocumentDeleted(id);
            log.info("Document deleted and message published: id={}", id);
        } catch (Exception e) {
            log.error("Failed to delete document with ID: {}", id, e);
            throw new ServiceException("Failed to delete document", e);
        }
    }


    public byte[] renderPdfPage(UUID id, int pageNumber, float scale) {
        log.info("Rendering page {} of document {} with scale {}", pageNumber, id, scale);
        try {
            var doc = documentBusinessLogic.getDocumentById(id);
            byte[] renderedPage = pdfRenderingBusinessLogic.renderPdfPage(doc, pageNumber, scale);
            log.debug("Successfully rendered page {} for document {}", pageNumber, id);
            return renderedPage;
        } catch (Exception e) {
            log.error("Failed to render page {} of document {}", pageNumber, id, e);
            throw e; // Re-throw to preserve original exception type
        }
    }

    public int getPdfPageCount(UUID id) {
        log.debug("Getting page count for document {}", id);
        try {
            var doc = documentBusinessLogic.getDocumentById(id);
            int pageCount = pdfRenderingBusinessLogic.getPdfPageCount(doc);
            log.debug("Document {} has {} pages", id, pageCount);
            return pageCount;
        } catch (Exception e) {
            log.error("Failed to get page count for document {}", id, e);
            throw e; // Re-throw to preserve original exception type
        }
    }

    public byte[] getDocumentContent(Document document) {
        log.debug("Retrieving content for document {}", document.getId());
        try {
            byte[] content = documentBusinessLogic.getDocumentContent(document);
            log.debug("Retrieved content for document {}, size: {} bytes", 
                    document.getId(), content.length);
            return content;
        } catch (Exception e) {
            log.error("Failed to retrieve content for document {}", document.getId(), e);
            throw new ServiceException("Failed to retrieve document content", e);
        }
    }
}
