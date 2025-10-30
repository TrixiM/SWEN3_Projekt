package fhtw.wien.ocrworker.elasticsearch;

import fhtw.wien.ocrworker.dto.OcrResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElasticsearchServiceTest {
    
    @Mock
    private DocumentIndexRepository repository;
    
    @Mock
    private ElasticsearchOperations elasticsearchOperations;
    
    @Mock
    private SearchHits<DocumentIndex> searchHits;
    
    @Mock
    private SearchHit<DocumentIndex> searchHit;
    
    private ElasticsearchService service;
    
    @BeforeEach
    void setUp() {
        service = new ElasticsearchService(repository, elasticsearchOperations);
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
    void testSearch_Success() {
        // Arrange
        String query = "test query";
        UUID documentId = UUID.randomUUID();
        
        DocumentIndex doc1 = new DocumentIndex(
                documentId,
                "Test Document",
                "This is a test query document",
                28,
                1,
                "eng",
                85,
                Instant.now()
        );
        
        when(searchHit.getContent()).thenReturn(doc1);
        when(searchHits.stream()).thenReturn(List.of(searchHit).stream());
        when(elasticsearchOperations.search(any(Query.class), eq(DocumentIndex.class)))
                .thenReturn(searchHits);
        
        // Act
        List<DocumentIndex> results = service.search(query);
        
        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(doc1.getDocumentId(), results.get(0).getDocumentId());
        assertEquals(doc1.getTitle(), results.get(0).getTitle());
        
        verify(elasticsearchOperations).search(any(Query.class), eq(DocumentIndex.class));
    }
    
    @Test
    void testSearch_NoResults() {
        // Arrange
        String query = "nonexistent";
        
        when(searchHits.stream()).thenReturn(List.<SearchHit<DocumentIndex>>of().stream());
        when(elasticsearchOperations.search(any(Query.class), eq(DocumentIndex.class)))
                .thenReturn(searchHits);
        
        // Act
        List<DocumentIndex> results = service.search(query);
        
        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
        
        verify(elasticsearchOperations).search(any(Query.class), eq(DocumentIndex.class));
    }
    
    @Test
    void testFindByDocumentId() {
        // Arrange
        UUID documentId = UUID.randomUUID();
        DocumentIndex expectedDoc = new DocumentIndex(
                documentId,
                "Test Document",
                "Test content",
                12,
                1,
                "eng",
                85,
                Instant.now()
        );
        
        when(repository.findByDocumentId(documentId)).thenReturn(expectedDoc);
        
        // Act
        DocumentIndex result = service.findByDocumentId(documentId);
        
        // Assert
        assertNotNull(result);
        assertEquals(documentId, result.getDocumentId());
        assertEquals("Test Document", result.getTitle());
        
        verify(repository).findByDocumentId(documentId);
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
