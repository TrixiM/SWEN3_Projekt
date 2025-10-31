package fhtw.wien.service;

import fhtw.wien.business.DocumentBusinessLogic;
import fhtw.wien.business.PdfRenderingBusinessLogic;
import fhtw.wien.domain.Document;
import fhtw.wien.exception.NotFoundException;
import fhtw.wien.messaging.DocumentMessageProducer;
import fhtw.wien.repo.DocumentRepo;
import fhtw.wien.service.DocumentService;
import fhtw.wien.service.MinIOStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentServiceTest {

    private DocumentRepo repo;
    private MinIOStorageService minioStorageService;
    private DocumentBusinessLogic documentBusinessLogic;
    private PdfRenderingBusinessLogic pdfRenderingBusinessLogic;
    private DocumentMessageProducer messageProducer;
    private DocumentService service;

    @BeforeEach
    void setUp() {
        repo = mock(DocumentRepo.class);
        minioStorageService = mock(MinIOStorageService.class);
        documentBusinessLogic = new DocumentBusinessLogic(repo, minioStorageService);
        pdfRenderingBusinessLogic = mock(PdfRenderingBusinessLogic.class);
        messageProducer = mock(DocumentMessageProducer.class);
        service = new DocumentService(documentBusinessLogic, pdfRenderingBusinessLogic, messageProducer);
    }

//    @Test
//    void create_shouldSaveDocument() {
//        Document doc = mock(Document.class);
//        when(repo.save(doc)).thenReturn(doc);
//
//        Document result = service.create(doc);
//
//        assertEquals(doc, result);
//        verify(repo).save(doc);
//    }

    @Test
    void create_shouldSaveDocument() {
        Document doc = new Document(
                "Test Title", "file.txt", "text/plain", 123L,
                "test-bucket", "object-key", "s3://test-bucket/object-key", "abc123"
        );
        when(repo.save(doc)).thenReturn(doc);

        Document result = service.create(doc);

        assertEquals(doc, result);
        verify(repo).save(doc);
    }

    @Test
    void get_shouldReturnDocument() {
        UUID id = UUID.randomUUID();
        Document doc = new Document(
                "Test Title", "file.txt", "text/plain", 123L,
                "test-bucket", "object-key", "s3://test-bucket/object-key", "abc123"
        );
        // Set the ID if your Document class allows it, or use a constructor that sets it
        // e.g., doc.setId(id);
        when(repo.findById(id)).thenReturn(Optional.of(doc));

        Document result = service.get(id);

        assertEquals(doc, result);
        verify(repo).findById(id);
    }

    @Test
    void delete_shouldRemoveDocument() {
        UUID id = UUID.randomUUID();
        Document doc = mock(Document.class);
        when(repo.findById(id)).thenReturn(Optional.of(doc));
        when(doc.getId()).thenReturn(id);

        service.delete(id);

        verify(repo).deleteById(id);
    }

    @Test
    void delete_shouldThrowNotFound() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.delete(id));
    }
}
