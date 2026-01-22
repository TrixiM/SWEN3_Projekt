package fhtw.wien.service;

import fhtw.wien.domain.Document;

import java.util.UUID;

public interface DocumentMessageService {//for sake of testing
    void publishDocumentCreated(Document document);
    void deleteDocument(UUID id);
}
