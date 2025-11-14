package fhtw.wien.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for tracking document analytics and statistics.
 * Provides insights into document processing and content quality.
 */
@Entity
@Table(name = "document_analytics")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor
public class DocumentAnalytics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "document_id", nullable = false, unique = true)
    private UUID documentId;
    
    @Column(name = "total_characters")
    private int totalCharacters;
    
    @Column(name = "total_words")
    private int totalWords;
    
    @Column(name = "total_pages")
    private int totalPages;
    
    @Column(name = "average_confidence")
    private int averageConfidence;
    
    @Column(name = "language")
    private String language;
    
    @Column(name = "ocr_processing_time_ms")
    private long ocrProcessingTimeMs;
    
    @Column(name = "word_count_per_page")
    private double wordCountPerPage;
    
    @Column(name = "character_count_per_page")
    private double characterCountPerPage;
    
    @Column(name = "is_high_quality")
    private boolean isHighQuality;
    
    @Column(name = "quality_score")
    private double qualityScore;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    public DocumentAnalytics(UUID documentId, int totalCharacters, int totalWords, 
                            int totalPages, int averageConfidence, String language, 
                            long ocrProcessingTimeMs) {
        this.documentId = documentId;
        this.totalCharacters = totalCharacters;
        this.totalWords = totalWords;
        this.totalPages = totalPages;
        this.averageConfidence = averageConfidence;
        this.language = language;
        this.ocrProcessingTimeMs = ocrProcessingTimeMs;
        
        // Calculate derived metrics
        this.wordCountPerPage = totalPages > 0 ? (double) totalWords / totalPages : 0;
        this.characterCountPerPage = totalPages > 0 ? (double) totalCharacters / totalPages : 0;
        
        // Calculate quality score (weighted average of confidence and content density)
        this.qualityScore = calculateQualityScore();
        this.isHighQuality = this.qualityScore >= 70.0;
    }
    
    /**
     * Calculate quality score based on OCR confidence and content density.
     */
    private double calculateQualityScore() {
        // Confidence weight: 70%
        double confidenceScore = averageConfidence * 0.7;
        
        // Content density weight: 30%
        // Assuming typical page has 300-500 words
        double densityScore = Math.min(wordCountPerPage / 400.0 * 100, 100) * 0.3;
        
        return confidenceScore + densityScore;
    }
    
    /**
     * Update analytics with new data.
     */
    public void updateAnalytics(int totalCharacters, int totalWords, int totalPages, 
                                int averageConfidence, long ocrProcessingTimeMs) {
        this.totalCharacters = totalCharacters;
        this.totalWords = totalWords;
        this.totalPages = totalPages;
        this.averageConfidence = averageConfidence;
        this.ocrProcessingTimeMs = ocrProcessingTimeMs;
        
        this.wordCountPerPage = totalPages > 0 ? (double) totalWords / totalPages : 0;
        this.characterCountPerPage = totalPages > 0 ? (double) totalCharacters / totalPages : 0;
        
        this.qualityScore = calculateQualityScore();
        this.isHighQuality = this.qualityScore >= 70.0;
    }
}
