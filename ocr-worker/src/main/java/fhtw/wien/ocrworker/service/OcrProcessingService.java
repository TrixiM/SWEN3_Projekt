package fhtw.wien.ocrworker.service;

import fhtw.wien.ocrworker.dto.Document;
import fhtw.wien.ocrworker.dto.OcrResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OcrProcessingService {

    private static final Logger log = LoggerFactory.getLogger(OcrProcessingService.class);

    private final UnifiedOcrService unifiedOcrService;

    public OcrProcessingService(UnifiedOcrService unifiedOcrService) {
        this.unifiedOcrService = unifiedOcrService;
    }

    public OcrResultDto processDocument(Document document) {
        log.info("Processing document {} ({})", document.id(), document.title());
        try {
            return unifiedOcrService.processDocument(document);
        } catch (Exception e) {
            log.error("OCR failed for {}", document.id(), e);
            return OcrResultDto.failure(document.id(), document.title(), "OCR error: " + e.getMessage(), 0L);
        }
    }
}
