package fhtw.wien.genaiworker.service;

import fhtw.wien.genaiworker.config.GenAIConfig;
import fhtw.wien.genaiworker.exception.GenAIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Service for interacting with Google Gemini API.
 * Generates text summaries from document content using the Gemini Pro model.
 */
@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    
    private static final int MAX_INPUT_LENGTH = 50000; // Gemini Pro limit
    private static final String SUMMARY_PROMPT_TEMPLATE = 
            "Please provide a concise summary of the following document in 3-5 sentences. " +
            "Focus on the main topics, key information, and overall purpose of the document.\n\n" +
            "Document content:\n%s";

    private final GenAIConfig config;
    private final RestTemplate restTemplate;

    public GeminiService(GenAIConfig config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Generates a summary for the given text using Google Gemini API.
     *
     * @param text the text to summarize
     * @return the generated summary
     * @throws GenAIException if summarization fails
     */
    public String generateSummary(String text) {
        log.info("🤖 Generating summary using Google Gemini API...");
        long startTime = System.currentTimeMillis();

        try {
            // Validate API key
            if (config.getApi().getKey() == null || config.getApi().getKey().isEmpty()) {
                throw new GenAIException("Gemini API key is not configured");
            }

            // Truncate text if too long
            String processedText = truncateText(text, MAX_INPUT_LENGTH);
            log.debug("Input text length: {} characters (original: {})", 
                    processedText.length(), text.length());

            // Create prompt
            String prompt = String.format(SUMMARY_PROMPT_TEMPLATE, processedText);

            // Build request
            String url = config.getApi().getUrl() + "?key=" + config.getApi().getKey();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = buildRequestBody(prompt);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.debug("Sending request to Gemini API: {}", config.getApi().getUrl());

            // Call API
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            // Extract summary from response
            String summary = extractSummaryFromResponse(response.getBody());
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("✅ Summary generated successfully in {}ms (length: {} characters)", 
                    processingTime, summary.length());
            
            return summary;

        } catch (HttpClientErrorException e) {
            log.error("❌ Gemini API client error: {} - {}", e.getStatusCode(), e.getMessage());
            throw new GenAIException("Gemini API request failed: " + e.getStatusCode(), e);
            
        } catch (HttpServerErrorException e) {
            log.error("❌ Gemini API server error: {} - {}", e.getStatusCode(), e.getMessage());
            throw new GenAIException("Gemini API server error: " + e.getStatusCode(), e);
            
        } catch (Exception e) {
            log.error("❌ Unexpected error calling Gemini API", e);
            throw new GenAIException("Failed to generate summary: " + e.getMessage(), e);
        }
    }

    /**
     * Builds the request body for Gemini API.
     */
    private Map<String, Object> buildRequestBody(String prompt) {
        return Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature", config.getTemperature(),
                        "maxOutputTokens", config.getMaxTokens(),
                        "topP", 0.8,
                        "topK", 40
                )
        );
    }

    /**
     * Extracts the summary text from Gemini API response.
     */
    @SuppressWarnings("unchecked")
    private String extractSummaryFromResponse(Map<String, Object> responseBody) {
        try {
            if (responseBody == null) {
                throw new GenAIException("Empty response from Gemini API");
            }

            List<Map<String, Object>> candidates = 
                    (List<Map<String, Object>>) responseBody.get("candidates");
            
            if (candidates == null || candidates.isEmpty()) {
                throw new GenAIException("No candidates in Gemini API response");
            }

            Map<String, Object> content = 
                    (Map<String, Object>) candidates.get(0).get("content");
            
            List<Map<String, Object>> parts = 
                    (List<Map<String, Object>>) content.get("parts");
            
            if (parts == null || parts.isEmpty()) {
                throw new GenAIException("No text parts in Gemini API response");
            }

            String text = (String) parts.get(0).get("text");
            
            if (text == null || text.trim().isEmpty()) {
                throw new GenAIException("Empty text in Gemini API response");
            }

            return text.trim();

        } catch (ClassCastException | NullPointerException e) {
            log.error("❌ Failed to parse Gemini API response", e);
            throw new GenAIException("Invalid response format from Gemini API", e);
        }
    }

    /**
     * Truncates text to maximum length, trying to preserve sentence boundaries.
     */
    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }

        log.warn("⚠️ Text exceeds maximum length ({}), truncating to {} characters", 
                text.length(), maxLength);

        // Try to truncate at sentence boundary
        String truncated = text.substring(0, maxLength);
        int lastPeriod = truncated.lastIndexOf('.');
        int lastNewline = truncated.lastIndexOf('\n');
        int breakPoint = Math.max(lastPeriod, lastNewline);

        if (breakPoint > maxLength / 2) {
            // Found reasonable break point
            return text.substring(0, breakPoint + 1);
        } else {
            // No good break point, just truncate
            return truncated + "...";
        }
    }

    /**
     * Checks if the Gemini API is properly configured.
     */
    public boolean isConfigured() {
        return config.getApi().getKey() != null && 
               !config.getApi().getKey().isEmpty() &&
               config.getApi().getUrl() != null &&
               !config.getApi().getUrl().isEmpty();
    }
}
