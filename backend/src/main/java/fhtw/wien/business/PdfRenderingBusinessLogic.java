package fhtw.wien.business;

import fhtw.wien.domain.Document;
import fhtw.wien.exception.NotFoundException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class PdfRenderingBusinessLogic {

    public byte[] renderPdfPage(Document document, int pageNumber, float scale) {
        if (document.getPdfData() == null) {
            throw new NotFoundException("PDF data not found for document: " + document.getId());
        }

        try (PDDocument pdfDocument = Loader.loadPDF(document.getPdfData())) {
            if (pageNumber < 1 || pageNumber > pdfDocument.getNumberOfPages()) {
                throw new IllegalArgumentException("Invalid page number: " + pageNumber);
            }

            PDFRenderer renderer = new PDFRenderer(pdfDocument);
            BufferedImage image = renderer.renderImageWithDPI(pageNumber - 1, 96 * scale);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to render PDF page", e);
        }
    }

    public int getPdfPageCount(Document document) {
        if (document.getPdfData() == null) {
            throw new NotFoundException("PDF data not found for document: " + document.getId());
        }

        try (PDDocument pdfDocument = Loader.loadPDF(document.getPdfData())) {
            return pdfDocument.getNumberOfPages();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read PDF", e);
        }
    }
}
