package fhtw.wien.business;

import fhtw.wien.domain.Document;
import fhtw.wien.exception.NotFoundException;
import fhtw.wien.repo.DocumentRepo;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class DocumentBusinessLogic {

    private final DocumentRepo repository;

    public DocumentBusinessLogic(DocumentRepo repository) {
        this.repository = repository;
    }

    @Transactional
    public Document createOrUpdateDocument(Document doc) {
        return repository.save(doc);
    }

    @Transactional(readOnly = true)
    public Document getDocumentById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Document not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Document> getAllDocuments() {
        return repository.findAll();
    }

    @Transactional
    public void deleteDocument(UUID id) {
        var existing = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Document not found: " + id));
        repository.deleteById(existing.getId());
    }
}
