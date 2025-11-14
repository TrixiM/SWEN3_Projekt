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

/**
 * Unified service that coordinates all OCR processing operations.
 * Handles file type detection, PDF conversion, OCR extraction, and result aggregation.
 */
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
    
    /**
     * Processes a document from MinIO storage for OCR text extraction.
     *
     * @param document the document metadata from the message queue
     * @return OCR processing result
     */
    public OcrResultDto processDocument(DocumentResponse document) {
        long startTime = System.currentTimeMillis();
        
        log.info("Starting OCR processing for document: {} ('{}')", document.id(), document.title());
        
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
            
            log.info("OCR processing completed for document: {} - {}", document.id(), result.getSummary());
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
    
    /**
     * Validates the document for OCR processing.
     *
     * @param document the document to validate
     * @throws IllegalArgumentException if document is invalid
     */
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
        
        if (!fileTypeDetector.isValidFileSize(document.sizeBytes(), ocrConfig.getMaxFileSizeBytes())) {
            throw new IllegalArgumentException("Document size exceeds maximum allowed size");
        }
    }
    
    /**
     * Downloads document content from MinIO storage.
     *
     * @param document the document metadata
     * @return the document data as byte array
     * @throws IOException if download fails
     */
    private byte[] downloadDocumentFromStorage(DocumentResponse document) throws IOException {
        log.debug("Downloading document from MinIO: bucket={}, key={}", document.bucket(), document.objectKey());
        
        try {
            byte[] data = minioClientService.downloadDocument(document.objectKey());
            
            log.debug("Successfully downloaded document: {} bytes", data.length);
            return data;
            
        } catch (Exception e) {
            log.error("Failed to download document from MinIO: {}", document.objectKey(), e);
            throw new IOException("Failed to download document from storage", e);
        }
    }
    
    /**
     * Detects the file type for processing strategy.
     *
     * @param document the document metadata
     * @param documentData the document data
     * @return the detected file type
     * @throws IOException if file type detection fails
     */
    private FileTypeDetector.FileType detectFileType(DocumentResponse document, byte[] documentData) throws IOException {
        log.debug("Detecting file type for document: {}", document.id());
        
        // First try by content type
        FileTypeDetector.FileType typeFromContentType = fileTypeDetector.getFileTypeFromContentType(document.contentType());
        
        // Then verify by magic number
        FileTypeDetector.FileType typeFromMagicNumber;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(documentData)) {
            typeFromMagicNumber = fileTypeDetector.detectFileType(inputStream);
        }
        
        // Use magic number detection as authoritative
        if (typeFromMagicNumber != FileTypeDetector.FileType.UNSUPPORTED) {
            if (typeFromContentType != typeFromMagicNumber) {
                log.warn("Content type mismatch for document {}: content-type says {}, magic number says {}", 
                        document.id(), typeFromContentType, typeFromMagicNumber);
            }
            return typeFromMagicNumber;
        }
        
        return typeFromContentType;
    }
    
    /**
     * Processes a PDF document for OCR.
     *
     * @param document the document metadata
     * @param pdfData the PDF data
     * @param startTime the processing start time
     * @return OCR result
     * @throws IOException if processing fails
     */
    private OcrResultDto processPdfDocument(DocumentResponse document, byte[] pdfData, long startTime) 
            throws IOException, TesseractException {
        
        log.debug("Processing PDF document: {}", document.id());
        
        // Validate PDF
        if (!pdfConverterService.isValidPdf(pdfData)) {
            throw new IOException("Invalid or corrupted PDF document");
        }
        
        // Convert PDF pages to images
        List<byte[]> pageImages = pdfConverterService.convertPdfToImages(pdfData);
        
        if (pageImages.isEmpty()) {
            throw new IOException("PDF contains no processable pages");
        }
        
        log.debug("Converted PDF to {} images for OCR", pageImages.size());
        
        // Process each page with OCR
        List<OcrResultDto.PageResult> pageResults = new ArrayList<>();
        StringBuilder fullText = new StringBuilder();
        int totalConfidence = 0;
        
        for (int i = 0; i < pageImages.size(); i++) {
            int pageNumber = i + 1;
            long pageStartTime = System.currentTimeMillis();
            
            try {
                // Preprocess image if enabled
                byte[] imageData = pdfConverterService.preprocessImage(pageImages.get(i));
                
                // Extract text with confidence
                TesseractOcrService.OcrResult ocrResult = tesseractOcrService.extractTextWithConfidence(
                        imageData, ocrConfig.getDefaultLanguage());
                
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
                
                log.debug("Processed page {}/{}: {} characters, {}% confidence", 
                         pageNumber, pageImages.size(), ocrResult.getText().length(), ocrResult.getConfidence());
                
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
    
    /**
     * Processes an image document for OCR.
     *
     * @param document the document metadata
     * @param imageData the image data
     * @param startTime the processing start time
     * @return OCR result
     * @throws IOException if processing fails
     */
    private OcrResultDto processImageDocument(DocumentResponse document, byte[] imageData, long startTime) 
            throws IOException, TesseractException {
        
        log.debug("Processing image document: {}", document.id());
        
        // Preprocess image if enabled
        byte[] processedImageData = pdfConverterService.preprocessImage(imageData);
        
        // Extract text with confidence
        TesseractOcrService.OcrResult ocrResult = tesseractOcrService.extractTextWithConfidence(
                processedImageData, ocrConfig.getDefaultLanguage());
        
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
    
    /**
     * Creates a result for unsupported file types.
     *
     * @param document the document metadata
     * @param startTime the processing start time
     * @return OCR result indicating unsupported file type
     */
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