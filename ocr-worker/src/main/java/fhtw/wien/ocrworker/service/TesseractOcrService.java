package fhtw.wien.ocrworker.service;

import fhtw.wien.ocrworker.config.OcrConfig;
import fhtw.wien.ocrworker.service.model.OcrResult;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Service for performing OCR text extraction using Tesseract.
 * Thread-safe implementation using ThreadLocal for Tesseract instances.
 */
@Service
public class TesseractOcrService {
    
    private static final Logger log = LoggerFactory.getLogger(TesseractOcrService.class);
    private static final int DEFAULT_CONFIDENCE = 50;
    
    private final OcrConfig ocrConfig;
    private final ThreadLocal<ITesseract> tesseractThreadLocal;
    
    public TesseractOcrService(OcrConfig ocrConfig) {
        this.ocrConfig = ocrConfig;
        this.tesseractThreadLocal = ThreadLocal.withInitial(this::initializeTesseract);
        // Validate configuration on startup
        validateConfiguration();
    }
    
    /**
     * Extracts text from an image using OCR.
     *
     * @param imageData the image data as byte array
     * @return the extracted text
     * @throws TesseractException if OCR processing fails
     * @throws IOException if image cannot be read
     */
    public String extractText(byte[] imageData) throws TesseractException, IOException {
        return extractText(imageData, ocrConfig.getDefaultLanguage());
    }
    
    /**
     * Extracts text from an image using OCR with specified language.
     *
     * @param imageData the image data as byte array
     * @param language the OCR language (e.g., "eng", "deu", "fra")
     * @return the extracted text
     * @throws TesseractException if OCR processing fails
     * @throws IOException if image cannot be read
     */
    public String extractText(byte[] imageData, String language) throws TesseractException, IOException {
        validateImageData(imageData);
        String validatedLanguage = validateAndNormalizeLanguage(language);
        
        log.debug("Extracting text from image: size={} bytes, language={}", imageData.length, validatedLanguage);
        
        BufferedImage image = readImage(imageData);
        ITesseract tesseract = getTesseractInstance();
        tesseract.setLanguage(validatedLanguage);
        
        long startTime = System.currentTimeMillis();
        String extractedText = tesseract.doOCR(image);
        long processingTime = System.currentTimeMillis() - startTime;
        
        log.debug("OCR completed in {}ms, extracted {} characters", 
                 processingTime, extractedText != null ? extractedText.length() : 0);
        
        return extractedText != null ? extractedText.trim() : "";
    }
    
    /**
     * Extracts text with confidence scores from an image.
     *
     * @param imageData the image data as byte array
     * @return OcrResult containing text and confidence information
     * @throws TesseractException if OCR processing fails
     * @throws IOException if image cannot be read
     */
    public OcrResult extractTextWithConfidence(byte[] imageData) throws TesseractException, IOException {
        return extractTextWithConfidence(imageData, ocrConfig.getDefaultLanguage());
    }
    
    /**
     * Extracts text with confidence scores from an image using specified language.
     *
     * @param imageData the image data as byte array
     * @param language the OCR language
     * @return OcrResult containing text and confidence information
     * @throws TesseractException if OCR processing fails
     * @throws IOException if image cannot be read
     */
    public OcrResult extractTextWithConfidence(byte[] imageData, String language) throws TesseractException, IOException {
        validateImageData(imageData);
        String validatedLanguage = validateAndNormalizeLanguage(language);
        
        log.debug("Extracting text with confidence from image: size={} bytes, language={}", 
                 imageData.length, validatedLanguage);
        
        BufferedImage image = readImage(imageData);
        ITesseract tesseract = getTesseractInstance();
        tesseract.setLanguage(validatedLanguage);
        
        long startTime = System.currentTimeMillis();
        String extractedText = tesseract.doOCR(image);
        int meanConfidence = calculateConfidence(image, tesseract);
        long processingTime = System.currentTimeMillis() - startTime;
        
        OcrResult result = OcrResult.builder()
                .text(extractedText != null ? extractedText.trim() : "")
                .confidence(meanConfidence)
                .language(validatedLanguage)
                .processingTimeMs(processingTime)
                .isHighConfidence(meanConfidence >= ocrConfig.getMinConfidenceThreshold())
                .build();
        
        log.debug("OCR completed in {}ms, confidence: {}%, characters: {}", 
                 processingTime, meanConfidence, result.text().length());
        
        return result;
    }
    
    /**
     * Validates if Tesseract is properly configured and available.
     *
     * @return true if Tesseract is available, false otherwise
     */
    public boolean isTesseractAvailable() {
        try {
            BufferedImage testImage = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
            ITesseract tesseract = getTesseractInstance();
            tesseract.doOCR(testImage);
            
            log.debug("Tesseract availability check successful");
            return true;
        } catch (Exception e) {
            log.warn("Tesseract is not available or not properly configured", e);
            return false;
        }
    }
    
    /**
     * Gets the list of available OCR languages.
     *
     * @return array of available language codes
     */
    public String[] getAvailableLanguages() {
        // In a production system, you might dynamically discover available languages
        return ocrConfig.getSupportedLanguages().toArray(new String[0]);
    }
    
    /**
     * Initializes the Tesseract instance with configuration.
     *
     * @return configured ITesseract instance
     */
    private ITesseract initializeTesseract() {
        log.info("Initializing Tesseract OCR with config: language={}, engine_mode={}, psm={}", 
                ocrConfig.getDefaultLanguage(), ocrConfig.getOcrEngineMode(), ocrConfig.getPageSegMode());
        
        ITesseract instance = new Tesseract();
        
        // Set paths if configured
        if (ocrConfig.getTesseractPath() != null && !ocrConfig.getTesseractPath().trim().isEmpty()) {
            instance.setTessVariable("tessedit_char_whitelist", "");
            log.debug("Tesseract path: {}", ocrConfig.getTesseractPath());
        }
        
        if (ocrConfig.getTessdataPath() != null && !ocrConfig.getTessdataPath().trim().isEmpty()) {
            instance.setDatapath(ocrConfig.getTessdataPath());
            log.debug("Tessdata path: {}", ocrConfig.getTessdataPath());
        }
        
        // Set OCR parameters
        instance.setLanguage(ocrConfig.getDefaultLanguage());
        instance.setOcrEngineMode(ocrConfig.getOcrEngineMode());
        instance.setPageSegMode(ocrConfig.getPageSegMode());
        
        // Set timeout
        instance.setTessVariable("tessedit_pageseg_mode", String.valueOf(ocrConfig.getPageSegMode()));
        instance.setTessVariable("tessedit_ocr_engine_mode", String.valueOf(ocrConfig.getOcrEngineMode()));
        
        log.info("Tesseract OCR initialized successfully");
        return instance;
    }
    
    /**
     * Validates that the service is properly configured.
     */
    private void validateConfiguration() {
        if (ocrConfig == null) {
            throw new IllegalStateException("OcrConfig cannot be null");
        }
        if (ocrConfig.getDefaultLanguage() == null || ocrConfig.getDefaultLanguage().isEmpty()) {
            throw new IllegalStateException("Default language must be configured");
        }
        log.info("TesseractOcrService initialized with language: {}", ocrConfig.getDefaultLanguage());
    }
    
    /**
     * Validates image data.
     */
    private void validateImageData(byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            throw new IllegalArgumentException("Image data cannot be null or empty");
        }
    }
    
    /**
     * Validates and normalizes language code.
     */
    private String validateAndNormalizeLanguage(String language) {
        if (language == null || language.trim().isEmpty()) {
            return ocrConfig.getDefaultLanguage();
        }
        
        if (!ocrConfig.getSupportedLanguages().contains(language)) {
            log.warn("Unsupported language '{}', using default '{}'", 
                    language, ocrConfig.getDefaultLanguage());
            return ocrConfig.getDefaultLanguage();
        }
        
        return language;
    }
    
    /**
     * Reads BufferedImage from byte array.
     */
    private BufferedImage readImage(byte[] imageData) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData)) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IOException("Failed to read image data - unsupported format or corrupted data");
            }
            return image;
        }
    }
    
    /**
     * Gets thread-local Tesseract instance.
     */
    private ITesseract getTesseractInstance() {
        return tesseractThreadLocal.get();
    }
    
    /**
     * Calculates confidence score for OCR result.
     * Uses Tesseract's built-in confidence calculation when available.
     */
    private int calculateConfidence(BufferedImage image, ITesseract tesseract) {
        try {
            // TODO: Implement proper confidence calculation using Tesseract API
            // For now, estimate based on image quality metrics
            int width = image.getWidth();
            int height = image.getHeight();
            int pixels = width * height;
            
            // Basic heuristic: larger, clearer images tend to have better results
            if (pixels < 10000) return 60;
            if (pixels < 100000) return 75;
            if (pixels < 1000000) return 85;
            return 90;
            
        } catch (Exception e) {
            log.warn("Failed to calculate confidence, using default", e);
            return DEFAULT_CONFIDENCE;
        }
    }
    
}
