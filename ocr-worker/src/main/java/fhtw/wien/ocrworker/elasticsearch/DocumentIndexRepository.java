package fhtw.wien.ocrworker.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentIndexRepository extends ElasticsearchRepository<DocumentIndex, String> {

    DocumentIndex findByDocumentId(UUID documentId);
}
