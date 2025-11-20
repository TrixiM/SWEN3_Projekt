package fhtw.wien.business;

import fhtw.wien.domain.Document;
import fhtw.wien.exception.InvalidRequestException;
import fhtw.wien.exception.NotFoundException;
import fhtw.wien.exception.PdfProcessingException;
import fhtw.wien.util.PdfValidator;
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
    private static final int DEFAULT_DPI = 96;
    private static final String IMAGE_FORMAT = "PNG";
    
    private final DocumentBusinessLogic documentBusinessLogic;
    
    public PdfRenderingBusinessLogic(DocumentBusinessLogic documentBusinessLogic) {
        this.documentBusinessLogic = documentBusinessLogic;
    }

    public byte[] renderPdfPage(Document document, int pageNumber, float scale) {
        log.debug("Rendering page {} of document {} with scale {}", pageNumber, document.getId(), scale);
        
        // Validate inputs
        if (document == null) {
            throw new InvalidRequestException("Document cannot be null");
        }
        PdfValidator.validateScale(scale);

        try {
            // Get PDF content from MinIO
            byte[] pdfData = documentBusinessLogic.getDocumentContent(document);
            
            try (PDDocument pdfDocument = Loader.loadPDF(pdfData)) {
                int totalPages = pdfDocument.getNumberOfPages();
                log.debug("PDF loaded successfully. Total pages: {}", totalPages);
                
                PdfValidator.validatePageNumber(document, pageNumber, totalPages);

                return renderPageToImage(pdfDocument, pageNumber, scale, document.getId());
            }
            
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
        
        // Validate document
        if (document == null) {
            throw new InvalidRequestException("Document cannot be null");
        }

        try {
            // Get PDF content from MinIO
            byte[] pdfData = documentBusinessLogic.getDocumentContent(document);
            
            try (PDDocument pdfDocument = Loader.loadPDF(pdfData)) {
                int pageCount = pdfDocument.getNumberOfPages();
                log.debug("Document {} has {} pages", document.getId(), pageCount);
                return pageCount;
            }
        } catch (IOException e) {
            log.error("IO error while reading PDF for document {}", document.getId(), e);
            throw new PdfProcessingException("Failed to read PDF", e);
        } catch (Exception e) {
            log.error("Unexpected error while reading PDF for document {}", document.getId(), e);
            throw new PdfProcessingException("Unexpected error while reading PDF", e);
        }
    }
    

    private byte[] renderPageToImage(PDDocument pdfDocument, int pageNumber, float scale, Object documentId) throws IOException {
        PDFRenderer renderer = new PDFRenderer(pdfDocument);
        BufferedImage image = renderer.renderImageWithDPI(pageNumber - 1, DEFAULT_DPI * scale);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, IMAGE_FORMAT, baos);
            byte[] imageBytes = baos.toByteArray();
            
            log.debug("Successfully rendered page {} for document {}, image size: {} bytes", 
                    pageNumber, documentId, imageBytes.length);
            return imageBytes;
        }
    }
}
