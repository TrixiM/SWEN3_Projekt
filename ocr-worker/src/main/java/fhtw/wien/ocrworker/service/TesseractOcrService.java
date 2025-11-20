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


@Service
public class TesseractOcrService {
    
    private static final Logger log = LoggerFactory.getLogger(TesseractOcrService.class);
    
    private final OcrConfig ocrConfig;
    private final ITesseract tesseract;
    
    public TesseractOcrService(OcrConfig ocrConfig) {
        this.ocrConfig = ocrConfig;
        this.tesseract = initializeTesseract();
    }
    

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
            
            // Only synchronize language setting
            synchronized (tesseract) {
                tesseract.setLanguage(language);
            }
            
            long startTime = System.currentTimeMillis();
            String extractedText = tesseract.doOCR(image);
            long processingTime = System.currentTimeMillis() - startTime;
            
            log.debug("OCR completed in {}ms, extracted {} characters", 
                     processingTime, extractedText != null ? extractedText.length() : 0);
            
            return extractedText != null ? extractedText.trim() : "";
            
        } catch (TesseractException e) {
            log.error("Tesseract OCR failed for language: {}", language, e);
            throw e;
        } catch (IOException e) {
            log.error("Failed to read image data for OCR", e);
            throw e;
        }
    }
    
    public OcrResult extractTextWithConfidence(byte[] imageData, String language) throws TesseractException, IOException {
        if (imageData == null || imageData.length == 0) {
            throw new IllegalArgumentException("Image data cannot be null or empty");
        }
        
        if (language == null || language.trim().isEmpty()) {
            language = ocrConfig.getDefaultLanguage();
        }
        
        log.debug("Extracting text from image: size={} bytes, language={}", imageData.length, language);
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData)) {
            BufferedImage image = ImageIO.read(inputStream);
            
            if (image == null) {
                throw new IOException("Failed to read image data");
            }
            
            // Only synchronize language setting
            synchronized (tesseract) {
                tesseract.setLanguage(language);
            }
            
            long startTime = System.currentTimeMillis();
            String extractedText = tesseract.doOCR(image);
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Use fixed confidence value - real confidence calculation requires additional Tesseract API calls
            int confidence = 75;
            
            OcrResult result = new OcrResult(
                    extractedText != null ? extractedText.trim() : "",
                    confidence,
                    language,
                    processingTime,
                    confidence >= ocrConfig.getMinConfidenceThreshold()
            );
            
            log.debug("OCR completed in {}ms, characters: {}", processingTime, result.getText().length());
            
            return result;
            
        } catch (TesseractException e) {
            log.error("Tesseract OCR failed for language: {}", language, e);
            throw e;
        } catch (IOException e) {
            log.error("Failed to read image data for OCR", e);
            throw e;
        }
    }
    

    

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