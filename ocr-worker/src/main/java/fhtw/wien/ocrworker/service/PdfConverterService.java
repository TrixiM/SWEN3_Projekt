package fhtw.wien.ocrworker.service;

import fhtw.wien.ocrworker.config.OcrConfig;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfConverterService {
    
    private static final Logger log = LoggerFactory.getLogger(PdfConverterService.class);
    
    private final OcrConfig ocrConfig;
    
    public PdfConverterService(OcrConfig ocrConfig) {
        this.ocrConfig = ocrConfig;
    }

    public List<byte[]> convertPdfToImages(byte[] pdfData) throws IOException {
        log.debug("Converting PDF to images, size: {} bytes, DPI: {}", 
                pdfData.length, ocrConfig.getPdfRenderingDpi());
        
        List<byte[]> images = new ArrayList<>();
        
        try (PDDocument document = Loader.loadPDF(pdfData)) {
            int pageCount = document.getNumberOfPages();
            log.debug("PDF has {} pages", pageCount);
            
            if (pageCount == 0) {
                log.warn("PDF document has no pages");
                return images;
            }
            
            PDFRenderer renderer = new PDFRenderer(document);
            
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                try {
                    BufferedImage bufferedImage = renderer.renderImageWithDPI(
                            pageIndex, 
                            ocrConfig.getPdfRenderingDpi(), 
                            ImageType.RGB
                    );
                    
                    byte[] imageData = convertImageToBytes(bufferedImage, ocrConfig.getImageFormat());
                    images.add(imageData);
                    
                } catch (IOException e) {
                    log.error("Failed to convert page {} of PDF", pageIndex + 1, e);
                    throw new IOException("Failed to convert PDF page " + (pageIndex + 1), e);
                }
            }
            
            return images;
            
        } catch (IOException e) {
            log.error("Failed to load PDF document", e);
            throw new IOException("Failed to load PDF document", e);
        }
    }
    

    

    private byte[] convertImageToBytes(BufferedImage image, String format) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, format, outputStream)) {
                throw new IOException("Failed to write image in format: " + format);
            }
            return outputStream.toByteArray();
        }
    }
}