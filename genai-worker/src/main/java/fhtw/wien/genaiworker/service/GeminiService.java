package fhtw.wien.genaiworker.service;

import fhtw.wien.genaiworker.config.GenAIConfig;
import fhtw.wien.genaiworker.exception.GenAIException;
import fhtw.wien.genaiworker.service.model.GeminiApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Service for interacting with Google Gemini API.
 * Generates text summaries from document content using the Gemini Pro model.
 * Thread-safe and protected by resilience4j patterns (circuit breaker, retry, rate limiter).
 */
@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    
    // API Limits and Configuration
    private static final int MAX_INPUT_LENGTH = 50_000; // Gemini Pro character limit
    private static final int MIN_CONFIDENCE_THRESHOLD = 70; // Minimum confidence for "high confidence"
    private static final double TOP_P = 0.8;
    private static final int TOP_K = 40;
    
    private static final String SUMMARY_PROMPT_TEMPLATE = 
            "Please provide a concise summary of the following document in 3-5 sentences. " +
            "Focus on the main topics, key information, and overall purpose of the document.\n\n" +
            "Document content:\n%s";

    private final GenAIConfig config;
    private final RestTemplate restTemplate;

    public GeminiService(GenAIConfig config, RestTemplateBuilder restTemplateBuilder) {
        this.config = config;
        // Configure RestTemplate with proper timeouts
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Generates a summary for the given text using Google Gemini API.
     * Protected by circuit breaker, retry logic, and rate limiter.
     *
     * @param text the text to summarize
     * @return the generated summary
     * @throws GenAIException if summarization fails
     */
    @CircuitBreaker(name = "geminiService", fallbackMethod = "generateSummaryFallback")
    @Retry(name = "geminiService")
    @RateLimiter(name = "geminiService")
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

            // Call API with typed response
            ResponseEntity<GeminiApiResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    GeminiApiResponse.class
            );

            // Extract summary from typed response
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
                        "topP", TOP_P,
                        "topK", TOP_K
                )
        );
    }

    /**
     * Extracts the summary text from typed Gemini API response.
     */
    private String extractSummaryFromResponse(GeminiApiResponse response) {
        if (response == null) {
            throw new GenAIException("Empty response from Gemini API");
        }
        
        try {
            return response.extractText();
        } catch (IllegalStateException e) {
            log.error("❌ Failed to extract text from Gemini API response", e);
            throw new GenAIException(e.getMessage(), e);
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
     * Fallback method for generateSummary when circuit is open or retries exhausted.
     */
    private String generateSummaryFallback(String text, Exception ex) {
        log.warn("⚠️ Gemini API unavailable, using fallback. Reason: {}", ex.getMessage());
        return "[Summary temporarily unavailable - Gemini API service is experiencing issues. " +
               "This document contains " + text.length() + " characters and will be summarized once the service recovers.]";
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
