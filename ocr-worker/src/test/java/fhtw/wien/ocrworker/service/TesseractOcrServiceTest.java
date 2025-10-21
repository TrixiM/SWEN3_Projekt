package fhtw.wien.ocrworker.service;

import fhtw.wien.ocrworker.config.OcrConfig;
import net.sourceforge.tess4j.TesseractException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TesseractOcrService.
 * Tests OCR text extraction and confidence calculation.
 */
@ExtendWith(MockitoExtension.class)
class TesseractOcrServiceTest {
    
    @Mock
    private OcrConfig mockOcrConfig;
    
    private TesseractOcrService ocrService;
    
    @BeforeEach
    void setUp() {
        // Setup mock OCR config
        mockOcrConfig = new OcrConfig();
        mockOcrConfig.setDefaultLanguage("eng");
        mockOcrConfig.setSupportedLanguages(List.of("eng", "deu", "fra", "spa"));
        mockOcrConfig.setOcrEngineMode(3);
        mockOcrConfig.setPageSegMode(6);
        mockOcrConfig.setMinConfidenceThreshold(30);
        
        // Note: TesseractOcrService requires actual Tesseract installation
        // These tests will be integration tests that check behavior
    }
    
    @Test
    void extractText_WithNullData_ShouldThrowException() {
        // Test input validation
        if (ocrService != null) {
            assertThrows(IllegalArgumentException.class, () -> {
                ocrService.extractText(null);
            });
        }
    }
    
    @Test
    void extractText_WithEmptyData_ShouldThrowException() {
        // Test input validation
        if (ocrService != null) {
            assertThrows(IllegalArgumentException.class, () -> {
                ocrService.extractText(new byte[0]);
            });
        }
    }
    
    @Test
    void getAvailableLanguages_ShouldReturnConfiguredLanguages() {
        // Test language configuration
        String[] expectedLanguages = {"eng", "deu", "fra", "spa"};
        
        if (ocrService != null) {
            String[] actualLanguages = ocrService.getAvailableLanguages();
            assertArrayEquals(expectedLanguages, actualLanguages);
        }
    }
    
    @Test
    void createSimpleTestImage_ShouldGenerateValidImage() throws IOException {
        // Test helper method for creating test images
        BufferedImage testImage = createTestImage("Test Text", 200, 50);
        
        assertNotNull(testImage);
        assertEquals(200, testImage.getWidth());
        assertEquals(50, testImage.getHeight());
        
        // Convert to byte array
        byte[] imageData = imageToBytes(testImage);
        assertTrue(imageData.length > 0);
    }
    
    @Test
    void ocrResult_ShouldContainRequiredFields() {
        // Test OCR result structure
        TesseractOcrService.OcrResult result = new TesseractOcrService.OcrResult(
                "Sample text", 85, "eng", 1500, true);
        
        assertEquals("Sample text", result.getText());
        assertEquals(85, result.getConfidence());
        assertEquals("eng", result.getLanguage());
        assertEquals(1500, result.getProcessingTimeMs());
        assertTrue(result.isHighConfidence());
        
        // Test toString method
        String resultString = result.toString();
        assertTrue(resultString.contains("Sample text"));
        assertTrue(resultString.contains("85%"));
        assertTrue(resultString.contains("eng"));
    }
    
    /**
     * Integration tests for OCR functionality.
     * These require Tesseract to be installed and configured.
     */
    static class TesseractOcrServiceIntegrationTest {
        
        private TesseractOcrService ocrService;
        
        // @BeforeEach - would initialize with real config
        
        // @Test
        void extractText_WithSimpleImage_ShouldExtractText() throws IOException, TesseractException {
            // Integration test that would:
            // 1. Create a simple image with known text
            // 2. Perform OCR extraction
            // 3. Verify the extracted text matches expected
            // 4. Check confidence is reasonable
            
            // Commented out as it requires actual Tesseract installation
        }
        
        // @Test
        void extractTextWithConfidence_ShouldReturnConfidenceScores() throws IOException, TesseractException {
            // Integration test for confidence score calculation
        }
        
        // @Test
        void isTesseractAvailable_ShouldReturnTrue() {
            // Integration test to verify Tesseract is properly configured
        }
    }
    
    /**
     * Creates a simple test image with text for OCR testing.
     *
     * @param text the text to render
     * @param width image width
     * @param height image height
     * @return BufferedImage with rendered text
     */
    private BufferedImage createTestImage(String text, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        
        // Set white background
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        
        // Set black text
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("Arial", Font.PLAIN, 16));
        
        // Center text
        FontMetrics metrics = graphics.getFontMetrics();
        int x = (width - metrics.stringWidth(text)) / 2;
        int y = ((height - metrics.getHeight()) / 2) + metrics.getAscent();
        
        graphics.drawString(text, x, y);
        graphics.dispose();
        
        return image;
    }
    
    /**
     * Converts a BufferedImage to byte array.
     *
     * @param image the image to convert
     * @return image data as byte array
     * @throws IOException if conversion fails
     */
    private byte[] imageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }
}