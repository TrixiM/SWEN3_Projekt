package fhtw.wien.genaiworker.service;

import fhtw.wien.genaiworker.dto.OcrResultDto;
import fhtw.wien.genaiworker.dto.SummaryResultMessage;
import fhtw.wien.genaiworker.exception.GenAIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for processing document summarization requests.
 * Coordinates between OCR results and GenAI summary generation with retry logic.
 */
@Service
public class SummarizationService {

    private static final Logger log = LoggerFactory.getLogger(SummarizationService.class);

    private final GeminiService geminiService;

    @Value("${genai.summary.max-input-length:50000}")
    private int maxInputLength;

    @Value("${genai.summary.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${genai.summary.retry.delay-ms:2000}")
    private long retryDelayMs;

    public SummarizationService(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    /**
     * Processes an OCR completion message and generates a summary asynchronously.
     *
     * @param ocrMessage the OCR completion message
     * @return CompletableFuture containing the summary result
     */
    @Async
    public CompletableFuture<SummaryResultMessage> processSummarization(OcrResultDto ocrMessage) {
        log.info("📝 Processing summarization request for document: {} ('{}')", 
                ocrMessage.documentId(), ocrMessage.documentTitle());

        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate OCR result
                if (!ocrMessage.isSuccess()) {
                    String error = "OCR processing was not successful, cannot generate summary";
                    log.warn("⚠️ {}", error);
                    return SummaryResultMessage.failure(
                            ocrMessage.documentId(),
                            ocrMessage.documentTitle(),
                            error,
                            System.currentTimeMillis() - startTime
                    );
                }

                if (!ocrMessage.hasValidText()) {
                    String error = String.format(
                            "Extracted text is too short or empty (characters: %d)",
                            ocrMessage.totalCharacters()
                    );
                    log.warn("⚠️ {}", error);
                    return SummaryResultMessage.failure(
                            ocrMessage.documentId(),
                            ocrMessage.documentTitle(),
                            error,
                            System.currentTimeMillis() - startTime
                    );
                }

                // Check if GenAI is configured
                if (!geminiService.isConfigured()) {
                    String error = "Gemini API is not properly configured";
                    log.error("❌ {}", error);
                    return SummaryResultMessage.failure(
                            ocrMessage.documentId(),
                            ocrMessage.documentTitle(),
                            error,
                            System.currentTimeMillis() - startTime
                    );
                }

                log.info("📄 Document has {} characters from {} pages (confidence: {}%)", 
                        ocrMessage.totalCharacters(), 
                        ocrMessage.totalPages(),
                        ocrMessage.overallConfidence());

                // Generate summary with retry logic
                String summary = generateSummaryWithRetry(ocrMessage.extractedText());

                long processingTime = System.currentTimeMillis() - startTime;
                log.info("✅ Summary generated successfully for document {} in {}ms", 
                        ocrMessage.documentId(), processingTime);

                return SummaryResultMessage.success(
                        ocrMessage.documentId(),
                        ocrMessage.documentTitle(),
                        summary,
                        processingTime
                );

            } catch (GenAIException e) {
                long processingTime = System.currentTimeMillis() - startTime;
                log.error("❌ GenAI error for document {}: {}", ocrMessage.documentId(), e.getMessage());
                
                return SummaryResultMessage.failure(
                        ocrMessage.documentId(),
                        ocrMessage.documentTitle(),
                        "GenAI API error: " + e.getMessage(),
                        processingTime
                );

            } catch (Exception e) {
                long processingTime = System.currentTimeMillis() - startTime;
                log.error("❌ Unexpected error processing summarization for document {}", 
                        ocrMessage.documentId(), e);
                
                return SummaryResultMessage.failure(
                        ocrMessage.documentId(),
                        ocrMessage.documentTitle(),
                        "Unexpected error: " + e.getMessage(),
                        processingTime
                );
            }
        });
    }

    /**
     * Generates summary with retry logic for transient failures.
     */
    private String generateSummaryWithRetry(String text) throws GenAIException {
        GenAIException lastException = null;
        
        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                log.debug("Attempt {}/{} to generate summary", attempt, maxRetryAttempts);
                return geminiService.generateSummary(text);
                
            } catch (GenAIException e) {
                lastException = e;
                log.warn("⚠️ Attempt {}/{} failed: {}", attempt, maxRetryAttempts, e.getMessage());
                
                // Don't retry on client errors (4xx)
                if (e.getMessage().contains("4")) {
                    log.error("❌ Client error detected, not retrying");
                    throw e;
                }
                
                // Wait before retry (except on last attempt)
                if (attempt < maxRetryAttempts) {
                    try {
                        log.debug("Waiting {}ms before retry...", retryDelayMs);
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new GenAIException("Retry interrupted", ie);
                    }
                }
            }
        }
        
        log.error("❌ All {} attempts failed", maxRetryAttempts);
        throw lastException;
    }
}
