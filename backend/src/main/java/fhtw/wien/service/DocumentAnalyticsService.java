package fhtw.wien.service;

import fhtw.wien.domain.DocumentAnalytics;
import fhtw.wien.dto.AnalyticsSummaryDto;
import fhtw.wien.dto.DocumentAnalyticsDto;
import fhtw.wien.repository.DocumentAnalyticsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing document analytics.
 */
@Service
public class DocumentAnalyticsService {
    
    private static final Logger log = LoggerFactory.getLogger(DocumentAnalyticsService.class);
    
    private final DocumentAnalyticsRepository repository;
    
    public DocumentAnalyticsService(DocumentAnalyticsRepository repository) {
        this.repository = repository;
    }
    
    /**
     * Create or update analytics for a document.
     */
    @Transactional
    public DocumentAnalyticsDto createOrUpdateAnalytics(UUID documentId, int totalCharacters, 
                                                        int totalWords, int totalPages, 
                                                        int averageConfidence, String language, 
                                                        long ocrProcessingTimeMs) {
        log.info("📊 Creating/updating analytics for document: {}", documentId);
        
        Optional<DocumentAnalytics> existing = repository.findByDocumentId(documentId);
        
        DocumentAnalytics analytics;
        if (existing.isPresent()) {
            analytics = existing.get();
            analytics.updateAnalytics(totalCharacters, totalWords, totalPages, 
                                     averageConfidence, ocrProcessingTimeMs);
            log.info("✏️ Updated existing analytics for document: {}", documentId);
        } else {
            analytics = new DocumentAnalytics(documentId, totalCharacters, totalWords, 
                                             totalPages, averageConfidence, language, 
                                             ocrProcessingTimeMs);
            log.info("✨ Created new analytics for document: {}", documentId);
        }
        
        analytics = repository.save(analytics);
        log.info("✅ Analytics saved for document: {} (quality score: {})", 
                documentId, analytics.getQualityScore());
        
        return mapToDto(analytics);
    }
    
    /**
     * Get analytics for a specific document.
     */
    public Optional<DocumentAnalyticsDto> getAnalyticsByDocumentId(UUID documentId) {
        log.info("🔍 Retrieving analytics for document: {}", documentId);
        return repository.findByDocumentId(documentId).map(this::mapToDto);
    }
    
    /**
     * Get all high-quality documents.
     */
    public List<DocumentAnalyticsDto> getHighQualityDocuments() {
        log.info("🔍 Retrieving high-quality documents");
        List<DocumentAnalytics> documents = repository.findByIsHighQuality(true);
        log.info("✅ Found {} high-quality documents", documents.size());
        return documents.stream().map(this::mapToDto).collect(Collectors.toList());
    }
    
    /**
     * Get documents by language.
     */
    public List<DocumentAnalyticsDto> getDocumentsByLanguage(String language) {
        log.info("🔍 Retrieving documents by language: {}", language);
        return repository.findByLanguage(language).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get documents with minimum confidence.
     */
    public List<DocumentAnalyticsDto> getDocumentsByMinConfidence(int minConfidence) {
        log.info("🔍 Retrieving documents with confidence >= {}", minConfidence);
        return repository.findByAverageConfidenceGreaterThanEqual(minConfidence).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get overall analytics summary.
     */
    public AnalyticsSummaryDto getAnalyticsSummary() {
        log.info("📊 Generating analytics summary");
        
        long totalDocuments = repository.count();
        long highQualityDocuments = repository.findByIsHighQuality(true).size();
        Double averageQualityScore = repository.getAverageQualityScore();
        Long totalPagesProcessed = repository.getTotalPagesProcessed();
        Double averageProcessingTime = repository.getAverageProcessingTime();
        
        // Calculate total characters and words
        List<DocumentAnalytics> allAnalytics = repository.findAll();
        long totalCharacters = allAnalytics.stream()
                .mapToLong(DocumentAnalytics::getTotalCharacters)
                .sum();
        long totalWords = allAnalytics.stream()
                .mapToLong(DocumentAnalytics::getTotalWords)
                .sum();
        
        log.info("✅ Analytics summary generated: {} total documents, {} high-quality", 
                totalDocuments, highQualityDocuments);
        
        return new AnalyticsSummaryDto(
                totalDocuments,
                highQualityDocuments,
                averageQualityScore != null ? averageQualityScore : 0.0,
                totalPagesProcessed != null ? totalPagesProcessed : 0L,
                averageProcessingTime != null ? averageProcessingTime : 0.0,
                totalCharacters,
                totalWords
        );
    }
    
    /**
     * Map DocumentAnalytics entity to DTO.
     */
    private DocumentAnalyticsDto mapToDto(DocumentAnalytics analytics) {
        return new DocumentAnalyticsDto(
                analytics.getId(),
                analytics.getDocumentId(),
                analytics.getTotalCharacters(),
                analytics.getTotalWords(),
                analytics.getTotalPages(),
                analytics.getAverageConfidence(),
                analytics.getLanguage(),
                analytics.getOcrProcessingTimeMs(),
                analytics.getWordCountPerPage(),
                analytics.getCharacterCountPerPage(),
                analytics.isHighQuality(),
                analytics.getQualityScore(),
                analytics.getCreatedAt(),
                analytics.getUpdatedAt()
        );
    }
}
