package fhtw.wien.genaiworker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiResponse(
        List<Candidate> candidates
) {
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(
            Content content,
            String finishReason,
            int index
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

    public String extractText() {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("No candidates in Gemini API response");
        }
        
        Content content = candidates.get(0).content();
        if (content == null || content.parts() == null || content.parts().isEmpty()) {
            throw new IllegalStateException("No content parts in Gemini API response");
        }
        
        String text = content.parts().get(0).text();
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalStateException("Empty text in Gemini API response");
        }
        
        return text.trim();
    }
}
