package fhtw.wien.genaiworker.service;

import fhtw.wien.genaiworker.config.GenAIConfig;
import fhtw.wien.genaiworker.dto.GeminiResponse;
import fhtw.wien.genaiworker.exception.GenAIException;
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

    public GeminiService(GenAIConfig config, RestTemplateBuilder restTemplateBuilder) {
        this.config = config;
        // Configure RestTemplate with proper timeouts
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    @CircuitBreaker(name = "geminiService")
    @Retry(name = "geminiService")
    @RateLimiter(name = "geminiService")
    public String generateSummary(String text) {
        log.debug("🤖 Calling Gemini API (input: {} chars)", text.length());
        long startTime = System.currentTimeMillis();

        try {
            // Validate API key
            if (config.getApi().getKey() == null || config.getApi().getKey().isEmpty()) {
                throw new GenAIException("Gemini API key is not configured");
            }

            // Truncate text if too long
            String processedText = truncateText(text, MAX_INPUT_LENGTH);

            // Create prompt and request
            String prompt = String.format(SUMMARY_PROMPT_TEMPLATE, processedText);
            String url = config.getApi().getUrl() + "?key=" + config.getApi().getKey();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(buildRequestBody(prompt), headers);

            // Call API with type-safe response parsing
            ResponseEntity<GeminiResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    GeminiResponse.class
            );

            if (response.getBody() == null) {
                throw new GenAIException("Empty response from Gemini API");
            }

            String summary = response.getBody().extractText();
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.debug("✅ Gemini API returned summary in {}ms ({} chars)", processingTime, summary.length());
            
            return summary;

        } catch (HttpClientErrorException e) {
            log.error("❌ Gemini API client error: {}", e.getStatusCode());
            throw new GenAIException("Gemini API client error: " + e.getStatusCode(), e);
            
        } catch (HttpServerErrorException e) {
            log.error("❌ Gemini API server error: {}", e.getStatusCode());
            throw new GenAIException("Gemini API server error: " + e.getStatusCode(), e);
            
        } catch (GenAIException e) {
            throw e; // Re-throw GenAIException as-is
            
        } catch (Exception e) {
            log.error("❌ Unexpected error calling Gemini API", e);
            throw new GenAIException("Failed to generate summary: " + e.getMessage(), e);
        }
    }


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

    public boolean isConfigured() {
        return config.getApi().getKey() != null && 
               !config.getApi().getKey().isEmpty() &&
               config.getApi().getUrl() != null &&
               !config.getApi().getUrl().isEmpty();
    }
}
