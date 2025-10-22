package fhtw.wien.ocrworker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration class for OCR processing settings.
 * Manages Tesseract OCR and PDF processing parameters.
 */
@Configuration
@ConfigurationProperties(prefix = "ocr")
@Data
public class OcrConfig {
    
    /**
     * Path to Tesseract executable.
     * Default locations are checked if not specified.
     */
    private String tesseractPath;
    
    /**
     * Path to Tesseract tessdata directory.
     * Contains language model files.
     */
    private String tessdataPath;
    
    /**
     * Default language for OCR processing.
     * Can be overridden per document.
     */
    private String defaultLanguage = "eng";
    
    /**
     * Supported languages for OCR.
     * Must have corresponding tessdata files.
     */
    private List<String> supportedLanguages = List.of("eng", "deu", "fra", "spa");
    
    /**
     * OCR Engine Mode.
     * 0 = Legacy engine only
     * 1 = Neural nets LSTM engine only
     * 2 = Legacy + LSTM engines
     * 3 = Default, based on what is available
     */
    private int ocrEngineMode = 3;
    
    /**
     * Page Segmentation Mode.
     * 1 = Automatic page segmentation with OSD
     * 3 = Fully automatic page segmentation, but no OSD
     * 6 = Uniform block of text (default)
     * 8 = Treat the image as a single word
     * 13 = Raw line. Treat the image as a single text line
     */
    private int pageSegMode = 6;
    
    /**
     * DPI (Dots Per Inch) for PDF to image conversion.
     * Higher values provide better quality but larger file sizes.
     */
    private int pdfRenderingDpi = 300;
    
    /**
     * Image format for PDF to image conversion.
     */
    private String imageFormat = "PNG";
    
    /**
     * Maximum file size for processing (in bytes).
     * Files larger than this will be rejected.
     */
    private long maxFileSizeBytes = 50 * 1024 * 1024; // 50MB
    
    /**
     * Timeout for OCR processing per page (in seconds).
     */
    private int ocrTimeoutSeconds = 30;
    
    /**
     * Enable text preprocessing (denoising, contrast enhancement).
     */
    private boolean enablePreprocessing = true;
    
    /**
     * Minimum confidence threshold for OCR results (0-100).
     * Results below this threshold will be flagged as low confidence.
     */
    private int minConfidenceThreshold = 30;
}