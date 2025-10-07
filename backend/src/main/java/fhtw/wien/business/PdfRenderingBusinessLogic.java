package fhtw.wien.business;

import fhtw.wien.domain.Document;
import fhtw.wien.exception.InvalidRequestException;
import fhtw.wien.exception.NotFoundException;
import fhtw.wien.exception.PdfProcessingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class PdfRenderingBusinessLogic {

    private static final Logger log = LoggerFactory.getLogger(PdfRenderingBusinessLogic.class);

    public byte[] renderPdfPage(Document document, int pageNumber, float scale) {
        log.debug("Rendering page {} of document {} with scale {}", pageNumber, document.getId(), scale);
        
        if (document.getPdfData() == null) {
            log.error("PDF data is null for document: {}", document.getId());
            throw new NotFoundException("PDF data not found for document: " + document.getId());
        }

        try (PDDocument pdfDocument = Loader.loadPDF(document.getPdfData())) {
            int totalPages = pdfDocument.getNumberOfPages();
            log.debug("PDF loaded successfully. Total pages: {}", totalPages);
            
            if (pageNumber < 1 || pageNumber > totalPages) {
                log.warn("Invalid page number {} requested for document {} (total pages: {})", 
                        pageNumber, document.getId(), totalPages);
                throw new InvalidRequestException("Invalid page number: " + pageNumber + 
                        ". Document has " + totalPages + " pages");
            }

            PDFRenderer renderer = new PDFRenderer(pdfDocument);
            BufferedImage image = renderer.renderImageWithDPI(pageNumber - 1, 96 * scale);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();
            
            log.debug("Successfully rendered page {} for document {}, image size: {} bytes", 
                    pageNumber, document.getId(), imageBytes.length);
            return imageBytes;
        } catch (InvalidRequestException | NotFoundException e) {
            throw e; // Re-throw validation and not-found exceptions as-is
        } catch (IOException e) {
            log.error("IO error while rendering page {} of document {}", pageNumber, document.getId(), e);
            throw new PdfProcessingException("Failed to render PDF page", e);
        } catch (Exception e) {
            log.error("Unexpected error while rendering page {} of document {}", pageNumber, document.getId(), e);
            throw new PdfProcessingException("Unexpected error during PDF rendering", e);
        }
    }

    public int getPdfPageCount(Document document) {
        log.debug("Getting page count for document: {}", document.getId());
        
        if (document.getPdfData() == null) {
            log.error("PDF data is null for document: {}", document.getId());
            throw new NotFoundException("PDF data not found for document: " + document.getId());
        }

        try (PDDocument pdfDocument = Loader.loadPDF(document.getPdfData())) {
            int pageCount = pdfDocument.getNumberOfPages();
            log.debug("Document {} has {} pages", document.getId(), pageCount);
            return pageCount;
        } catch (IOException e) {
            log.error("IO error while reading PDF for document {}", document.getId(), e);
            throw new PdfProcessingException("Failed to read PDF", e);
        } catch (Exception e) {
            log.error("Unexpected error while reading PDF for document {}", document.getId(), e);
            throw new PdfProcessingException("Unexpected error while reading PDF", e);
        }
    }
}
