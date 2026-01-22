package fhtw.wien.genaiworker.service;

import fhtw.wien.genaiworker.dto.OcrResultDto;
import fhtw.wien.genaiworker.dto.SummaryResultMessage;
import fhtw.wien.genaiworker.exception.GenAIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public class SummarizationService {

    private static final Logger log = LoggerFactory.getLogger(SummarizationService.class);
    private final GeminiService geminiService;
    public SummarizationService(GeminiService geminiService) {
        this.geminiService = geminiService;
    }


    public SummaryResultMessage processSummarization(OcrResultDto ocrMessage) {

        long startTime = System.currentTimeMillis();

        Optional<String> validationError = validateOcrResult(ocrMessage); //Validate OCR Input
        if (validationError.isPresent()) {
            log.warn("⚠️ Validation failed: {}", validationError.get());
            return SummaryResultMessage.failure(
                    ocrMessage.documentId(),
                    ocrMessage.documentTitle(),
                    validationError.get(),
                    System.currentTimeMillis() - startTime
            );
        }
        if(!ocrMessage.minimumTotalCharactersForSummary()){ //if text shorter than 50 chars summary generation is skipped
            return SummaryResultMessage.success(
                    ocrMessage.documentId(),
                    ocrMessage.documentTitle(),
                    ocrMessage.extractedText(),
                    0
            );
        }

        try {
            String summary = geminiService.generateSummary(ocrMessage.extractedText());

            long processingTime = System.currentTimeMillis() - startTime;
            log.info("✅ Summary generated: id={}, time={}ms", ocrMessage.documentId(), processingTime);

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
            log.error("❌ Unexpected error for document {}", ocrMessage.documentId(), e);

            return SummaryResultMessage.failure(
                    ocrMessage.documentId(),
                    ocrMessage.documentTitle(),
                    "Unexpected error: " + e.getMessage(),
                    processingTime
            );
        }
    }


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
