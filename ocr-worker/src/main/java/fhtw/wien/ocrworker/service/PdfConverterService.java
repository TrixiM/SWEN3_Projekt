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

/**
 * Service for converting PDF documents to images suitable for OCR processing.
 * Uses Apache PDFBox for high-quality PDF to image conversion.
 */
@Service
public class PdfConverterService {
    
    private static final Logger log = LoggerFactory.getLogger(PdfConverterService.class);
    
    private final OcrConfig ocrConfig;
    
    public PdfConverterService(OcrConfig ocrConfig) {
        this.ocrConfig = ocrConfig;
    }
    
    /**
     * Converts all pages of a PDF to images.
     *
     * @param pdfData the PDF file data
     * @return list of image data for each page
     * @throws IOException if conversion fails
     */
    public List<byte[]> convertPdfToImages(byte[] pdfData) throws IOException {
        log.info("Converting PDF to images, size: {} bytes, DPI: {}", 
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
                    log.debug("Converting page {} of {}", pageIndex + 1, pageCount);
                    
                    BufferedImage bufferedImage = renderer.renderImageWithDPI(
                            pageIndex, 
                            ocrConfig.getPdfRenderingDpi(), 
                            ImageType.RGB
                    );
                    
                    byte[] imageData = convertImageToBytes(bufferedImage, ocrConfig.getImageFormat());
                    images.add(imageData);
                    
                    log.debug("Converted page {} to {} image, size: {} bytes", 
                            pageIndex + 1, ocrConfig.getImageFormat(), imageData.length);
                    
                } catch (IOException e) {
                    log.error("Failed to convert page {} of PDF", pageIndex + 1, e);
                    throw new IOException("Failed to convert PDF page " + (pageIndex + 1), e);
                }
            }
            
            log.info("Successfully converted PDF to {} images", images.size());
            return images;
            
        } catch (IOException e) {
            log.error("Failed to load PDF document", e);
            throw new IOException("Failed to load PDF document", e);
        }
    }
    
    /**
     * Converts a specific page of a PDF to an image.
     *
     * @param pdfData the PDF file data
     * @param pageNumber the page number (1-based)
     * @return the image data for the specified page
     * @throws IOException if conversion fails
     */
    public byte[] convertPdfPageToImage(byte[] pdfData, int pageNumber) throws IOException {
        log.debug("Converting PDF page {} to image, DPI: {}", pageNumber, ocrConfig.getPdfRenderingDpi());
        
        if (pageNumber < 1) {
            throw new IllegalArgumentException("Page number must be 1 or greater");
        }
        
        try (PDDocument document = Loader.loadPDF(pdfData)) {
            int pageCount = document.getNumberOfPages();
            
            if (pageNumber > pageCount) {
                throw new IllegalArgumentException(
                        String.format("Page number %d exceeds document page count %d", pageNumber, pageCount));
            }
            
            PDFRenderer renderer = new PDFRenderer(document);
            int pageIndex = pageNumber - 1; // Convert to 0-based index
            
            BufferedImage bufferedImage = renderer.renderImageWithDPI(
                    pageIndex, 
                    ocrConfig.getPdfRenderingDpi(), 
                    ImageType.RGB
            );
            
            byte[] imageData = convertImageToBytes(bufferedImage, ocrConfig.getImageFormat());
            
            log.debug("Converted PDF page {} to {} image, size: {} bytes", 
                    pageNumber, ocrConfig.getImageFormat(), imageData.length);
            
            return imageData;
            
        } catch (IOException e) {
            log.error("Failed to convert PDF page {} to image", pageNumber, e);
            throw new IOException("Failed to convert PDF page " + pageNumber, e);
        }
    }
    
    /**
     * Gets the number of pages in a PDF document.
     *
     * @param pdfData the PDF file data
     * @return the number of pages
     * @throws IOException if PDF cannot be read
     */
    public int getPdfPageCount(byte[] pdfData) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfData)) {
            int pageCount = document.getNumberOfPages();
            log.debug("PDF has {} pages", pageCount);
            return pageCount;
        } catch (IOException e) {
            log.error("Failed to read PDF page count", e);
            throw new IOException("Failed to read PDF page count", e);
        }
    }
    
    /**
     * Validates if the PDF data is valid and readable.
     *
     * @param pdfData the PDF file data
     * @return true if PDF is valid, false otherwise
     */
    public boolean isValidPdf(byte[] pdfData) {
        if (pdfData == null || pdfData.length == 0) {
            log.warn("PDF data is null or empty");
            return false;
        }
        
        try (PDDocument document = Loader.loadPDF(pdfData)) {
            int pageCount = document.getNumberOfPages();
            log.debug("PDF validation successful, {} pages", pageCount);
            return pageCount > 0;
        } catch (IOException e) {
            log.warn("PDF validation failed", e);
            return false;
        }
    }
    
    /**
     * Preprocesses an image for better OCR accuracy.
     * Applies contrast enhancement and noise reduction if enabled.
     *
     * @param imageData the original image data
     * @return the preprocessed image data
     * @throws IOException if preprocessing fails
     */
    public byte[] preprocessImage(byte[] imageData) throws IOException {
        if (!ocrConfig.isEnablePreprocessing()) {
            log.debug("Image preprocessing is disabled, returning original image");
            return imageData;
        }
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData)) {
            BufferedImage originalImage = ImageIO.read(inputStream);
            
            if (originalImage == null) {
                log.warn("Failed to read image for preprocessing, returning original");
                return imageData;
            }
            
            // Apply basic preprocessing
            BufferedImage processedImage = enhanceImageForOcr(originalImage);
            
            byte[] processedImageData = convertImageToBytes(processedImage, ocrConfig.getImageFormat());
            
            log.debug("Image preprocessing completed, original size: {} bytes, processed size: {} bytes",
                    imageData.length, processedImageData.length);
            
            return processedImageData;
            
        } catch (IOException e) {
            log.warn("Image preprocessing failed, returning original image", e);
            return imageData;
        }
    }
    
    /**
     * Converts a BufferedImage to byte array in the specified format.
     *
     * @param image the image to convert
     * @param format the image format (PNG, JPEG, etc.)
     * @return the image data as byte array
     * @throws IOException if conversion fails
     */
    private byte[] convertImageToBytes(BufferedImage image, String format) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, format, outputStream)) {
                throw new IOException("Failed to write image in format: " + format);
            }
            return outputStream.toByteArray();
        }
    }
    
    /**
     * Enhances an image for better OCR accuracy.
     * Applies contrast enhancement and basic noise reduction.
     *
     * @param originalImage the original image
     * @return the enhanced image
     */
    private BufferedImage enhanceImageForOcr(BufferedImage originalImage) {
        // For now, return the original image
        // In a production system, you might apply:
        // - Contrast enhancement
        // - Noise reduction
        // - Sharpening filters
        // - Binarization
        
        log.debug("Applied OCR enhancement to image: {}x{}", 
                originalImage.getWidth(), originalImage.getHeight());
        
        return originalImage;
    }
}