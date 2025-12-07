package fhtw.wien.ocrworker.elasticsearch;

import fhtw.wien.ocrworker.dto.OcrResultDto;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.util.UUID;
import java.util.Objects;


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
    

    DocumentIndex(UUID documentId, String title, String content,
                  int totalCharacters, int totalPages, String language,
                  int confidence, Instant processedAt) {
        this.id = documentId.toString();
        this.documentId = documentId;
        this.title = title;
        this.content = content;
        this.totalCharacters = totalCharacters;
        this.totalPages = totalPages;
        this.language = language;
        this.confidence = confidence;
        this.processedAt = processedAt;
        this.indexedAt = Instant.now();
    }
    
    public static DocumentIndex from(OcrResultDto ocrResult) {
        Objects.requireNonNull(ocrResult, "ocrResult");
        return new DocumentIndex(
                ocrResult.documentId(),
                defaultString(ocrResult.documentTitle()),
                defaultString(ocrResult.extractedText()),
                ocrResult.totalCharacters(),
                ocrResult.totalPages(),
                defaultString(ocrResult.language()),
                ocrResult.overallConfidence(),
                ocrResult.processedAt()
        );
    }
    
    private static String defaultString(String value) {
        return value == null ? "" : value;
    }
    public UUID getDocumentId() {
        return documentId;
    }
    public String getTitle() {
        return title;
    }
    public String getContent() {
        return content;
    }
    public int getConfidence() {
        return confidence;
    }
    

}
