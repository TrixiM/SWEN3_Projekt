package fhtw.wien.service;

import fhtw.wien.business.DocumentBusinessLogic;
import fhtw.wien.business.PdfRenderingBusinessLogic;
import fhtw.wien.domain.Document;
import fhtw.wien.domain.DocumentAccessStat;
import fhtw.wien.dto.DocumentAccessStatDto;
import fhtw.wien.exception.NotFoundException;
import fhtw.wien.exception.ServiceException;
import fhtw.wien.messaging.DocumentMessageProducer;
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
    private final DocumentMessageService messageService;

    public DocumentService(DocumentBusinessLogic documentBusinessLogic,
                          PdfRenderingBusinessLogic pdfRenderingBusinessLogic,
                          DocumentMessageService messageService) {
        this.documentBusinessLogic = documentBusinessLogic;
        this.pdfRenderingBusinessLogic = pdfRenderingBusinessLogic;
        this.messageService = messageService;
    }


    public Document create(Document doc, InputStream pdfStream) {
        try {
            Document created = documentBusinessLogic.createOrUpdateDocument(doc, pdfStream);
            messageService.publishDocumentCreated(created);
            return created;
        } catch (Exception e) {
            log.error("Failed to create document: {}", doc.getTitle(), e);
            throw new ServiceException("Failed to create document", e);
        }
    }

    public Document update(Document doc) {
        try {
            Document updated = documentBusinessLogic.createOrUpdateDocument(doc, null);
            
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

    public List<DocumentAccessStatDto> getAccessStat(UUID documentId){
        return documentBusinessLogic.getDocumentAccessStats(documentId);
    }
    public void delete(UUID id) {
        try {
            documentBusinessLogic.deleteDocument(id);
            messageService.deleteDocument(id);
            log.debug("Document deleted: id={}", id);
        } catch (NotFoundException e) {
            throw e;
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
