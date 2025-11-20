package fhtw.wien.ocrworker.service;

import fhtw.wien.ocrworker.config.OcrConfig;
import fhtw.wien.ocrworker.dto.DocumentResponse;
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
    

    public OcrResultDto processDocument(DocumentResponse document) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Validate document
            validateDocument(document);
            
            // Download document from MinIO
            byte[] documentData = downloadDocumentFromStorage(document);
            
            // Detect file type
            FileTypeDetector.FileType fileType = detectFileType(document, documentData);
            
            // Process based on file type
            OcrResultDto result = switch (fileType) {
                case PDF -> processPdfDocument(document, documentData, startTime);
                case IMAGE -> processImageDocument(document, documentData, startTime);
                case UNSUPPORTED -> createUnsupportedFileResult(document, startTime);
            };
            
            return result;
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("OCR processing failed for document: {}", document.id(), e);
            
            return OcrResultDto.failure(
                    document.id(),
                    document.title(),
                    "OCR processing failed: " + e.getMessage(),
                    processingTime
            );
        }
    }
    

    private void validateDocument(DocumentResponse document) {
        if (document == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }
        
        if (document.id() == null) {
            throw new IllegalArgumentException("Document ID cannot be null");
        }
        
        if (document.objectKey() == null || document.objectKey().trim().isEmpty()) {
            throw new IllegalArgumentException("Document object key cannot be null or empty");
        }
        
        if (!fileTypeDetector.isSupportedContentType(document.contentType())) {
            log.warn("Unsupported content type for document {}: {}", document.id(), document.contentType());
        }
    }
    

    private byte[] downloadDocumentFromStorage(DocumentResponse document) throws IOException {
        return minioClientService.downloadDocument(document.objectKey());
    }
    

    private FileTypeDetector.FileType detectFileType(DocumentResponse document, byte[] documentData) throws IOException {
        // Detect file type using magic number analysis
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(documentData)) {
            return fileTypeDetector.detectFileType(inputStream);
        }
    }
    

    private OcrResultDto processPdfDocument(DocumentResponse document, byte[] pdfData, long startTime) 
            throws IOException, TesseractException {
        
        // Convert PDF pages to images
        List<byte[]> pageImages = pdfConverterService.convertPdfToImages(pdfData);
        
        if (pageImages.isEmpty()) {
            throw new IOException("PDF contains no processable pages");
        }
        
        // Process each page with OCR
        List<OcrResultDto.PageResult> pageResults = new ArrayList<>();
        StringBuilder fullText = new StringBuilder();
        int totalConfidence = 0;
        
        for (int i = 0; i < pageImages.size(); i++) {
            int pageNumber = i + 1;
            long pageStartTime = System.currentTimeMillis();
            
            try {
                // Extract text with confidence
                TesseractOcrService.OcrResult ocrResult = tesseractOcrService.extractTextWithConfidence(
                        pageImages.get(i), ocrConfig.getDefaultLanguage());
                
                long pageProcessingTime = System.currentTimeMillis() - pageStartTime;
                
                // Create page result
                OcrResultDto.PageResult pageResult = OcrResultDto.fromTesseractResult(
                        pageNumber, 
                        ocrResult.getText(), 
                        ocrResult.getConfidence(),
                        pageProcessingTime
                );
                
                pageResults.add(pageResult);
                
                // Append to full text
                if (!ocrResult.getText().isEmpty()) {
                    if (fullText.length() > 0) {
                        fullText.append("\n\n--- Page ").append(pageNumber).append(" ---\n");
                    }
                    fullText.append(ocrResult.getText());
                }
                
                totalConfidence += ocrResult.getConfidence();
                
            } catch (Exception e) {
                log.error("Failed to process page {} of document {}", pageNumber, document.id(), e);
                
                // Add failed page result
                OcrResultDto.PageResult failedPageResult = new OcrResultDto.PageResult(
                        pageNumber, "", 0, 0, false, System.currentTimeMillis() - pageStartTime);
                pageResults.add(failedPageResult);
            }
        }
        
        // Calculate overall confidence
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
    

    private OcrResultDto processImageDocument(DocumentResponse document, byte[] imageData, long startTime) 
            throws IOException, TesseractException {
        
        // Extract text with confidence
        TesseractOcrService.OcrResult ocrResult = tesseractOcrService.extractTextWithConfidence(
                imageData, ocrConfig.getDefaultLanguage());
        
        long processingTime = System.currentTimeMillis() - startTime;
        
        // Create single page result
        OcrResultDto.PageResult pageResult = OcrResultDto.fromTesseractResult(
                1, ocrResult.getText(), ocrResult.getConfidence(), processingTime);
        
        return OcrResultDto.success(
                document.id(),
                document.title(),
                ocrResult.getText(),
                List.of(pageResult),
                ocrConfig.getDefaultLanguage(),
                ocrResult.getConfidence(),
                processingTime
        );
    }

    private OcrResultDto createUnsupportedFileResult(DocumentResponse document, long startTime) {
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