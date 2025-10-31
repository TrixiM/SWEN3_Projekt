package fhtw.wien.ocrworker.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FileTypeDetector.
 * Tests file type detection using magic numbers and content types.
 */
class FileTypeDetectorTest {
    
    private FileTypeDetector fileTypeDetector;
    
    @BeforeEach
    void setUp() {
        fileTypeDetector = new FileTypeDetector();
    }
    
    @Test
    void detectFileType_WithPdfMagicNumber_ShouldReturnPDF() throws IOException {
        // PDF magic number: %PDF
        byte[] pdfMagicNumber = "%PDF-1.4".getBytes();
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfMagicNumber)) {
            FileTypeDetector.FileType result = fileTypeDetector.detectFileType(inputStream);
            assertEquals(FileTypeDetector.FileType.PDF, result);
        }
    }
    
    @Test
    void detectFileType_WithPngMagicNumber_ShouldReturnImage() throws IOException {
        // PNG magic number
        byte[] pngMagicNumber = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pngMagicNumber)) {
            FileTypeDetector.FileType result = fileTypeDetector.detectFileType(inputStream);
            assertEquals(FileTypeDetector.FileType.IMAGE, result);
        }
    }
    
    @Test
    void detectFileType_WithJpegMagicNumber_ShouldReturnImage() throws IOException {
        // JPEG magic number
        byte[] jpegMagicNumber = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(jpegMagicNumber)) {
            FileTypeDetector.FileType result = fileTypeDetector.detectFileType(inputStream);
            assertEquals(FileTypeDetector.FileType.IMAGE, result);
        }
    }
    
    @Test
    void detectFileType_WithTooFewBytes_ShouldReturnUnsupported() throws IOException {
        byte[] shortData = {0x01, 0x02}; // Too few bytes
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(shortData)) {
            FileTypeDetector.FileType result = fileTypeDetector.detectFileType(inputStream);
            assertEquals(FileTypeDetector.FileType.UNSUPPORTED, result);
        }
    }
    
    @Test
    void detectFileType_WithUnknownMagicNumber_ShouldReturnUnsupported() throws IOException {
        byte[] unknownData = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(unknownData)) {
            FileTypeDetector.FileType result = fileTypeDetector.detectFileType(inputStream);
            assertEquals(FileTypeDetector.FileType.UNSUPPORTED, result);
        }
    }
    
    @Test
    void isSupportedContentType_WithValidPdfType_ShouldReturnTrue() {
        assertTrue(fileTypeDetector.isSupportedContentType("application/pdf"));
    }
    
    @Test
    void isSupportedContentType_WithValidImageType_ShouldReturnTrue() {
        assertTrue(fileTypeDetector.isSupportedContentType("image/jpeg"));
        assertTrue(fileTypeDetector.isSupportedContentType("image/png"));
        assertTrue(fileTypeDetector.isSupportedContentType("image/bmp"));
        assertTrue(fileTypeDetector.isSupportedContentType("image/tiff"));
    }
    
    @Test
    void isSupportedContentType_WithUnsupportedType_ShouldReturnFalse() {
        assertFalse(fileTypeDetector.isSupportedContentType("text/plain"));
        assertFalse(fileTypeDetector.isSupportedContentType("application/msword"));
        assertFalse(fileTypeDetector.isSupportedContentType("video/mp4"));
    }
    
    @Test
    void isSupportedContentType_WithNullType_ShouldReturnFalse() {
        assertFalse(fileTypeDetector.isSupportedContentType(null));
    }
    
    @Test
    void isSupportedContentType_WithCharsetParameter_ShouldIgnoreParameter() {
        assertTrue(fileTypeDetector.isSupportedContentType("application/pdf; charset=utf-8"));
    }
    
    @Test
    void getFileTypeFromContentType_WithPdfType_ShouldReturnPDF() {
        FileTypeDetector.FileType result = fileTypeDetector.getFileTypeFromContentType("application/pdf");
        assertEquals(FileTypeDetector.FileType.PDF, result);
    }
    
    @Test
    void getFileTypeFromContentType_WithImageType_ShouldReturnImage() {
        FileTypeDetector.FileType result = fileTypeDetector.getFileTypeFromContentType("image/jpeg");
        assertEquals(FileTypeDetector.FileType.IMAGE, result);
    }
    
    @Test
    void getFileTypeFromContentType_WithUnsupportedType_ShouldReturnUnsupported() {
        FileTypeDetector.FileType result = fileTypeDetector.getFileTypeFromContentType("text/plain");
        assertEquals(FileTypeDetector.FileType.UNSUPPORTED, result);
    }
    
    @Test
    void isValidFileSize_WithValidSize_ShouldReturnTrue() {
        long validSize = 5 * 1024 * 1024; // 5MB
        long maxSize = 10 * 1024 * 1024; // 10MB
        
        assertTrue(fileTypeDetector.isValidFileSize(validSize, maxSize));
    }
    
    @Test
    void isValidFileSize_WithOversizedFile_ShouldReturnFalse() {
        long oversizeFile = 15 * 1024 * 1024; // 15MB
        long maxSize = 10 * 1024 * 1024; // 10MB
        
        assertFalse(fileTypeDetector.isValidFileSize(oversizeFile, maxSize));
    }
    
    @Test
    void isValidFileSize_WithZeroSize_ShouldReturnFalse() {
        assertFalse(fileTypeDetector.isValidFileSize(0, 1024));
    }
    
    @Test
    void isValidFileSize_WithNegativeSize_ShouldReturnFalse() {
        assertFalse(fileTypeDetector.isValidFileSize(-100, 1024));
    }
    
    @Test
    void getSupportedTypesDescription_ShouldReturnDescription() {
        String description = fileTypeDetector.getSupportedTypesDescription();
        
        assertNotNull(description);
        assertTrue(description.contains("PDF"));
        assertTrue(description.contains("JPEG"));
        assertTrue(description.contains("PNG"));
    }
    
    @Test
    void fileTypeEnum_ShouldHaveCorrectProperties() {
        // Test PDF type
        assertEquals("PDF Document", FileTypeDetector.FileType.PDF.getDescription());
        assertTrue(FileTypeDetector.FileType.PDF.requiresConversion());
        
        // Test IMAGE type
        assertEquals("Image", FileTypeDetector.FileType.IMAGE.getDescription());
        assertFalse(FileTypeDetector.FileType.IMAGE.requiresConversion());
        
        // Test UNSUPPORTED type
        assertEquals("Unsupported", FileTypeDetector.FileType.UNSUPPORTED.getDescription());
        assertFalse(FileTypeDetector.FileType.UNSUPPORTED.requiresConversion());
    }
}