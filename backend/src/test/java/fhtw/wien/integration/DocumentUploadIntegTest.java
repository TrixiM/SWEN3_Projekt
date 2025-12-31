package fhtw.wien.integration;

import fhtw.wien.repo.DocumentRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers //to start containers before tests and stop them after tests
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK) //Starts full Spring Application context (loads @Service, @Repository, @Controller and Configurations), uses MockMvc for api requests/responses via variable set as webEnvironment
@AutoConfigureMockMvc //to simulate HTTP requests e.g. .perform(post("/endpoint/path1"))
@Transactional //Wrap each test in a DB transaction and rolls back changes once test is done
class DocumentUploadIntegTest {



    @Autowired
    private DocumentRepo repository;

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:16");



    @Test
    void shouldUploadAndProcessPdfDocument() throws Exception {

    }

    @Test
    void shouldUploadDocumentWithoutOcrIfNotPDF() throws Exception {}

    @Test
    void shouldKeepSummaryContentBlankIfOcrFails() throws Exception {}

}
