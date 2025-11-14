package fhtw.wien.business;

import fhtw.wien.domain.Document;
import fhtw.wien.exception.InvalidRequestException;
import fhtw.wien.exception.NotFoundException;
import fhtw.wien.exception.PdfProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PdfRenderingBusinessLogic Unit Tests")
class PdfRenderingBusinessLogicTest {

    @Mock
    private DocumentBusinessLogic documentBusinessLogic;

    @InjectMocks
    private PdfRenderingBusinessLogic pdfRenderingLogic;

    private Document testDocument;
    private UUID testId;
    private byte[] validPdfData;

    @BeforeEach
    void setUp() throws IOException {
        testId = UUID.randomUUID();
        
        // Load a minimal valid PDF for testing
        validPdfData = loadTestPdfFromResources();
        
        testDocument = new Document();
        testDocument.setId(testId);
        testDocument.setTitle("Test PDF Document");
        testDocument.setOriginalFilename("test.pdf");
        testDocument.setContentType("application/pdf");
        testDocument.setSizeBytes(validPdfData.length);
        testDocument.setObjectKey("documents/test.pdf");
    }

    /**
     * Loads a minimal valid PDF from test resources.
     * If resources are not available, creates a minimal valid PDF programmatically.
     */
    private byte[] loadTestPdfFromResources() throws IOException {
        // Try to load from resources first
        try (InputStream is = getClass().getResourceAsStream("/test-pdf/sample.pdf")) {
            if (is != null) {
                return is.readAllBytes();
            }
        } catch (Exception e) {
            // Fall through to create minimal PDF
        }
        
        // Create a minimal valid PDF programmatically
        return createMinimalValidPdf();
    }

    /**
     * Creates a minimal valid PDF document for testing.
     * This is a bare-bones PDF with the minimum structure required by PDFBox.
     */
    private byte[] createMinimalValidPdf() {
        // Minimal PDF structure (simplified)
        String pdfContent = "%PDF-1.4\n" +
                "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n" +
                "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n" +
                "3 0 obj\n<< /Type /Page /Parent 2 0 R /Resources 4 0 R /MediaBox [0 0 612 792] >>\nendobj\n" +
                "4 0 obj\n<< >>\nendobj\n" +
                "xref\n0 5\n" +
                "0000000000 65535 f\n" +
                "0000000009 00000 n\n" +
                "0000000058 00000 n\n" +
                "0000000115 00000 n\n" +
                "0000000214 00000 n\n" +
                "trailer\n<< /Size 5 /Root 1 0 R >>\n" +
                "startxref\n225\n%%EOF";
        
        return pdfContent.getBytes();
    }

    // ========== RENDER PDF PAGE TESTS ==========

    @Test
    @DisplayName("Should render PDF page successfully with valid inputs")
    void renderPdfPage_ValidInputs_Success() {
        // Given
        int pageNumber = 1;
        float scale = 1.0f;
        
        when(documentBusinessLogic.getDocumentContent(testDocument))
                .thenReturn(validPdfData);

        // When
        byte[] result = pdfRenderingLogic.renderPdfPage(testDocument, pageNumber, scale);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result.length).isGreaterThan(0);
        
        verify(documentBusinessLogic).getDocumentContent(testDocument);
    }

    @Test
    @DisplayName("Should render PDF page with custom scale")
    void renderPdfPage_CustomScale_Success() {
        // Given
        int pageNumber = 1;
        float scale = 2.0f; // 2x zoom
        
        when(documentBusinessLogic.getDocumentContent(testDocument))
                .thenReturn(validPdfData);

        // When
        byte[] result = pdfRenderingLogic.renderPdfPage(testDocument, pageNumber, scale);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        
        verify(documentBusinessLogic).getDocumentContent(testDocument);
    }

    @Test
    @DisplayName("Should reject null document")
    void renderPdfPage_NullDocument_ThrowsException() {
        // When / Then
        assertThatThrownBy(() -> pdfRenderingLogic.renderPdfPage(null, 1, 1.0f))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Document cannot be null");
        
        verifyNoInteractions(documentBusinessLogic);
    }

    @Test
    @DisplayName("Should reject invalid scale - zero")
    void renderPdfPage_ZeroScale_ThrowsException() {
        // When / Then
        assertThatThrownBy(() -> pdfRenderingLogic.renderPdfPage(testDocument, 1, 0.0f))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Scale must be between");
    }

    @Test
    @DisplayName("Should reject invalid scale - negative")
    void renderPdfPage_NegativeScale_ThrowsException() {
        // When / Then
        assertThatThrownBy(() -> pdfRenderingLogic.renderPdfPage(testDocument, 1, -1.0f))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Scale must be between");
    }

    @Test
    @DisplayName("Should reject invalid scale - too large")
    void renderPdfPage_TooLargeScale_ThrowsException() {
        // When / Then
        assertThatThrownBy(() -> pdfRenderingLogic.renderPdfPage(testDocument, 1, 10.0f))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Scale must be between");
    }

    @Test
    @DisplayName("Should reject invalid page number - zero")
    void renderPdfPage_PageNumberZero_ThrowsException() {
        // Given
        when(documentBusinessLogic.getDocumentContent(testDocument))
                .thenReturn(validPdfData);

        // When / Then
        assertThatThrownBy(() -> pdfRenderingLogic.renderPdfPage(testDocument, 0, 1.0f))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Invalid page number");
    }

    @Test
    @DisplayName("Should reject invalid page number - exceeds total pages")
    void renderPdfPage_PageNumberTooLarge_ThrowsException() {
        // Given
        when(documentBusinessLogic.getDocumentContent(testDocument))
                .thenReturn(validPdfData);

        // When / Then
        assertThatThrownBy(() -> pdfRenderingLogic.renderPdfPage(testDocument, 999, 1.0f))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Invalid page number");
    }

    @Test
    @DisplayName("Should handle corrupted PDF data")
    void renderPdfPage_CorruptedPdf_ThrowsPdfProcessingException() {
        // Given
        byte[] corruptedPdf = "Not a valid PDF".getBytes();
        
        when(documentBusinessLogic.getDocumentContent(testDocument))
                .thenReturn(corruptedPdf);

        // When / Then
        assertThatThrownBy(() -> pdfRenderingLogic.renderPdfPage(testDocument, 1, 1.0f))
                .isInstanceOf(PdfProcessingException.class)
                .hasMessageContaining("Failed to render PDF page");
    }

    @Test
    @DisplayName("Should handle MinIO retrieval failure")
    void renderPdfPage_MinIOError_ThrowsException() {
        // Given
        when(documentBusinessLogic.getDocumentContent(testDocument))
                .thenThrow(new RuntimeException("MinIO connection error"));

        // When / Then
        assertThatThrownBy(() -> pdfRenderingLogic.renderPdfPage(testDocument, 1, 1.0f))
                .isInstanceOf(PdfProcessingException.class);
    }

    // ========== GET PAGE COUNT TESTS ==========

    @Test
    @DisplayName("Should get PDF page count successfully")
    void getPdfPageCount_ValidDocument_ReturnsCount() {
        // Given
        when(documentBusinessLogic.getDocumentContent(testDocument))
                .thenReturn(validPdfData);

        // When
        int result = pdfRenderingLogic.getPdfPageCount(testDocument);

        // Then
        assertThat(result).isGreaterThan(0);
        assertThat(result).isEqualTo(1); // Our minimal PDF has 1 page
        
        verify(documentBusinessLogic).getDocumentContent(testDocument);
    }

    @Test
    @DisplayName("Should reject null document when getting page count")
    void getPdfPageCount_NullDocument_ThrowsException() {
        // When / Then
        assertThatThrownBy(() -> pdfRenderingLogic.getPdfPageCount(null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Document cannot be null");
        
        verifyNoInteractions(documentBusinessLogic);
    }

    @Test
    @DisplayName("Should handle corrupted PDF when getting page count")
    void getPdfPageCount_CorruptedPdf_ThrowsPdfProcessingException() {
        // Given
        byte[] corruptedPdf = "Invalid PDF content".getBytes();
        
        when(documentBusinessLogic.getDocumentContent(testDocument))
                .thenReturn(corruptedPdf);

        // When / Then
        assertThatThrownBy(() -> pdfRenderingLogic.getPdfPageCount(testDocument))
                .isInstanceOf(PdfProcessingException.class)
                .hasMessageContaining("Failed to read PDF");
    }

    @Test
    @DisplayName("Should handle MinIO retrieval failure when getting page count")
    void getPdfPageCount_MinIOError_ThrowsException() {
        // Given
        when(documentBusinessLogic.getDocumentContent(testDocument))
                .thenThrow(new RuntimeException("Storage service unavailable"));

        // When / Then
        assertThatThrownBy(() -> pdfRenderingLogic.getPdfPageCount(testDocument))
                .isInstanceOf(PdfProcessingException.class);
    }

    @Test
    @DisplayName("Should handle empty PDF bytes")
    void getPdfPageCount_EmptyPdf_ThrowsPdfProcessingException() {
        // Given
        byte[] emptyPdf = new byte[0];
        
        when(documentBusinessLogic.getDocumentContent(testDocument))
                .thenReturn(emptyPdf);

        // When / Then
        assertThatThrownBy(() -> pdfRenderingLogic.getPdfPageCount(testDocument))
                .isInstanceOf(PdfProcessingException.class);
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    @DisplayName("Should render first page with minimum scale")
    void renderPdfPage_MinimumScale_Success() {
        // Given
        float minimumScale = 0.1f;
        
        when(documentBusinessLogic.getDocumentContent(testDocument))
                .thenReturn(validPdfData);

        // When
        byte[] result = pdfRenderingLogic.renderPdfPage(testDocument, 1, minimumScale);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Should render page with maximum allowed scale")
    void renderPdfPage_MaximumScale_Success() {
        // Given
        float maximumScale = 5.0f;
        
        when(documentBusinessLogic.getDocumentContent(testDocument))
                .thenReturn(validPdfData);

        // When
        byte[] result = pdfRenderingLogic.renderPdfPage(testDocument, 1, maximumScale);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle document with missing object key")
    void renderPdfPage_MissingObjectKey_ThrowsException() {
        // Given
        testDocument.setObjectKey(null);
        
        when(documentBusinessLogic.getDocumentContent(testDocument))
                .thenThrow(new NotFoundException("Document content not available"));

        // When / Then
        assertThatThrownBy(() -> pdfRenderingLogic.renderPdfPage(testDocument, 1, 1.0f))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Should properly handle NotFoundException from business logic")
    void renderPdfPage_DocumentNotFound_RethrowsNotFoundException() {
        // Given
        when(documentBusinessLogic.getDocumentContent(testDocument))
                .thenThrow(new NotFoundException("Document not found in storage"));

        // When / Then
        assertThatThrownBy(() -> pdfRenderingLogic.renderPdfPage(testDocument, 1, 1.0f))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Document not found");
    }

    @Test
    @DisplayName("Should properly handle InvalidRequestException from business logic")
    void renderPdfPage_InvalidRequest_RethrowsInvalidRequestException() {
        // Given
        when(documentBusinessLogic.getDocumentContent(testDocument))
                .thenThrow(new InvalidRequestException("Invalid document state"));

        // When / Then
        assertThatThrownBy(() -> pdfRenderingLogic.renderPdfPage(testDocument, 1, 1.0f))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Invalid document state");
    }
}
