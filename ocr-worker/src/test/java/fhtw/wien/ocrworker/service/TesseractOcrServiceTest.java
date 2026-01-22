package fhtw.wien.ocrworker.service;

import fhtw.wien.ocrworker.config.OcrConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TesseractOcrServiceTest {

    private OcrConfig mockOcrConfig;

    @BeforeEach
    void setUp() {
        mockOcrConfig = new OcrConfig();
        mockOcrConfig.setDefaultLanguage("eng");
        mockOcrConfig.setSupportedLanguages(List.of("eng", "deu", "fra", "spa"));
        mockOcrConfig.setOcrEngineMode(3);
        mockOcrConfig.setPageSegMode(6);
    }

    @Test
    void getAvailableLanguages_ShouldReturnConfiguredLanguages() {
        List<String> expectedLanguages = List.of("eng", "deu", "fra", "spa");
        assertEquals(expectedLanguages, mockOcrConfig.getSupportedLanguages());
    }

    @Test
    void createSimpleTestImage_ShouldGenerateValidImage() throws IOException {
        BufferedImage testImage = createTestImage("Test Text", 200, 50);

        assertNotNull(testImage);
        assertEquals(200, testImage.getWidth());
        assertEquals(50, testImage.getHeight());

        byte[] imageData = imageToBytes(testImage);
        assertTrue(imageData.length > 0);
    }

    @Test
    void ocrResult_ShouldContainRequiredFields() {
        TesseractOcrService.OcrResult result = new TesseractOcrService.OcrResult(
                "Sample text", 85, "eng", 1500);

        assertEquals("Sample text", result.text());
        assertEquals(85, result.confidence());
        assertEquals("eng", result.language());
        assertEquals(1500, result.processingTimeMs());

        String resultString = result.toString();
        assertTrue(resultString.contains("Sample text"));
        assertTrue(resultString.contains("85%"));
        assertTrue(resultString.contains("eng"));
    }

    private BufferedImage createTestImage(String text, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);

        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("Arial", Font.PLAIN, 16));

        FontMetrics metrics = graphics.getFontMetrics();
        int x = (width - metrics.stringWidth(text)) / 2;
        int y = ((height - metrics.getHeight()) / 2) + metrics.getAscent();

        graphics.drawString(text, x, y);
        graphics.dispose();

        return image;
    }

    private byte[] imageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }
}
