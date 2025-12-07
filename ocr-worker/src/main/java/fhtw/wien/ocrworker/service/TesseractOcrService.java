package fhtw.wien.ocrworker.service;

import fhtw.wien.ocrworker.config.OcrConfig;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.ITessAPI.TessPageIteratorLevel;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
public class TesseractOcrService {

    private static final Logger log = LoggerFactory.getLogger(TesseractOcrService.class);

    private final OcrConfig ocrConfig;
    private final ITesseract tesseract;

    public TesseractOcrService(OcrConfig ocrConfig) {
        this.ocrConfig = ocrConfig;
        this.tesseract = createTesseract();
    }

    public OcrResult extractText(byte[] imageData, String language) throws TesseractException, IOException {
        if (imageData == null || imageData.length == 0) {
            throw new IllegalArgumentException("Image data cannot be null or empty");
        }

        String resolvedLanguage = (language == null || language.isBlank())
                ? ocrConfig.getDefaultLanguage()
                : language.trim();

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData)) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IOException("Failed to read image data");
            }

            String extractedText;
            long processingTime;
            int confidence;

            synchronized (tesseract) {
                tesseract.setLanguage(resolvedLanguage);
                long startTime = System.currentTimeMillis();
                extractedText = tesseract.doOCR(image);
                processingTime = System.currentTimeMillis() - startTime;
                confidence = calculateConfidence(image);
            }

            String cleanedText = extractedText == null ? "" : extractedText.trim();
            if (cleanedText.isEmpty()) {
                confidence = 0;
            }

            log.debug("OCR completed in {} ms ({} chars)", processingTime, cleanedText.length());
            return new OcrResult(cleanedText, confidence, resolvedLanguage, processingTime);
        } catch (TesseractException | IOException e) {
            log.error("Tesseract OCR failed (language={})", resolvedLanguage, e);
            throw e;
        }
    }

    private int calculateConfidence(BufferedImage image) throws TesseractException {
        var words = tesseract.getWords(image, TessPageIteratorLevel.RIL_WORD);
        if (words == null || words.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (Word word : words) {
            total += word.getConfidence();
        }
        return total / words.size();
    }

    private ITesseract createTesseract() {
        ITesseract instance = new Tesseract();

        if (ocrConfig.getTessdataPath() != null && !ocrConfig.getTessdataPath().isBlank()) {
            instance.setDatapath(ocrConfig.getTessdataPath());
            log.info("Using custom tessdata path: {}", ocrConfig.getTessdataPath());
        } else {
            log.info("Using system tessdata (TESSDATA_PREFIX) and default Tesseract install path");
        }

        instance.setLanguage(ocrConfig.getDefaultLanguage());
        instance.setOcrEngineMode(ocrConfig.getOcrEngineMode());
        instance.setPageSegMode(ocrConfig.getPageSegMode());
        return instance;
    }

    public record OcrResult(String text, int confidence, String language, long processingTimeMs) {
        @Override
        public String toString() {
            String preview = text == null ? "" : text;
            if (preview.length() > 50) {
                preview = preview.substring(0, 50);
            }
            return "OcrResult{text='" + preview + "', confidence=" + confidence +
                    "%, language='" + language + "', time=" + processingTimeMs + "ms}";
        }
    }
}
