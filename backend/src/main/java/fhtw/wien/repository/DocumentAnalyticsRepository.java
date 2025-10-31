package fhtw.wien.repository;

import fhtw.wien.domain.DocumentAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for document analytics operations.
 */
@Repository
public interface DocumentAnalyticsRepository extends JpaRepository<DocumentAnalytics, UUID> {
    
    /**
     * Find analytics by document ID.
     */
    Optional<DocumentAnalytics> findByDocumentId(UUID documentId);
    
    /**
     * Find all high-quality documents.
     */
    List<DocumentAnalytics> findByIsHighQuality(boolean isHighQuality);
    
    /**
     * Find documents by language.
     */
    List<DocumentAnalytics> findByLanguage(String language);
    
    /**
     * Find documents with confidence above threshold.
     */
    List<DocumentAnalytics> findByAverageConfidenceGreaterThanEqual(int minConfidence);
    
    /**
     * Get average quality score across all documents.
     */
    @Query("SELECT AVG(a.qualityScore) FROM DocumentAnalytics a")
    Double getAverageQualityScore();
    
    /**
     * Get total number of pages processed.
     */
    @Query("SELECT SUM(a.totalPages) FROM DocumentAnalytics a")
    Long getTotalPagesProcessed();
    
    /**
     * Get average OCR processing time.
     */
    @Query("SELECT AVG(a.ocrProcessingTimeMs) FROM DocumentAnalytics a")
    Double getAverageProcessingTime();
}
