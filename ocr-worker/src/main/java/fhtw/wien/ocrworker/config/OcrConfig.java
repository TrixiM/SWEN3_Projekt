package fhtw.wien.ocrworker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
//Tesseract Configuration
@Configuration
@ConfigurationProperties(prefix = "ocr")
@Data
public class OcrConfig {

    private String tessdataPath; //Path to the folder containing Tesseract language training data files (*.traineddata)
    private String defaultLanguage = "eng"; //default ocr lang
    private List<String> supportedLanguages = List.of("eng", "deu", "fra", "spa"); //only used in test
    private int ocrEngineMode = 3; //default
    private int pageSegMode = 6; //assume single uniform block of text
    private int pdfRenderingDpi = 300; //Controls resolution, the higher -> the better OCR accuracy but slower
    private String imageFormat = "PNG"; //output format when converting pdf -> images
}
