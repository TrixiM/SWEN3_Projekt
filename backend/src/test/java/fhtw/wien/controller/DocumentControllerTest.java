package fhtw.wien.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fhtw.wien.domain.Document;
import fhtw.wien.domain.DocumentStatus;
import fhtw.wien.dto.DocumentResponse;
import fhtw.wien.exception.InvalidRequestException;
import fhtw.wien.exception.NotFoundException;
import fhtw.wien.service.DocumentService;
import fhtw.wien.util.DocumentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DocumentService documentService;

    private Document testDocument;
    private DocumentResponse testDocumentResponse;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testDocument = new Document(
                "Test Document",
                "test.pdf",
                "application/pdf",
                1024L,
                "test-bucket",
                "object-key",
                "s3://test-bucket/object-key",
                "checksum123"
        );
        testDocument.setId(testId);
        testDocument.setStatus(DocumentStatus.UPLOADED);

        testDocumentResponse = new DocumentResponse(
                testId,
                "Test Document",
                "test.pdf",
                "application/pdf",
                1024L,
                "test-bucket",
                "object-key",
                "s3://test-bucket/object-key",
                "checksum123",
                DocumentStatus.UPLOADED,
                List.of("tag1", "tag2"),
                null,
                1,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    void create_WithValidFile_ShouldReturnCreated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );

        when(documentService.create(any(Document.class), any(byte[].class))).thenReturn(testDocument);

        mockMvc.perform(multipart("/v1/documents")
                        .file(file)
                        .param("title", "Test Document")
                        .param("tags", "tag1", "tag2"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(testId.toString()))
                .andExpect(jsonPath("$.title").value("Test Document"))
                .andExpect(jsonPath("$.originalFilename").value("test.pdf"));

        verify(documentService, times(1)).create(any(Document.class), any(byte[].class));
    }

    @Test
    void create_WithEmptyFile_ShouldReturnBadRequest() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                new byte[0]
        );

        mockMvc.perform(multipart("/v1/documents")
                        .file(emptyFile)
                        .param("title", "Test Document"))
                .andExpect(status().isBadRequest());

        verify(documentService, never()).create(any(Document.class), any(byte[].class));
    }

    @Test
    void create_WithNoTitle_ShouldDefaultToFilename() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );

        when(documentService.create(any(Document.class), any(byte[].class))).thenReturn(testDocument);

        mockMvc.perform(multipart("/v1/documents")
                        .file(file))
                .andExpect(status().isCreated());

        verify(documentService, times(1)).create(any(Document.class), any(byte[].class));
    }

    @Test
    void getAll_ShouldReturnDocumentList() throws Exception {
        when(documentService.getAll()).thenReturn(List.of(testDocument));

        mockMvc.perform(get("/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(testId.toString()))
                .andExpect(jsonPath("$[0].title").value("Test Document"));

        verify(documentService, times(1)).getAll();
    }

    @Test
    void get_WithValidId_ShouldReturnDocument() throws Exception {
        when(documentService.get(testId)).thenReturn(testDocument);

        mockMvc.perform(get("/v1/documents/{id}", testId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testId.toString()))
                .andExpect(jsonPath("$.title").value("Test Document"));

        verify(documentService, times(1)).get(testId);
    }

    @Test
    void get_WithInvalidId_ShouldReturnNotFound() throws Exception {
        UUID invalidId = UUID.randomUUID();
        when(documentService.get(invalidId)).thenThrow(new NotFoundException("Document not found"));

        mockMvc.perform(get("/v1/documents/{id}", invalidId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(documentService, times(1)).get(invalidId);
    }

    @Test
    void getContent_WithValidId_ShouldReturnPdfContent() throws Exception {
        byte[] pdfData = "PDF content".getBytes();

        when(documentService.get(testId)).thenReturn(testDocument);
        when(documentService.getDocumentContent(testDocument)).thenReturn(pdfData);

        mockMvc.perform(get("/v1/documents/{id}/content", testId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfData));

        verify(documentService, times(1)).get(testId);
        verify(documentService, times(1)).getDocumentContent(testDocument);
    }

    @Test
    void renderPage_WithValidParameters_ShouldReturnImage() throws Exception {
        byte[] imageData = "PNG image data".getBytes();
        int pageNumber = 1;
        float scale = 1.5f;

        when(documentService.renderPdfPage(testId, pageNumber, scale)).thenReturn(imageData);

        mockMvc.perform(get("/v1/documents/{id}/pages/{pageNumber}", testId, pageNumber)
                        .param("scale", String.valueOf(scale)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(imageData));

        verify(documentService, times(1)).renderPdfPage(testId, pageNumber, scale);
    }

    @Test
    void renderPage_WithInvalidPageNumber_ShouldReturnBadRequest() throws Exception {
        int invalidPageNumber = 0;

        mockMvc.perform(get("/v1/documents/{id}/pages/{pageNumber}", testId, invalidPageNumber))
                .andExpect(status().isBadRequest());

        verify(documentService, never()).renderPdfPage(any(), anyInt(), anyFloat());
    }

    @Test
    void getPageCount_WithValidId_ShouldReturnCount() throws Exception {
        int pageCount = 5;

        when(documentService.getPdfPageCount(testId)).thenReturn(pageCount);

        mockMvc.perform(get("/v1/documents/{id}/pages/count", testId))
                .andExpect(status().isOk())
                .andExpect(content().string(String.valueOf(pageCount)));

        verify(documentService, times(1)).getPdfPageCount(testId);
    }

    @Test
    void delete_WithValidId_ShouldReturnNoContent() throws Exception {
        doNothing().when(documentService).delete(testId);

        mockMvc.perform(delete("/v1/documents/{id}", testId))
                .andExpect(status().isNoContent());

        verify(documentService, times(1)).delete(testId);
    }

    @Test
    void delete_WithInvalidId_ShouldReturnNotFound() throws Exception {
        UUID invalidId = UUID.randomUUID();
        doThrow(new NotFoundException("Document not found")).when(documentService).delete(invalidId);

        mockMvc.perform(delete("/v1/documents/{id}", invalidId))
                .andExpect(status().isNotFound());

        verify(documentService, times(1)).delete(invalidId);
    }

    @Test
    void update_WithValidData_ShouldReturnUpdatedDocument() throws Exception {
        Document updateRequest = new Document(
                "Updated Title",
                "test.pdf",
                "application/pdf",
                1024L,
                "test-bucket",
                "object-key",
                "s3://test-bucket/object-key",
                "checksum123"
        );

        when(documentService.get(testId)).thenReturn(testDocument);
        when(documentService.update(any(Document.class))).thenReturn(testDocument);

        mockMvc.perform(put("/v1/documents/{id}", testId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testId.toString()));

        verify(documentService, times(1)).get(testId);
        verify(documentService, times(1)).update(any(Document.class));
    }
}
