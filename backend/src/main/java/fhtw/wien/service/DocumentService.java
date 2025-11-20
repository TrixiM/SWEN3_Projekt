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
            log.debug("Document updated: id={}", updated.getId());
            
            return updated;
        } catch (Exception e) {
            log.error("Failed to update document with ID: {}", doc.getId(), e);
            throw new ServiceException("Failed to update document", e);
        }
    }

    public Document get(UUID id) {
        return documentBusinessLogic.getDocumentById(id);
    }

    public List<Document> getAll() {
        return documentBusinessLogic.getAllDocuments();
    }

    public void delete(UUID id) {
        try {
            documentBusinessLogic.deleteDocument(id);
            
            // Publish message after document is deleted
            messageProducer.publishDocumentDeleted(id);
            log.debug("Document deleted, message published: id={}", id);
        } catch (Exception e) {
            log.error("Failed to delete document with ID: {}", id, e);
            throw new ServiceException("Failed to delete document", e);
        }
    }


    public byte[] renderPdfPage(UUID id, int pageNumber, float scale) {
        var doc = documentBusinessLogic.getDocumentById(id);
        return pdfRenderingBusinessLogic.renderPdfPage(doc, pageNumber, scale);
    }

    public int getPdfPageCount(UUID id) {
        var doc = documentBusinessLogic.getDocumentById(id);
        return pdfRenderingBusinessLogic.getPdfPageCount(doc);
    }

    public byte[] getDocumentContent(Document document) {
        return documentBusinessLogic.getDocumentContent(document);
    }
}
