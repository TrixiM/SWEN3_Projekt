package fhtw.wien.repository;

import fhtw.wien.domain.DocumentAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface DocumentAnalyticsRepository extends JpaRepository<DocumentAnalytics, UUID> {

    Optional<DocumentAnalytics> findByDocumentId(UUID documentId);
    List<DocumentAnalytics> findByIsHighQuality(boolean isHighQuality);
    List<DocumentAnalytics> findByLanguage(String language);
    List<DocumentAnalytics> findByAverageConfidenceGreaterThanEqual(int minConfidence);
    

    @Query("SELECT AVG(a.qualityScore) FROM DocumentAnalytics a")
    Double getAverageQualityScore();

    @Query("SELECT SUM(a.totalPages) FROM DocumentAnalytics a")
    Long getTotalPagesProcessed();

    @Query("SELECT AVG(a.ocrProcessingTimeMs) FROM DocumentAnalytics a")
    Double getAverageProcessingTime();
}
