package fhtw.wien.ocrworker.elasticsearch;

import fhtw.wien.ocrworker.dto.OcrResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElasticsearchServiceTest {
    
    @Mock
    private DocumentIndexRepository repository;
    
    private ElasticsearchService service;
    
    @BeforeEach
    void setUp() {
        service = new ElasticsearchService(repository);
    }
    
    @Test
    void testIndexDocument_Success() {
        // Arrange
        UUID documentId = UUID.randomUUID();
        String title = "Test Document";
        String content = "This is test content for OCR processing";
        
        OcrResultDto ocrResult = OcrResultDto.success(
                documentId,
                title,
                content,
                null,
                "eng",
                85,
                1000L
        );
        
        DocumentIndex expectedIndex = new DocumentIndex(
                documentId,
                title,
                content,
                content.length(),
                0,
                "eng",
                85,
                ocrResult.processedAt()
        );
        
        when(repository.save(any(DocumentIndex.class))).thenReturn(expectedIndex);
        
        // Act
        DocumentIndex result = service.indexDocument(ocrResult);
        
        // Assert
        assertNotNull(result);
        assertEquals(documentId, result.getDocumentId());
        assertEquals(title, result.getTitle());
        assertEquals(content, result.getContent());
        assertEquals(85, result.getConfidence());
        
        ArgumentCaptor<DocumentIndex> captor = ArgumentCaptor.forClass(DocumentIndex.class);
        verify(repository).save(captor.capture());
        
        DocumentIndex captured = captor.getValue();
        assertEquals(documentId, captured.getDocumentId());
        assertEquals(title, captured.getTitle());
        assertEquals(content, captured.getContent());
    }
    
    @Test
    void testIndexDocument_Failure() {
        // Arrange
        UUID documentId = UUID.randomUUID();
        OcrResultDto ocrResult = OcrResultDto.success(
                documentId,
                "Test Document",
                "Test content",
                null,
                "eng",
                85,
                1000L
        );
        
        when(repository.save(any(DocumentIndex.class)))
                .thenThrow(new RuntimeException("Database error"));
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.indexDocument(ocrResult);
        });
        
        assertEquals("Failed to index document", exception.getMessage());
        verify(repository).save(any(DocumentIndex.class));
    }
    
    
    @Test
    void testDeleteDocument_Success() {
        // Arrange
        UUID documentId = UUID.randomUUID();
        doNothing().when(repository).deleteById(documentId.toString());
        
        // Act
        assertDoesNotThrow(() -> service.deleteDocument(documentId));
        
        // Assert
        verify(repository).deleteById(documentId.toString());
    }
    
    @Test
    void testDeleteDocument_Failure() {
        // Arrange
        UUID documentId = UUID.randomUUID();
        doThrow(new RuntimeException("Delete failed"))
                .when(repository).deleteById(documentId.toString());
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.deleteDocument(documentId);
        });
        
        assertEquals("Failed to delete document", exception.getMessage());
        verify(repository).deleteById(documentId.toString());
    }
}
