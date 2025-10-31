package fhtw.wien.ocrworker.service;

import fhtw.wien.ocrworker.config.OcrConfig;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.ImageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Service for performing OCR text extraction using Tesseract.
 * Provides high-level OCR functionality with configuration support.
 */
@Service
public class TesseractOcrService {
    
    private static final Logger log = LoggerFactory.getLogger(TesseractOcrService.class);
    
    private final OcrConfig ocrConfig;
    private final ITesseract tesseract;
    
    public TesseractOcrService(OcrConfig ocrConfig) {
        this.ocrConfig = ocrConfig;
        this.tesseract = initializeTesseract();
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
        if (imageData == null || imageData.length == 0) {
            throw new IllegalArgumentException("Image data cannot be null or empty");
        }
        
        if (language == null || language.trim().isEmpty()) {
            language = ocrConfig.getDefaultLanguage();
        }
        
        if (!ocrConfig.getSupportedLanguages().contains(language)) {
            log.warn("Unsupported language '{}', using default '{}'", language, ocrConfig.getDefaultLanguage());
            language = ocrConfig.getDefaultLanguage();
        }
        
        log.debug("Extracting text from image: size={} bytes, language={}", imageData.length, language);
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData)) {
            BufferedImage image = ImageIO.read(inputStream);
            
            if (image == null) {
                throw new IOException("Failed to read image data");
            }
            
            // Set language for this operation
            synchronized (tesseract) {
                tesseract.setLanguage(language);
                
                long startTime = System.currentTimeMillis();
                String extractedText = tesseract.doOCR(image);
                long processingTime = System.currentTimeMillis() - startTime;
                
                log.debug("OCR completed in {}ms, extracted {} characters", 
                         processingTime, extractedText != null ? extractedText.length() : 0);
                
                return extractedText != null ? extractedText.trim() : "";
            }
            
        } catch (TesseractException e) {
            log.error("Tesseract OCR failed for language: {}", language, e);
            throw e;
        } catch (IOException e) {
            log.error("Failed to read image data for OCR", e);
            throw e;
        }
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
        if (imageData == null || imageData.length == 0) {
            throw new IllegalArgumentException("Image data cannot be null or empty");
        }
        
        if (language == null || language.trim().isEmpty()) {
            language = ocrConfig.getDefaultLanguage();
        }
        
        log.debug("Extracting text with confidence from image: size={} bytes, language={}", 
                 imageData.length, language);
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData)) {
            BufferedImage image = ImageIO.read(inputStream);
            
            if (image == null) {
                throw new IOException("Failed to read image data");
            }
            
            synchronized (tesseract) {
                tesseract.setLanguage(language);
                
                long startTime = System.currentTimeMillis();
                
                // Extract text
                String extractedText = tesseract.doOCR(image);
                
                // Get confidence (this is a simplified approach - in production you might use
                // more sophisticated confidence calculation methods)
                int meanConfidence = getMeanConfidence(image);
                
                long processingTime = System.currentTimeMillis() - startTime;
                
                OcrResult result = new OcrResult(
                        extractedText != null ? extractedText.trim() : "",
                        meanConfidence,
                        language,
                        processingTime,
                        meanConfidence >= ocrConfig.getMinConfidenceThreshold()
                );
                
                log.debug("OCR completed in {}ms, confidence: {}%, characters: {}", 
                         processingTime, meanConfidence, result.getText().length());
                
                return result;
            }
            
        } catch (TesseractException e) {
            log.error("Tesseract OCR with confidence failed for language: {}", language, e);
            throw e;
        } catch (IOException e) {
            log.error("Failed to read image data for OCR with confidence", e);
            throw e;
        }
    }
    
    /**
     * Validates if Tesseract is properly configured and available.
     *
     * @return true if Tesseract is available, false otherwise
     */
    public boolean isTesseractAvailable() {
        try {
            // Create a small test image
            BufferedImage testImage = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
            
            synchronized (tesseract) {
                tesseract.doOCR(testImage);
            }
            
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
     * Gets the mean confidence score for OCR result.
     * This is a simplified implementation - in production you might use more sophisticated methods.
     *
     * @param image the processed image
     * @return confidence score (0-100)
     */
    private int getMeanConfidence(BufferedImage image) {
        try {
            synchronized (tesseract) {
                // This is a placeholder implementation
                // In a real implementation, you might use Tesseract's confidence methods
                // or implement your own confidence calculation based on image quality
                return 85; // Default confidence for demonstration
            }
        } catch (Exception e) {
            log.warn("Failed to calculate confidence, using default", e);
            return 50; // Fallback confidence
        }
    }
    
    /**
     * Represents an OCR result with confidence information.
     */
    public static class OcrResult {
        private final String text;
        private final int confidence;
        private final String language;
        private final long processingTimeMs;
        private final boolean isHighConfidence;
        
        public OcrResult(String text, int confidence, String language, long processingTimeMs, boolean isHighConfidence) {
            this.text = text;
            this.confidence = confidence;
            this.language = language;
            this.processingTimeMs = processingTimeMs;
            this.isHighConfidence = isHighConfidence;
        }
        
        public String getText() { return text; }
        public int getConfidence() { return confidence; }
        public String getLanguage() { return language; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public boolean isHighConfidence() { return isHighConfidence; }
        
        @Override
        public String toString() {
            return String.format("OcrResult{text='%s...', confidence=%d%%, language='%s', time=%dms, highConfidence=%s}",
                    text.length() > 50 ? text.substring(0, 50) : text,
                    confidence, language, processingTimeMs, isHighConfidence);
        }
    }
}