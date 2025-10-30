package fhtw.wien.service;

import fhtw.wien.dto.DocumentSearchDto;
import fhtw.wien.elasticsearch.DocumentIndex;
import fhtw.wien.elasticsearch.DocumentSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class DocumentSearchServiceTest {
    
    @Mock
    private DocumentSearchRepository repository;
    
    @Mock
    private ElasticsearchOperations elasticsearchOperations;
    
    @Mock
    private SearchHits<DocumentIndex> searchHits;
    
    @Mock
    private SearchHit<DocumentIndex> searchHit;
    
    private DocumentSearchService service;
    
    @BeforeEach
    void setUp() {
        service = new DocumentSearchService(repository, elasticsearchOperations);
    }
    
    @Test
    void testSearch_Success() {
        // Arrange
        String query = "test query";
        UUID documentId = UUID.randomUUID();
        
        DocumentIndex doc = createDocumentIndex(documentId, "Test Document", 
                "This is a test query document with some content");
        
        when(searchHit.getContent()).thenReturn(doc);
        when(searchHits.stream()).thenReturn(List.of(searchHit).stream());
        when(elasticsearchOperations.search(any(Query.class), eq(DocumentIndex.class)))
                .thenReturn(searchHits);
        
        // Act
        List<DocumentSearchDto> results = service.search(query);
        
        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(documentId, results.get(0).documentId());
        assertEquals("Test Document", results.get(0).title());
        assertNotNull(results.get(0).contentSnippet());
        
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
        List<DocumentSearchDto> results = service.search(query);
        
        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
        
        verify(elasticsearchOperations).search(any(Query.class), eq(DocumentIndex.class));
    }
    
    @Test
    void testSearch_LongContentSnippet() {
        // Arrange
        String query = "test";
        UUID documentId = UUID.randomUUID();
        
        // Create content longer than 200 characters
        String longContent = "A".repeat(300);
        DocumentIndex doc = createDocumentIndex(documentId, "Test Document", longContent);
        
        when(searchHit.getContent()).thenReturn(doc);
        when(searchHits.stream()).thenReturn(List.of(searchHit).stream());
        when(elasticsearchOperations.search(any(Query.class), eq(DocumentIndex.class)))
                .thenReturn(searchHits);
        
        // Act
        List<DocumentSearchDto> results = service.search(query);
        
        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.get(0).contentSnippet().length() <= 203); // 200 + "..."
        assertTrue(results.get(0).contentSnippet().endsWith("..."));
    }
    
    @Test
    void testSearchByTitle_Success() {
        // Arrange
        String title = "Test Document";
        UUID documentId = UUID.randomUUID();
        
        DocumentIndex doc = createDocumentIndex(documentId, title, "Test content");
        
        when(repository.findByTitleContaining(title)).thenReturn(List.of(doc));
        
        // Act
        List<DocumentSearchDto> results = service.searchByTitle(title);
        
        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(documentId, results.get(0).documentId());
        assertEquals(title, results.get(0).title());
        
        verify(repository).findByTitleContaining(title);
    }
    
    @Test
    void testSearchByContent_Success() {
        // Arrange
        String content = "specific content";
        UUID documentId = UUID.randomUUID();
        
        DocumentIndex doc = createDocumentIndex(documentId, "Document", 
                "This document contains specific content");
        
        when(repository.findByContentContaining(content)).thenReturn(List.of(doc));
        
        // Act
        List<DocumentSearchDto> results = service.searchByContent(content);
        
        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(documentId, results.get(0).documentId());
        assertTrue(results.get(0).contentSnippet().contains("specific content"));
        
        verify(repository).findByContentContaining(content);
    }
    
    @Test
    void testSearch_Exception() {
        // Arrange
        String query = "test";
        
        when(elasticsearchOperations.search(any(Query.class), eq(DocumentIndex.class)))
                .thenThrow(new RuntimeException("Search error"));
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.search(query);
        });
        
        assertTrue(exception.getMessage().contains("Search failed"));
    }
    
    private DocumentIndex createDocumentIndex(UUID documentId, String title, String content) {
        DocumentIndex doc = new DocumentIndex();
        doc.setId(documentId.toString());
        doc.setDocumentId(documentId);
        doc.setTitle(title);
        doc.setContent(content);
        doc.setTotalCharacters(content.length());
        doc.setTotalPages(1);
        doc.setLanguage("eng");
        doc.setConfidence(85);
        doc.setIndexedAt(Instant.now());
        doc.setProcessedAt(Instant.now());
        return doc;
    }
}
