package fhtw.wien.service;

import fhtw.wien.exception.NotFoundException;
import fhtw.wien.domain.Document;
import fhtw.wien.repo.DocumentRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepo repository;

    public DocumentService(DocumentRepo repository) {
        this.repository = repository;
    }

    @Transactional
    public Document create(Document doc) {
        return repository.save(doc);
    }

    @Transactional(readOnly = true)
    public Document get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Document not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Document> getAll() {
        return repository.findAll();
    }

    @Transactional
    public void delete(UUID id) {
        var existing = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Document not found: " + id));
        repository.deleteById(existing.getId());
    }
}
