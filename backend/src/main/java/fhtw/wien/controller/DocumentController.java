package fhtw.wien.controller;


import fhtw.wien.domain.Document;
import fhtw.wien.dto.CreateDocumentDTO;
import fhtw.wien.dto.DocumentResponse;
import fhtw.wien.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> create(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title
    ) throws IOException {
        // Controller only handles HTTP concerns: file upload, request validation, response mapping
        var doc = new Document(
                title,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                "local-storage",
                "documents/" + System.currentTimeMillis() + "-" + file.getOriginalFilename(),
                "file://" + file.getOriginalFilename(),
                null
        );
        doc.setPdfData(file.getBytes());

        var saved = service.create(doc);
        var body = toResponse(saved);
        return ResponseEntity.created(URI.create("/v1/documents/" + saved.getId())).body(body);
    }

    @PutMapping("{id}") //still in construction
    public ResponseEntity<DocumentResponse> update(
            @PathVariable UUID id,
            @RequestBody Document updateRequest) {

        // get existing document from DB
        Document existing = service.get(id);

        // needs to be refined
        if (updateRequest.getTitle() != null) {
            existing.setTitle(updateRequest.getTitle());
        }
        if (updateRequest.getOriginalFilename() != null) {
            existing.setOriginalFilename(updateRequest.getOriginalFilename());
        }
        if (updateRequest.getContentType() != null) {
            existing.setContentType(updateRequest.getContentType());
        }
        if (updateRequest.getStatus() != null) {
            existing.setStatus(updateRequest.getStatus());
        }
        if (updateRequest.getChecksumSha256() != null) {
            existing.setChecksumSha256(updateRequest.getChecksumSha256());
        }
        if (updateRequest.getPdfData() != null) {
            existing.setPdfData(updateRequest.getPdfData());
        }
        // Endpoint still needs to be refined w optional and non editable fields

        existing.setUpdatedAt(Instant.now()); // update timestamp

        Document updated = service.update(existing);

        DocumentResponse response = new DocumentResponse(
                updated.getId(),
                updated.getTitle(),
                updated.getOriginalFilename(),
                updated.getContentType(),
                updated.getSizeBytes(),
                updated.getBucket(),
                updated.getObjectKey(),
                updated.getStorageUri(),
                updated.getChecksumSha256(),
                updated.getStatus(),
                updated.getVersion(),
                updated.getCreatedAt(),
                updated.getUpdatedAt()
        );

        return ResponseEntity.ok(response);
    }


    @GetMapping
    public List<DocumentResponse> getAll() {
        // Controller only handles HTTP concerns: response mapping
        return service.getAll().stream()
                .map(DocumentController::toResponse)
                .toList();
    }

    @GetMapping("{id}")
    public DocumentResponse get(@PathVariable UUID id) {
        // Controller only handles HTTP concerns: response mapping
        return toResponse(service.get(id));
    }

    @GetMapping("{id}/content")
    public ResponseEntity<byte[]> getContent(@PathVariable UUID id) {
        // Controller only handles HTTP concerns: response headers
        var doc = service.get(id);
        if (doc.getPdfData() == null) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", doc.getOriginalFilename());
        headers.setContentLength(doc.getPdfData().length);

        return new ResponseEntity<>(doc.getPdfData(), headers, HttpStatus.OK);
    }

    @GetMapping("{id}/pages/{pageNumber}")
    public ResponseEntity<byte[]> renderPage(
            @PathVariable UUID id,
            @PathVariable int pageNumber,
            @RequestParam(defaultValue = "1.5") float scale
    ) {
        // Controller only handles HTTP concerns: request params, response headers
        byte[] imageBytes = service.renderPdfPage(id, pageNumber, scale);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setCacheControl("max-age=3600");

        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }

    @GetMapping("{id}/pages/count")
    public ResponseEntity<Integer> getPageCount(@PathVariable UUID id) {
        // Controller only handles HTTP concerns: response wrapping
        int pageCount = service.getPdfPageCount(id);
        return ResponseEntity.ok(pageCount);
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        // Controller only handles HTTP concerns: status code
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

