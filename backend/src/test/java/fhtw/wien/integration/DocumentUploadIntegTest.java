package fhtw.wien.integration;

import fhtw.wien.domain.Document;
import fhtw.wien.dto.SummaryResultDto;
import fhtw.wien.messaging.DocumentMessageConsumer;
import fhtw.wien.repo.DocumentRepo;
import fhtw.wien.service.IdempotencyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers //to start containers before tests and stop them after tests
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK) //Starts full Spring Application context (loads @Service, @Repository, @Controller and Configurations), uses MockMvc for api requests/responses via variable set as webEnvironment
@AutoConfigureMockMvc //to simulate HTTP requests e.g. .perform(post("/endpoint/path1"))
@Transactional //Wrap each test in a DB transaction and rolls back changes once test is done
class DocumentUploadIntegTest {

     /* Test-only worker implementations.
     * These replace async queue-based workers with synchronous behavior.
     */
     @TestConfiguration
     static class FakeMessagingConfig {

         @Bean
         @Primary
         DocumentMessageConsumer fakeDocumentMessageConsumer(
                 DocumentRepo documentRepo, IdempotencyService idempotencyService
         ) {
             return new DocumentMessageConsumer(documentRepo, idempotencyService) {

                 @Override
                 public void handleSummaryResult(SummaryResultDto summaryResult) {
                     Document doc = documentRepo.findById(summaryResult.documentId()).orElseThrow();
                     doc.setSummary("Generated summary by GenAI Worker");
                     documentRepo.save(doc);
                 }
             };
         }
     }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepo repository;

    @Autowired
    DocumentMessageConsumer consumer;

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("PASSWORD");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    /**
     * Use case:
     * Upload PDF → OCR → AI summary → persisted
     */
    @Test
    void shouldUploadAndProcessPdfDocument() throws Exception {

        MockMultipartFile pdf =
                new MockMultipartFile(
                        "file",
                        "document.pdf",
                        MediaType.APPLICATION_PDF_VALUE,
                        "dummy pdf content".getBytes()
                );

        mockMvc.perform(multipart("/documents")
                        .file(pdf)
                        .param("title", "Test PDF"))
                .andExpect(status().isCreated());

        Document doc = repository.findAll().get(0);

        consumer.handleSummaryResult(
                SummaryResultDto.success(
                        doc.getId(),
                        doc.getTitle(),
                        "Generated summary",
                        5
                )
        );

        assertThat(doc.getTitle()).isEqualTo("document.pdf");
        //assertThat(doc.getContent()).isEqualTo("Extracted OCR text");
        assertThat(doc.getSummary()).isEqualTo("Generated summary");

    }

    /**
     * Use case:
     * Upload non-PDF → OCR not triggered → no summary
     */
    @Test
    void shouldUploadDocumentWithoutOcrIfNotPDF() throws Exception {
        MockMultipartFile txt =
                new MockMultipartFile(
                        "file",
                        "notes.txt",
                        MediaType.TEXT_PLAIN_VALUE,
                        "some text".getBytes()
                );

        mockMvc.perform(multipart("/documents")
                        .file(txt)
                        .param("title", "Text file"))
                .andExpect(status().isCreated());

        Document doc = repository.findAll().get(0);

        assertThat(doc.getTitle()).isEqualTo("notes.txt");
        //assertThat(doc.getOcrText()).isNull();
        assertThat(doc.getSummary()).isNull();
    }

    /**
     * Use case:
     * OCR fails → AI not executed → document still stored
     */
    @Test
    void shouldKeepSummaryContentBlankIfOcrFails() throws Exception {

        MockMultipartFile brokenPdf =
            new MockMultipartFile(
                    "file",
                    "broken.pdf",
                    MediaType.APPLICATION_PDF_VALUE,
                    new byte[0]   // forces OCR failure
            );

        mockMvc.perform(multipart("/documents")
                        .file(brokenPdf)
                        .param("title", "Broken PDF"))
                .andExpect(status().isCreated());

        Document doc = repository.findAll().get(0);

        assertThat(doc.getTitle()).isEqualTo("broken.pdf");
        //assertThat(doc.getOcrText()).isNull();
        assertThat(doc.getSummary()).isNull();
    }

}
