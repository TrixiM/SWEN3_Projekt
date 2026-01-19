package fhtw.wien.service;

import fhtw.wien.domain.Document;

import java.util.UUID;

public interface DocumentMessageService {
    void publishDocumentCreated(Document document);
    void deleteDocument(UUID id);
}
