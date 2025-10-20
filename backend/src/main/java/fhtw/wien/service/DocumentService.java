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

import java.util.List;
import java.util.UUID;

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

    public Document create(Document doc) {
        log.info("Creating document with title: {}", doc.getTitle());
        try {
            Document created = documentBusinessLogic.createOrUpdateDocument(doc);
            log.info("Document created with ID: {}", created.getId());
            
            // Publish message after document is created
            DocumentResponse response = DocumentMapper.toResponse(created);
            messageProducer.publishDocumentCreated(response);
            
            return created;
        } catch (Exception e) {
            log.error("Failed to create document: {}", doc.getTitle(), e);
            throw new ServiceException("Failed to create document", e);
        }
    }

    public Document update(Document doc) {
        log.info("Updating document with ID: {}", doc.getId());
        try {
            Document updated = documentBusinessLogic.createOrUpdateDocument(doc);
            log.info("Document updated with ID: {}", updated.getId());
            
            // Publish message after document is updated
            DocumentResponse response = DocumentMapper.toResponse(updated);
            
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
        log.info("Deleting document with ID: {}", id);
        try {
            documentBusinessLogic.deleteDocument(id);
            log.info("Document deleted with ID: {}", id);
            
            // Publish message after document is deleted
            messageProducer.publishDocumentDeleted(id);
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
}
