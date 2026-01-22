package fhtw.wien.ocrworker.elasticsearch;

import fhtw.wien.ocrworker.dto.OcrResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ElasticsearchService {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchService.class);

    private final DocumentIndexRepository repository;

    public ElasticsearchService(DocumentIndexRepository repository) {
        this.repository = repository;
    }

    public DocumentIndex indexDocument(OcrResultDto ocrResult) {
        log.debug("Indexing document {} into Elasticsearch", ocrResult.documentId());

        try {
            return repository.save(DocumentIndex.from(ocrResult));
        } catch (Exception e) {
            log.error("Failed to index document {}", ocrResult.documentId(), e);
            throw new RuntimeException("Failed to index document", e);
        }
    }

    public void deleteDocument(UUID documentId) {
        log.debug("Deleting document {} from Elasticsearch", documentId);
        try {
            repository.deleteById(documentId.toString());
        } catch (Exception e) {
            log.error("Failed to delete document {}", documentId, e);
            throw new RuntimeException("Failed to delete document", e);
        }
    }
}
