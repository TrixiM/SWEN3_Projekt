package fhtw.wien.genaiworker.service;

import fhtw.wien.genaiworker.dto.OcrResultDto;
import fhtw.wien.genaiworker.dto.SummaryResultMessage;
import fhtw.wien.genaiworker.exception.GenAIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service for processing document summarization requests.
 * Coordinates between OCR results and GenAI summary generation.
 */
@Service
public class SummarizationService {

    private static final Logger log = LoggerFactory.getLogger(SummarizationService.class);

    private final GeminiService geminiService;

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
        log.info("📝 Processing summarization for document: {} ('{}'), {} chars from {} pages",
                ocrMessage.documentId(), ocrMessage.documentTitle(),
                ocrMessage.totalCharacters(), ocrMessage.totalPages());

        long startTime = System.currentTimeMillis();

        try {
            // Validate OCR result
            Optional<String> validationError = validateOcrResult(ocrMessage);
            if (validationError.isPresent()) {
                log.warn("⚠️ Validation failed: {}", validationError.get());
                return CompletableFuture.completedFuture(
                        SummaryResultMessage.failure(
                                ocrMessage.documentId(),
                                ocrMessage.documentTitle(),
                                validationError.get(),
                                System.currentTimeMillis() - startTime
                        )
                );
            }

            // Generate summary (retry/circuit breaker handled by GeminiService)
            String summary = geminiService.generateSummary(ocrMessage.extractedText());

            long processingTime = System.currentTimeMillis() - startTime;
            log.info("✅ Summary generated for document {} in {}ms", ocrMessage.documentId(), processingTime);

            return CompletableFuture.completedFuture(
                    SummaryResultMessage.success(
                            ocrMessage.documentId(),
                            ocrMessage.documentTitle(),
                            summary,
                            processingTime
                    )
            );

        } catch (GenAIException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("❌ GenAI error for document {}: {}", ocrMessage.documentId(), e.getMessage());

            return CompletableFuture.completedFuture(
                    SummaryResultMessage.failure(
                            ocrMessage.documentId(),
                            ocrMessage.documentTitle(),
                            "GenAI API error: " + e.getMessage(),
                            processingTime
                    )
            );

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("❌ Unexpected error for document {}", ocrMessage.documentId(), e);

            return CompletableFuture.completedFuture(
                    SummaryResultMessage.failure(
                            ocrMessage.documentId(),
                            ocrMessage.documentTitle(),
                            "Unexpected error: " + e.getMessage(),
                            processingTime
                    )
            );
        }
    }

    /**
     * Validates the OCR result before processing.
     *
     * @param ocrMessage the OCR message to validate
     * @return Optional containing error message if validation fails, empty otherwise
     */
    private Optional<String> validateOcrResult(OcrResultDto ocrMessage) {
        if (!ocrMessage.isSuccess()) {
            return Optional.of("OCR processing was not successful, cannot generate summary");
        }

        if (!ocrMessage.hasValidText()) {
            return Optional.of(
                    String.format("Extracted text is too short or empty (characters: %d)",
                            ocrMessage.totalCharacters())
            );
        }

        if (!geminiService.isConfigured()) {
            return Optional.of("Gemini API is not properly configured");
        }

        return Optional.empty();
    }
}
