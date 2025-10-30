package fhtw.wien.service;

import fhtw.wien.domain.DocumentAnalytics;
import fhtw.wien.dto.AnalyticsSummaryDto;
import fhtw.wien.dto.DocumentAnalyticsDto;
import fhtw.wien.repository.DocumentAnalyticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentAnalyticsServiceTest {
    
    @Mock
    private DocumentAnalyticsRepository repository;
    
    private DocumentAnalyticsService service;
    
    @BeforeEach
    void setUp() {
        service = new DocumentAnalyticsService(repository);
    }
    
    @Test
    void testCreateOrUpdateAnalytics_NewDocument() {
        // Arrange
        UUID documentId = UUID.randomUUID();
        int totalCharacters = 1000;
        int totalWords = 200;
        int totalPages = 2;
        int confidence = 85;
        String language = "eng";
        long processingTime = 5000L;
        
        when(repository.findByDocumentId(documentId)).thenReturn(Optional.empty());
        when(repository.save(any(DocumentAnalytics.class))).thenAnswer(i -> i.getArgument(0));
        
        // Act
        DocumentAnalyticsDto result = service.createOrUpdateAnalytics(
                documentId, totalCharacters, totalWords, totalPages, 
                confidence, language, processingTime);
        
        // Assert
        assertNotNull(result);
        assertEquals(documentId, result.documentId());
        assertEquals(totalCharacters, result.totalCharacters());
        assertEquals(totalWords, result.totalWords());
        assertEquals(totalPages, result.totalPages());
        assertEquals(confidence, result.averageConfidence());
        assertEquals(language, result.language());
        
        ArgumentCaptor<DocumentAnalytics> captor = ArgumentCaptor.forClass(DocumentAnalytics.class);
        verify(repository).save(captor.capture());
        
        DocumentAnalytics saved = captor.getValue();
        assertEquals(documentId, saved.getDocumentId());
        assertTrue(saved.getQualityScore() > 0);
    }
    
    @Test
    void testCreateOrUpdateAnalytics_ExistingDocument() {
        // Arrange
        UUID documentId = UUID.randomUUID();
        DocumentAnalytics existing = new DocumentAnalytics(
                documentId, 500, 100, 1, 70, "eng", 3000L);
        
        when(repository.findByDocumentId(documentId)).thenReturn(Optional.of(existing));
        when(repository.save(any(DocumentAnalytics.class))).thenAnswer(i -> i.getArgument(0));
        
        // Act
        DocumentAnalyticsDto result = service.createOrUpdateAnalytics(
                documentId, 1000, 200, 2, 85, "eng", 5000L);
        
        // Assert
        assertNotNull(result);
        assertEquals(1000, result.totalCharacters());
        assertEquals(200, result.totalWords());
        assertEquals(85, result.averageConfidence());
        
        verify(repository).findByDocumentId(documentId);
        verify(repository).save(existing);
    }
    
    @Test
    void testGetAnalyticsByDocumentId_Found() {
        // Arrange
        UUID documentId = UUID.randomUUID();
        DocumentAnalytics analytics = new DocumentAnalytics(
                documentId, 1000, 200, 2, 85, "eng", 5000L);
        
        when(repository.findByDocumentId(documentId)).thenReturn(Optional.of(analytics));
        
        // Act
        Optional<DocumentAnalyticsDto> result = service.getAnalyticsByDocumentId(documentId);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(documentId, result.get().documentId());
        
        verify(repository).findByDocumentId(documentId);
    }
    
    @Test
    void testGetAnalyticsByDocumentId_NotFound() {
        // Arrange
        UUID documentId = UUID.randomUUID();
        when(repository.findByDocumentId(documentId)).thenReturn(Optional.empty());
        
        // Act
        Optional<DocumentAnalyticsDto> result = service.getAnalyticsByDocumentId(documentId);
        
        // Assert
        assertFalse(result.isPresent());
        verify(repository).findByDocumentId(documentId);
    }
    
    @Test
    void testGetHighQualityDocuments() {
        // Arrange
        DocumentAnalytics doc1 = new DocumentAnalytics(
                UUID.randomUUID(), 1000, 200, 2, 90, "eng", 5000L);
        DocumentAnalytics doc2 = new DocumentAnalytics(
                UUID.randomUUID(), 1200, 250, 3, 95, "eng", 6000L);
        
        when(repository.findByIsHighQuality(true)).thenReturn(List.of(doc1, doc2));
        
        // Act
        List<DocumentAnalyticsDto> results = service.getHighQualityDocuments();
        
        // Assert
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(DocumentAnalyticsDto::isHighQuality));
        
        verify(repository).findByIsHighQuality(true);
    }
    
    @Test
    void testGetDocumentsByLanguage() {
        // Arrange
        String language = "eng";
        DocumentAnalytics doc = new DocumentAnalytics(
                UUID.randomUUID(), 1000, 200, 2, 85, language, 5000L);
        
        when(repository.findByLanguage(language)).thenReturn(List.of(doc));
        
        // Act
        List<DocumentAnalyticsDto> results = service.getDocumentsByLanguage(language);
        
        // Assert
        assertEquals(1, results.size());
        assertEquals(language, results.get(0).language());
        
        verify(repository).findByLanguage(language);
    }
    
    @Test
    void testGetDocumentsByMinConfidence() {
        // Arrange
        int minConfidence = 80;
        DocumentAnalytics doc = new DocumentAnalytics(
                UUID.randomUUID(), 1000, 200, 2, 85, "eng", 5000L);
        
        when(repository.findByAverageConfidenceGreaterThanEqual(minConfidence))
                .thenReturn(List.of(doc));
        
        // Act
        List<DocumentAnalyticsDto> results = service.getDocumentsByMinConfidence(minConfidence);
        
        // Assert
        assertEquals(1, results.size());
        assertTrue(results.get(0).averageConfidence() >= minConfidence);
        
        verify(repository).findByAverageConfidenceGreaterThanEqual(minConfidence);
    }
    
    @Test
    void testGetAnalyticsSummary() {
        // Arrange
        DocumentAnalytics doc1 = new DocumentAnalytics(
                UUID.randomUUID(), 1000, 200, 2, 85, "eng", 5000L);
        DocumentAnalytics doc2 = new DocumentAnalytics(
                UUID.randomUUID(), 1500, 300, 3, 90, "eng", 7000L);
        
        when(repository.count()).thenReturn(2L);
        when(repository.findByIsHighQuality(true)).thenReturn(List.of(doc1, doc2));
        when(repository.getAverageQualityScore()).thenReturn(87.5);
        when(repository.getTotalPagesProcessed()).thenReturn(5L);
        when(repository.getAverageProcessingTime()).thenReturn(6000.0);
        when(repository.findAll()).thenReturn(List.of(doc1, doc2));
        
        // Act
        AnalyticsSummaryDto summary = service.getAnalyticsSummary();
        
        // Assert
        assertEquals(2, summary.totalDocuments());
        assertEquals(2, summary.highQualityDocuments());
        assertEquals(87.5, summary.averageQualityScore());
        assertEquals(5, summary.totalPagesProcessed());
        assertEquals(6000.0, summary.averageProcessingTimeMs());
        assertEquals(2500, summary.totalCharactersProcessed());
        assertEquals(500, summary.totalWordsProcessed());
    }
}
