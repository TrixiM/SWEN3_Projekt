package fhtw.wien.elasticsearch;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.util.UUID;

/**
 * Elasticsearch document entity for searching OCR text content.
 * This mirrors the structure indexed by the OCR worker.
 */
@Document(indexName = "documents")
public class DocumentIndex {
    
    @Id
    private String id;
    
    @Field(type = FieldType.Keyword)
    private UUID documentId;
    
    @Field(type = FieldType.Text)
    private String title;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String content;
    
    @Field(type = FieldType.Integer)
    private int totalCharacters;
    
    @Field(type = FieldType.Integer)
    private int totalPages;
    
    @Field(type = FieldType.Keyword)
    private String language;
    
    @Field(type = FieldType.Integer)
    private int confidence;
    
    @Field(type = FieldType.Date)
    private Instant indexedAt;
    
    @Field(type = FieldType.Date)
    private Instant processedAt;
    
    public DocumentIndex() {
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public UUID getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public int getTotalCharacters() {
        return totalCharacters;
    }
    
    public void setTotalCharacters(int totalCharacters) {
        this.totalCharacters = totalCharacters;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public int getConfidence() {
        return confidence;
    }
    
    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }
    
    public Instant getIndexedAt() {
        return indexedAt;
    }
    
    public void setIndexedAt(Instant indexedAt) {
        this.indexedAt = indexedAt;
    }
    
    public Instant getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}
