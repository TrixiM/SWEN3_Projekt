package fhtw.wien.genaiworker.service;

import fhtw.wien.genaiworker.config.GenAIConfig;
import fhtw.wien.genaiworker.dto.GeminiResponse;
import fhtw.wien.genaiworker.exception.GenAIException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private static final int MAX_INPUT_LENGTH = 50000;
    private static final String SUMMARY_PROMPT_TEMPLATE =
            "Provide a concise summary of the following document in 3-5 sentences. " +
                    "Focus on the main topics, key information, and overall purpose of the document.\n\n" +
                    "Document content:\n%s";

    private final GenAIConfig config;
    private final RestClient restClient;

    // Konstruktor: Hier bauen wir den modernen Client
    public GeminiService(GenAIConfig config, RestClient.Builder builder) {
        this.config = config;

        // Timeouts konfigurieren (etwas anders als beim RestTemplateBuilder)
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10s
        factory.setReadTimeout(30000);    // 30s

        this.restClient = builder
                .baseUrl(config.getApi().getUrl()) // Basis-URL aus Config
                .requestFactory(factory)
                .build();
    }

    @CircuitBreaker(name = "geminiService", fallbackMethod = "generateSummaryFallback") //prevents worker from repeatedly calling gemini if already failing (CLOSED -> GOOD, OPEN --> TOO MANY FAILURES, HALF-OPEN)
    @Retry(name = "geminiService") //retry if call to gemini fails -> amount set in application.properties | ciructBreaker only records failure after third consecutive fail
    @RateLimiter(name = "geminiService") //limits how many requests per time window are allowed
    public String generateSummary(String text) {
        long startTime = System.currentTimeMillis();

        if (!isConfigured()) {
            throw new GenAIException("Gemini API key or URL is not configured");
        }

        String processedText = truncateText(text, MAX_INPUT_LENGTH); //cutting by OCR extractedText up to 50000 char at "." or new line
        Map<String, Object> requestBody = buildRequestBody(String.format(SUMMARY_PROMPT_TEMPLATE, processedText)); //build prompt

        try {
            GeminiResponse response = restClient.post() //Build api request and send
                    .uri("/v1beta/models/{model}:generateContent?key={key}",
                            config.getModel(),
                            config.getApi().getKey()
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(GeminiResponse.class);

            if (response == null) {
                throw new GenAIException("Empty response from Gemini API");
            }

            String summary = response.extractText();
            log.debug("✅ Gemini API: {}ms, {} chars", System.currentTimeMillis() - startTime, summary.length());
            return summary;

        } catch (Exception e) {
            log.error("❌ Error calling Gemini API: {}", e.getMessage());
            throw new GenAIException("Failed to call Gemini API", e);
        }
    }

    @SuppressWarnings("unused") //might look unused by ide but is used by circuitbreaker as fallback
    public String generateSummaryFallback(String text, Throwable t) { //used when circut opens or third attempt of call fails
        log.warn("Gemini fallback triggered", t);
        return "Summary temporarily unavailable.";
    }



    private Map<String, Object> buildRequestBody(String prompt) {
        return Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
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
        log.warn("⚠️ Text exceeds limit ({}), truncating...", text.length());

        String truncated = text.substring(0, maxLength);
        int breakPoint = Math.max(truncated.lastIndexOf('.'), truncated.lastIndexOf('\n'));

        return (breakPoint > maxLength / 2)
                ? text.substring(0, breakPoint + 1)
                : truncated + "...";
    }

    public boolean isConfigured() {
        return config.getApi().getKey() != null && !config.getApi().getKey().isBlank() &&
                config.getApi().getUrl() != null && !config.getApi().getUrl().isBlank();
    }
}