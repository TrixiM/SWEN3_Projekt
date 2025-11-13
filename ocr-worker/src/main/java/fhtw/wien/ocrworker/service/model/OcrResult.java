package fhtw.wien.ocrworker.service.model;

/**
 * Immutable record representing OCR extraction result with confidence information.
 * Use the builder pattern for flexible object construction.
 */
public record OcrResult(
        String text,
        int confidence,
        String language,
        long processingTimeMs,
        boolean isHighConfidence
) {
    /**
     * Validates the OcrResult invariants.
     */
    public OcrResult {
        if (text == null) {
            throw new IllegalArgumentException("Text cannot be null");
        }
        if (confidence < 0 || confidence > 100) {
            throw new IllegalArgumentException("Confidence must be between 0 and 100");
        }
        if (language == null || language.isEmpty()) {
            throw new IllegalArgumentException("Language cannot be null or empty");
        }
        if (processingTimeMs < 0) {
            throw new IllegalArgumentException("Processing time cannot be negative");
        }
    }
    
    /**
     * Creates a builder for constructing OcrResult instances.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public String toString() {
        return String.format("OcrResult{text='%s...', confidence=%d%%, language='%s', time=%dms, highConfidence=%s}",
                text.length() > 50 ? text.substring(0, 50) : text,
                confidence, language, processingTimeMs, isHighConfidence);
    }
    
    /**
     * Builder for OcrResult.
     */
    public static class Builder {
        private String text = "";
        private int confidence = 0;
        private String language = "eng";
        private long processingTimeMs = 0;
        private boolean isHighConfidence = false;
        
        public Builder text(String text) {
            this.text = text;
            return this;
        }
        
        public Builder confidence(int confidence) {
            this.confidence = confidence;
            return this;
        }
        
        public Builder language(String language) {
            this.language = language;
            return this;
        }
        
        public Builder processingTimeMs(long processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
            return this;
        }
        
        public Builder isHighConfidence(boolean isHighConfidence) {
            this.isHighConfidence = isHighConfidence;
            return this;
        }
        
        public OcrResult build() {
            return new OcrResult(text, confidence, language, processingTimeMs, isHighConfidence);
        }
    }
}
