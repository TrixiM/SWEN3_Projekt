package fhtw.wien.integration;

import fhtw.wien.domain.Document;
import fhtw.wien.domain.DocumentStatus;
import fhtw.wien.dto.DocumentResponse;
import fhtw.wien.repo.DocumentRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the complete document upload workflow.
 * Uses TestContainers for PostgreSQL and H2 in-memory database.
 * 
 * Note: MinIO is mocked in this test. For full integration testing with MinIO,
 * add a MinIO TestContainer.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class DocumentUploadIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DocumentRepo documentRepo;

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @AfterEach
    void cleanup() {
        documentRepo.deleteAll();
    }

    @Test
    void fullDocumentUploadWorkflow_ShouldSucceed() {
        // Given: A file to upload
        String baseUrl = "http://localhost:" + port + "/v1/documents";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", createTestFileResource());
        body.add("title", "Integration Test Document");
        body.add("tags", "integration");
        body.add("tags", "test");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // When: Upload document
        ResponseEntity<DocumentResponse> uploadResponse = restTemplate.postForEntity(
                baseUrl,
                requestEntity,
                DocumentResponse.class
        );

        // Then: Should return 201 Created
        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(uploadResponse.getHeaders().getLocation()).isNotNull();

        DocumentResponse createdDocument = uploadResponse.getBody();
        assertNotNull(createdDocument);
        assertNotNull(createdDocument.id());
        assertEquals("Integration Test Document", createdDocument.title());
        assertThat(createdDocument.tags()).containsExactlyInAnyOrder("integration", "test");

        // Verify document is saved in database
        Document savedDocument = documentRepo.findById(createdDocument.id()).orElse(null);
        assertNotNull(savedDocument);
        assertEquals("Integration Test Document", savedDocument.getTitle());
        assertNotNull(savedDocument.getCreatedAt());
        assertNotNull(savedDocument.getUpdatedAt());
    }

    @Test
    void getAllDocuments_AfterUpload_ShouldReturnAllDocuments() {
        // Given: Two uploaded documents
        uploadTestDocument("Document 1");
        uploadTestDocument("Document 2");

        // When: Get all documents
        String baseUrl = "http://localhost:" + port + "/v1/documents";
        ResponseEntity<DocumentResponse[]> response = restTemplate.getForEntity(
                baseUrl,
                DocumentResponse[].class
        );

        // Then: Should return both documents
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentResponse[] documents = response.getBody();
        assertNotNull(documents);
        assertThat(documents.length).isGreaterThanOrEqualTo(2);
    }

    @Test
    void getDocumentById_WithValidId_ShouldReturnDocument() {
        // Given: An uploaded document
        UUID documentId = uploadTestDocument("Test Document").id();

        // When: Get document by ID
        String url = "http://localhost:" + port + "/v1/documents/" + documentId;
        ResponseEntity<DocumentResponse> response = restTemplate.getForEntity(
                url,
                DocumentResponse.class
        );

        // Then: Should return the document
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentResponse document = response.getBody();
        assertNotNull(document);
        assertEquals(documentId, document.id());
        assertEquals("Test Document", document.title());
    }

    @Test
    void getDocumentById_WithInvalidId_ShouldReturn404() {
        // Given: A non-existent document ID
        UUID invalidId = UUID.randomUUID();

        // When: Get document by ID
        String url = "http://localhost:" + port + "/v1/documents/" + invalidId;
        ResponseEntity<String> response = restTemplate.getForEntity(
                url,
                String.class
        );

        // Then: Should return 404
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteDocument_WithValidId_ShouldRemoveDocument() {
        // Given: An uploaded document
        UUID documentId = uploadTestDocument("Document to Delete").id();

        // Verify it exists
        assertThat(documentRepo.findById(documentId)).isPresent();

        // When: Delete the document
        String url = "http://localhost:" + port + "/v1/documents/" + documentId;
        restTemplate.delete(url);

        // Then: Document should be removed from database
        assertThat(documentRepo.findById(documentId)).isEmpty();
    }

    @Test
    void uploadDocument_WithoutFile_ShouldReturn400() {
        // Given: Request without file
        String baseUrl = "http://localhost:" + port + "/v1/documents";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("title", "Document Without File");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // When: Try to upload
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl,
                requestEntity,
                String.class
        );

        // Then: Should return 400 Bad Request
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void uploadDocument_WithEmptyTitle_ShouldReturn400() {
        // Given: Request with empty title
        String baseUrl = "http://localhost:" + port + "/v1/documents";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", createTestFileResource());
        body.add("title", "");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // When: Try to upload
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl,
                requestEntity,
                String.class
        );

        // Then: Should return 400 Bad Request
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // Helper methods

    private DocumentResponse uploadTestDocument(String title) {
        String baseUrl = "http://localhost:" + port + "/v1/documents";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", createTestFileResource());
        body.add("title", title);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<DocumentResponse> response = restTemplate.postForEntity(
                baseUrl,
                requestEntity,
                DocumentResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private org.springframework.core.io.Resource createTestFileResource() {
        // Create a simple test file resource
        byte[] testData = "Test PDF content for integration testing".getBytes();
        return new org.springframework.core.io.ByteArrayResource(testData) {
            @Override
            public String getFilename() {
                return "test-document.pdf";
            }
        };
    }
}
