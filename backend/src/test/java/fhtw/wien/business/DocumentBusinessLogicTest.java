package fhtw.wien.business;

import fhtw.wien.domain.Document;
import fhtw.wien.domain.DocumentStatus;
import fhtw.wien.exception.DataAccessException;
import fhtw.wien.exception.InvalidRequestException;
import fhtw.wien.exception.NotFoundException;
import fhtw.wien.repo.DocumentRepo;
import fhtw.wien.service.MinIOStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentBusinessLogicTest {

    @Mock
    private DocumentRepo repository;

    @Mock
    private MinIOStorageService minioStorageService;

    @InjectMocks
    private DocumentBusinessLogic businessLogic;

    private Document testDocument;
    private UUID testId;
    private byte[] testPdfData;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testPdfData = "Test PDF content".getBytes();
        
        testDocument = new Document(
                "Test Document",
                "test.pdf",
                "application/pdf",
                testPdfData.length,
                "test-bucket",
                "object-key",
                "s3://test-bucket/object-key",
                "checksum123"
        );
        testDocument.setPdfData(testPdfData);
    }

    // CREATE TESTS

    @Test
    void createOrUpdateDocument_WithNewDocument_ShouldUploadToMinioAndSave() {
        String expectedObjectKey = "documents/2024/01/01/" + testId + "-test.pdf";
        
        when(minioStorageService.uploadDocument(any(UUID.class), anyString(), anyString(), any(byte[].class)))
                .thenReturn(expectedObjectKey);
        when(minioStorageService.getBucketName()).thenReturn("test-bucket");
        when(repository.save(any(Document.class))).thenReturn(testDocument);

        Document result = businessLogic.createOrUpdateDocument(testDocument);

        assertNotNull(result);
        verify(minioStorageService).uploadDocument(any(UUID.class), eq("test.pdf"), eq("application/pdf"), eq(testPdfData));
        verify(repository).save(any(Document.class));
    }

    @Test
    void createOrUpdateDocument_WhenMinioUploadFails_ShouldThrowDataAccessException() {
        when(minioStorageService.uploadDocument(any(UUID.class), anyString(), anyString(), any(byte[].class)))
                .thenThrow(new RuntimeException("MinIO upload failed"));

        assertThrows(DataAccessException.class, () -> 
            businessLogic.createOrUpdateDocument(testDocument)
        );

        verify(minioStorageService).uploadDocument(any(UUID.class), anyString(), anyString(), any(byte[].class));
        verify(repository, never()).save(any(Document.class));
    }

    @Test
    void createOrUpdateDocument_WhenDatabaseSaveFails_ShouldCleanupMinioAndThrowException() {
        String objectKey = "test-object-key";
        
        when(minioStorageService.uploadDocument(any(UUID.class), anyString(), anyString(), any(byte[].class)))
                .thenReturn(objectKey);
        when(minioStorageService.getBucketName()).thenReturn("test-bucket");
        when(repository.save(any(Document.class))).thenThrow(new RuntimeException("Database error"));

        assertThrows(DataAccessException.class, () -> 
            businessLogic.createOrUpdateDocument(testDocument)
        );

        verify(minioStorageService).deleteDocument(objectKey);
    }

    // VALIDATION TESTS

    @Test
    void createOrUpdateDocument_WithNullDocument_ShouldThrowInvalidRequestException() {
        assertThrows(InvalidRequestException.class, () -> 
            businessLogic.createOrUpdateDocument(null)
        );
    }

    @Test
    void createOrUpdateDocument_WithEmptyTitle_ShouldThrowInvalidRequestException() {
        testDocument.setTitle("");

        assertThrows(InvalidRequestException.class, () -> 
            businessLogic.createOrUpdateDocument(testDocument)
        );
    }

    @Test
    void createOrUpdateDocument_WithNullTitle_ShouldThrowInvalidRequestException() {
        testDocument.setTitle(null);

        assertThrows(InvalidRequestException.class, () -> 
            businessLogic.createOrUpdateDocument(testDocument)
        );
    }

    @Test
    void createOrUpdateDocument_WithEmptyFilename_ShouldThrowInvalidRequestException() {
        testDocument.setOriginalFilename("");

        assertThrows(InvalidRequestException.class, () -> 
            businessLogic.createOrUpdateDocument(testDocument)
        );
    }

    @Test
    void createOrUpdateDocument_WithZeroSize_ShouldThrowInvalidRequestException() {
        testDocument.setSizeBytes(0);

        assertThrows(InvalidRequestException.class, () -> 
            businessLogic.createOrUpdateDocument(testDocument)
        );
    }

    @Test
    void createOrUpdateDocument_WithExcessiveSize_ShouldThrowInvalidRequestException() {
        testDocument.setSizeBytes(101 * 1024 * 1024); // 101 MB

        assertThrows(InvalidRequestException.class, () -> 
            businessLogic.createOrUpdateDocument(testDocument)
        );
    }

    // GET TESTS

    @Test
    void getDocumentById_WithValidId_ShouldReturnDocument() {
        testDocument.setId(testId);
        when(repository.findById(testId)).thenReturn(Optional.of(testDocument));

        Document result = businessLogic.getDocumentById(testId);

        assertNotNull(result);
        assertEquals(testId, result.getId());
        assertEquals("Test Document", result.getTitle());
        verify(repository).findById(testId);
    }

    @Test
    void getDocumentById_WithInvalidId_ShouldThrowNotFoundException() {
        when(repository.findById(testId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> 
            businessLogic.getDocumentById(testId)
        );

        verify(repository).findById(testId);
    }

    @Test
    void getDocumentById_WithNullId_ShouldThrowInvalidRequestException() {
        assertThrows(InvalidRequestException.class, () -> 
            businessLogic.getDocumentById(null)
        );

        verify(repository, never()).findById(any());
    }

    @Test
    void getAllDocuments_ShouldReturnListOfDocuments() {
        Document doc1 = new Document("Doc 1", "file1.pdf", "application/pdf", 1024L, "bucket", "key1", "uri1", "checksum1");
        Document doc2 = new Document("Doc 2", "file2.pdf", "application/pdf", 2048L, "bucket", "key2", "uri2", "checksum2");
        
        when(repository.findAll()).thenReturn(List.of(doc1, doc2));

        List<Document> result = businessLogic.getAllDocuments();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAll();
    }

    @Test
    void getAllDocuments_WhenEmpty_ShouldReturnEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        List<Document> result = businessLogic.getAllDocuments();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository).findAll();
    }

    // DELETE TESTS

    @Test
    void deleteDocument_WithValidId_ShouldDeleteFromMinioAndDatabase() {
        testDocument.setId(testId);
        testDocument.setObjectKey("test-object-key");
        
        when(repository.findById(testId)).thenReturn(Optional.of(testDocument));
        doNothing().when(minioStorageService).deleteDocument(anyString());
        doNothing().when(repository).deleteById(testId);

        businessLogic.deleteDocument(testId);

        verify(repository).findById(testId);
        verify(minioStorageService).deleteDocument("test-object-key");
        verify(repository).deleteById(testId);
    }

    @Test
    void deleteDocument_WhenMinioDeleteFails_ShouldStillDeleteFromDatabase() {
        testDocument.setId(testId);
        testDocument.setObjectKey("test-object-key");
        
        when(repository.findById(testId)).thenReturn(Optional.of(testDocument));
        doThrow(new RuntimeException("MinIO delete failed")).when(minioStorageService).deleteDocument(anyString());
        doNothing().when(repository).deleteById(testId);

        // Should not throw exception
        businessLogic.deleteDocument(testId);

        verify(minioStorageService).deleteDocument("test-object-key");
        verify(repository).deleteById(testId);
    }

    @Test
    void deleteDocument_WithNonExistentId_ShouldThrowNotFoundException() {
        when(repository.findById(testId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> 
            businessLogic.deleteDocument(testId)
        );

        verify(repository).findById(testId);
        verify(minioStorageService, never()).deleteDocument(anyString());
        verify(repository, never()).deleteById(any());
    }

    @Test
    void deleteDocument_WithNullId_ShouldThrowInvalidRequestException() {
        assertThrows(InvalidRequestException.class, () -> 
            businessLogic.deleteDocument(null)
        );

        verify(repository, never()).findById(any());
    }

    // GET CONTENT TESTS

    @Test
    void getDocumentContent_WithValidDocument_ShouldReturnPdfData() {
        testDocument.setObjectKey("test-object-key");
        byte[] expectedData = "PDF content from MinIO".getBytes();
        
        when(minioStorageService.downloadDocument("test-object-key")).thenReturn(expectedData);

        byte[] result = businessLogic.getDocumentContent(testDocument);

        assertNotNull(result);
        assertArrayEquals(expectedData, result);
        verify(minioStorageService).downloadDocument("test-object-key");
    }

    @Test
    void getDocumentContent_WithoutObjectKey_ShouldThrowDataAccessException() {
        testDocument.setObjectKey(null);

        assertThrows(DataAccessException.class, () -> 
            businessLogic.getDocumentContent(testDocument)
        );

        verify(minioStorageService, never()).downloadDocument(anyString());
    }

    @Test
    void getDocumentContent_WhenMinioDownloadFails_ShouldThrowDataAccessException() {
        testDocument.setObjectKey("test-object-key");
        
        when(minioStorageService.downloadDocument("test-object-key"))
                .thenThrow(new RuntimeException("MinIO download failed"));

        assertThrows(DataAccessException.class, () -> 
            businessLogic.getDocumentContent(testDocument)
        );

        verify(minioStorageService).downloadDocument("test-object-key");
    }
}
