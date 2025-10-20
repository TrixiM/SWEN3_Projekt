package fhtw.wien.util;

import fhtw.wien.domain.Document;
import fhtw.wien.exception.InvalidRequestException;
import fhtw.wien.exception.NotFoundException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Utility class for PDF validation operations.
 * Centralizes PDF validation logic to eliminate duplication and ensure consistency.
 */
public final class PdfValidator {
    
    private static final Logger log = LoggerFactory.getLogger(PdfValidator.class);
    
    private PdfValidator() {
        // Utility class
    }
    
    /**
     * Validates that a document has PDF data and that the PDF is readable.
     * 
     * @param document the document to validate
     * @throws NotFoundException if PDF data is null
     * @throws InvalidRequestException if PDF is corrupted or unreadable
     */
    public static void validatePdfData(Document document) {
        if (document.getPdfData() == null) {
            log.error("PDF data is null for document: {}", document.getId());
            throw new NotFoundException("PDF data not found for document: " + document.getId());
        }
        
        // Quick validation by trying to load the PDF
        try (PDDocument pdfDocument = Loader.loadPDF(document.getPdfData())) {
            // Just loading is enough to validate basic structure
            log.debug("PDF validation successful for document: {}", document.getId());
        } catch (IOException e) {
            log.error("PDF validation failed for document: {}", document.getId(), e);
            throw new InvalidRequestException("Invalid PDF format for document: " + document.getId());
        }
    }
    
    /**
     * Validates that a page number is within the valid range for a PDF document.
     * 
     * @param document the document containing the PDF
     * @param pageNumber the page number to validate (1-based)
     * @param totalPages the total number of pages in the PDF
     * @throws InvalidRequestException if page number is out of range
     */
    public static void validatePageNumber(Document document, int pageNumber, int totalPages) {
        if (pageNumber < 1 || pageNumber > totalPages) {
            log.warn("Invalid page number {} requested for document {} (total pages: {})", 
                    pageNumber, document.getId(), totalPages);
            throw new InvalidRequestException(
                String.format("Invalid page number: %d. Document has %d pages", pageNumber, totalPages)
            );
        }
    }
    
    /**
     * Validates the scale parameter for PDF rendering.
     * 
     * @param scale the scale factor
     * @throws InvalidRequestException if scale is not within acceptable range
     */
    public static void validateScale(float scale) {
        if (scale <= 0 || scale > 5.0f) {
            log.warn("Invalid scale factor requested: {}", scale);
            throw new InvalidRequestException("Scale must be between 0.1 and 5.0");
        }
    }
}