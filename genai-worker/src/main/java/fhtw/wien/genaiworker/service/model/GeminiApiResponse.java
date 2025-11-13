package fhtw.wien.genaiworker.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Typed model for Gemini API response.
 * Provides type-safe access to response fields.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiApiResponse(
        List<Candidate> candidates,
        PromptFeedback promptFeedback
) {
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(
            Content content,
            String finishReason,
            Integer index,
            List<SafetyRating> safetyRatings
    ) {}
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(
            List<Part> parts,
            String role
    ) {}
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Part(
            String text
    ) {}
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SafetyRating(
            String category,
            String probability
    ) {}
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PromptFeedback(
            List<SafetyRating> safetyRatings
    ) {}
    
    /**
     * Extracts the generated text from the response.
     * 
     * @return the generated text
     * @throws IllegalStateException if response structure is invalid
     */
    public String extractText() {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("No candidates in Gemini API response");
        }
        
        Candidate candidate = candidates.get(0);
        if (candidate.content() == null) {
            throw new IllegalStateException("No content in Gemini API candidate");
        }
        
        List<Part> parts = candidate.content().parts();
        if (parts == null || parts.isEmpty()) {
            throw new IllegalStateException("No parts in Gemini API content");
        }
        
        String text = parts.get(0).text();
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalStateException("Empty text in Gemini API response");
        }
        
        return text.trim();
    }
}
