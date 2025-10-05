package fhtw.wien.service;

import fhtw.wien.business.DocumentBusinessLogic;
import fhtw.wien.business.PdfRenderingBusinessLogic;
import fhtw.wien.domain.Document;
import fhtw.wien.dto.DocumentResponse;
import fhtw.wien.messaging.DocumentMessageProducer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

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
        Document created = documentBusinessLogic.createOrUpdateDocument(doc);
        // Publish message after document is created
        DocumentResponse response = toDocumentResponse(created);
        messageProducer.publishDocumentCreated(response);
        return created;
    }

    public Document update(Document doc) {
        Document updated = documentBusinessLogic.createOrUpdateDocument(doc);
        // Publish message after document is created
        DocumentResponse response = toDocumentResponse(updated);
        return updated;
    }


    public Document get(UUID id) {
        return documentBusinessLogic.getDocumentById(id);
    }

    public List<Document> getAll() {
        return documentBusinessLogic.getAllDocuments();
    }

    public void delete(UUID id) {
        documentBusinessLogic.deleteDocument(id);
        // Publish message after document is deleted
        messageProducer.publishDocumentDeleted(id);
    }

    private DocumentResponse toDocumentResponse(Document d) {
        return new DocumentResponse(
                d.getId(),
                d.getTitle(),
                d.getOriginalFilename(),
                d.getContentType(),
                d.getSizeBytes(),
                d.getBucket(),
                d.getObjectKey(),
                d.getStorageUri(),
                d.getChecksumSha256(),
                d.getStatus(),
                d.getVersion(),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }

    public byte[] renderPdfPage(UUID id, int pageNumber, float scale) {
        var doc = documentBusinessLogic.getDocumentById(id);
        return pdfRenderingBusinessLogic.renderPdfPage(doc, pageNumber, scale);
    }

    public int getPdfPageCount(UUID id) {
        var doc = documentBusinessLogic.getDocumentById(id);
        return pdfRenderingBusinessLogic.getPdfPageCount(doc);
    }
}
