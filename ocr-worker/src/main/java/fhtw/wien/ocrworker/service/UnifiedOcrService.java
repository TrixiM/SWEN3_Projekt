package fhtw.wien.ocrworker.service;

import fhtw.wien.ocrworker.config.OcrConfig;
import fhtw.wien.ocrworker.dto.Document;
import fhtw.wien.ocrworker.dto.OcrResultDto;
import fhtw.wien.ocrworker.util.FileTypeDetector;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class UnifiedOcrService {

    private static final Logger log = LoggerFactory.getLogger(UnifiedOcrService.class);

    private final OcrConfig ocrConfig;
    private final FileTypeDetector fileTypeDetector;
    private final PdfConverterService pdfConverterService;
    private final TesseractOcrService tesseractOcrService;
    private final MinIOClientService minioClientService;

    public UnifiedOcrService(
            OcrConfig ocrConfig,
            FileTypeDetector fileTypeDetector,
            PdfConverterService pdfConverterService,
            TesseractOcrService tesseractOcrService,
            MinIOClientService minioClientService) {

        this.ocrConfig = ocrConfig;
        this.fileTypeDetector = fileTypeDetector;
        this.pdfConverterService = pdfConverterService;
        this.tesseractOcrService = tesseractOcrService;
        this.minioClientService = minioClientService;
    }

    public OcrResultDto processDocument(Document document) {
        log.info("Processing document {} ({})", document.id(), document.title());
        validateDocument(document);
        long startTime = System.currentTimeMillis();

        try {
            byte[] documentData = minioClientService.downloadDocument(document.objectKey()); //download document from MinIO
            FileTypeDetector.FileType fileType = detectFileType(document, documentData); //detects file type

            return switch (fileType) {
                case PDF -> processPdfDocument(document, documentData, startTime);
                case IMAGE -> processImageDocument(document, documentData, startTime);
                case UNSUPPORTED -> createUnsupportedFileResult(document, startTime);
            };

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("OCR processing failed for document {}", document.id(), e);

            return OcrResultDto.failure(
                    document.id(),
                    document.title(),
                    "OCR processing failed: " + e.getMessage(),
                    processingTime
            );
        }
    }

    private void validateDocument(Document document) {
        if (document == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }
        if (document.id() == null) {
            throw new IllegalArgumentException("Document ID cannot be null");
        }
        if (document.objectKey() == null || document.objectKey().isBlank()) {
            throw new IllegalArgumentException("Document object key cannot be null or empty");
        }
        if (!fileTypeDetector.isSupportedContentType(document.contentType())) {
            log.warn("Unsupported content type for document {}: {}", document.id(), document.contentType());
        }
    }

    private FileTypeDetector.FileType detectFileType(Document document, byte[] documentData) throws IOException {
        FileTypeDetector.FileType fileType = fileTypeDetector.getFileTypeFromContentType(document.contentType());
        if (fileType != FileTypeDetector.FileType.UNSUPPORTED) {
            return fileType;
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(documentData)) {
            return fileTypeDetector.detectFileType(inputStream);
        }
    }

    private OcrResultDto processPdfDocument(Document document, byte[] pdfData, long startTime)
            throws IOException, TesseractException {
        //Convert PDF to image
        List<byte[]> pageImages = pdfConverterService.convertPdfToImages(pdfData);
        if (pageImages.isEmpty()) {
            throw new IOException("PDF contains no processable pages");
        }

        List<OcrResultDto.PageResult> pageResults = new ArrayList<>();
        StringBuilder fullText = new StringBuilder();
        int totalConfidence = 0;

        //Loop through pages
        for (int i = 0; i < pageImages.size(); i++) {
            int pageNumber = i + 1;
            long pageStartTime = System.currentTimeMillis();

            try {
                TesseractOcrService.OcrResult ocrResult = tesseractOcrService.extractText(
                        pageImages.get(i), ocrConfig.getDefaultLanguage());

                long pageProcessingTime = System.currentTimeMillis() - pageStartTime;
                pageResults.add(OcrResultDto.fromTesseractResult(
                        pageNumber,
                        ocrResult.text(),
                        ocrResult.confidence(),
                        pageProcessingTime
                ));

                if (!ocrResult.text().isEmpty()) {
                    if (fullText.length() > 0) {
                        fullText.append("\n\n--- Page ").append(pageNumber).append(" ---\n");
                    }
                    fullText.append(ocrResult.text());
                }

                totalConfidence += ocrResult.confidence();

            } catch (Exception e) {
                log.error("Failed to process page {} of document {}", pageNumber, document.id(), e);
                pageResults.add(new OcrResultDto.PageResult(pageNumber, "", 0, 0, false,
                        System.currentTimeMillis() - pageStartTime));
            }
        }
        //Calc overall confidence and processing time
        int overallConfidence = pageResults.isEmpty() ? 0 : totalConfidence / pageResults.size();
        long totalProcessingTime = System.currentTimeMillis() - startTime;

        return OcrResultDto.success(
                document.id(),
                document.title(),
                fullText.toString(),
                pageResults,
                ocrConfig.getDefaultLanguage(),
                overallConfidence,
                totalProcessingTime
        );
    }

    private OcrResultDto processImageDocument(Document document, byte[] imageData, long startTime)
            throws IOException, TesseractException {

        TesseractOcrService.OcrResult ocrResult = tesseractOcrService.extractText(
                imageData, ocrConfig.getDefaultLanguage());

        long processingTime = System.currentTimeMillis() - startTime;
        OcrResultDto.PageResult pageResult = OcrResultDto.fromTesseractResult(
                1, ocrResult.text(), ocrResult.confidence(), processingTime);

        return OcrResultDto.success(
                document.id(),
                document.title(),
                ocrResult.text(),
                List.of(pageResult),
                ocrConfig.getDefaultLanguage(),
                ocrResult.confidence(),
                processingTime
        );
    }

    private OcrResultDto createUnsupportedFileResult(Document document, long startTime) {
        long processingTime = System.currentTimeMillis() - startTime;

        String errorMessage = String.format("Unsupported file type: %s. %s",
                document.contentType(), fileTypeDetector.getSupportedTypesDescription());

        return OcrResultDto.failure(
                document.id(),
                document.title(),
                errorMessage,
                processingTime
        );
    }
}
