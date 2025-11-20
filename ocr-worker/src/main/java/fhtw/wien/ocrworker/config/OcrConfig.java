package fhtw.wien.ocrworker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
@ConfigurationProperties(prefix = "ocr")
@Data
public class OcrConfig {
    

    private String tesseractPath;
    private String tessdataPath;
    private String defaultLanguage = "eng";
    private List<String> supportedLanguages = List.of("eng", "deu", "fra", "spa");
    private int ocrEngineMode = 3;
    private int pageSegMode = 6;
    private int pdfRenderingDpi = 300;
    private String imageFormat = "PNG";
    private long maxFileSizeBytes = 50 * 1024 * 1024; // 50MB
    private int ocrTimeoutSeconds = 30;
    private boolean enablePreprocessing = true;
    private int minConfidenceThreshold = 30;
}