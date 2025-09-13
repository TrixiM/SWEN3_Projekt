package fhtw.wien.controller;


import fhtw.wien.domain.Document;
import fhtw.wien.dto.CreateDocumentDTO;
import fhtw.wien.dto.DocumentResponse;
import fhtw.wien.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/v1/documents")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DocumentResponse> create(@Valid @RequestBody CreateDocumentDTO req) {
        var storageUri = "s3://" + req.bucket() + "/" + req.objectKey(); // derive for now
        var doc = new Document(
                req.title(),
                req.originalFilename(),
                req.contentType(),
                req.sizeBytes(),
                req.bucket(),
                req.objectKey(),
                storageUri,
                req.checksumSha256()
        );
        var saved = service.create(doc);
        var body = toResponse(saved);
        return ResponseEntity.created(URI.create("/v1/documents/" + saved.getId())).body(body);
    }

    @GetMapping("{id}")
    public DocumentResponse get(@PathVariable UUID id) {
        return toResponse(service.get(id));
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    private static DocumentResponse toResponse(Document d) {
        return new DocumentResponse(
                d.getId(),
                d.getTitle(),
                d.getOriginalFilename(),
                d.getContentType(),
                d.getSizeBytes(),
                d.getBucket(),
                d.getObjectKey(),
                d.getStorageUri(),
                d.getChecksumSha256(),
                d.getStatus(),
                d.getVersion(),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }
}

