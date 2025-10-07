# Quick Reference: Exception Handling & Logging

## When to Use Which Exception

### Client-Side Errors (4xx)

**`NotFoundException`** - HTTP 404
```java
throw new NotFoundException("Document not found: " + id);
```
Use when: Resource doesn't exist in database

**`InvalidRequestException`** - HTTP 400  
```java
throw new InvalidRequestException("File cannot be empty");
throw new InvalidRequestException("Page number must be greater than 0");
```
Use when: Invalid input parameters, validation failures

### Server-Side Errors (5xx)

**`BusinessLogicException`** - HTTP 422
```java
throw new BusinessLogicException("Business rule violation", cause);
```
Use when: Business logic rules are violated

**`PdfProcessingException`** - HTTP 500
```java
throw new PdfProcessingException("Failed to render PDF page", e);
```
Use when: PDF rendering/processing fails

**`ServiceException`** - HTTP 500
```java
throw new ServiceException("Failed to create document", e);
```
Use when: Service orchestration fails

**`MessagingException`** - HTTP 503
```java
throw new MessagingException("Failed to publish event", e);
```
Use when: RabbitMQ operations fail

**`DataAccessException`** - HTTP 500
```java
throw new DataAccessException("Failed to save document", e);
```
Use when: Database operations fail

## Logging Patterns by Level

### INFO - Business Operations
```java
log.info("Creating document with title: {}", doc.getTitle());
log.info("Document created successfully with ID: {}", saved.getId());
log.info("POST /v1/documents - Creating document");
```
Use for: Entry/exit of business operations, successful completions

### DEBUG - Detailed Information
```java
log.debug("Fetching document by ID: {}", id);
log.debug("Retrieved {} documents from repository", documents.size());
log.debug("Document {} has {} pages", id, pageCount);
```
Use for: Detailed operation info, counts, technical details

### WARN - Handled Issues
```java
log.warn("Document not found with ID: {}", id);
log.warn("Invalid page number requested: {}", pageNumber);
log.warn("Received empty file for document creation");
```
Use for: Expected error conditions, validation failures

### ERROR - Exceptions
```java
log.error("Failed to create document: {}", doc.getTitle(), e);
log.error("Failed to render page {} of document {}", pageNumber, id, e);
```
Use for: Unexpected errors, exceptions with stack traces

## Exception Handling Pattern by Layer

### Controller Layer
```java
@GetMapping("{id}")
public DocumentResponse get(@PathVariable UUID id) {
    log.info("GET /v1/documents/{} - Retrieving document", id);
    var response = toResponse(service.get(id));
    log.debug("Retrieved document: {}", id);
    return response;
}
```
Pattern: Log request → Call service → Log success

### Service Layer
```java
public Document create(Document doc) {
    log.info("Creating document with title: {}", doc.getTitle());
    try {
        Document created = documentBusinessLogic.createOrUpdateDocument(doc);
        log.info("Document created with ID: {}", created.getId());
        messageProducer.publishDocumentCreated(toDocumentResponse(created));
        return created;
    } catch (Exception e) {
        log.error("Failed to create document: {}", doc.getTitle(), e);
        throw new ServiceException("Failed to create document", e);
    }
}
```
Pattern: Log operation → Try business logic → Log success OR catch → log error → throw layer exception

### Business Logic Layer
```java
@Transactional(readOnly = true)
public Document getDocumentById(UUID id) {
    log.debug("Fetching document by ID: {}", id);
    try {
        return repository.findById(id)
            .orElseThrow(() -> {
                log.warn("Document not found with ID: {}", id);
                return new NotFoundException("Document not found: " + id);
            });
    } catch (NotFoundException e) {
        throw e; // Re-throw domain exceptions
    } catch (Exception e) {
        log.error("Error fetching document with ID: {}", id, e);
        throw new DataAccessException("Failed to fetch document", e);
    }
}
```
Pattern: Log operation → Try repository → Handle result OR catch → wrap in layer exception

### Messaging Layer
```java
public void publishDocumentCreated(DocumentResponse document) {
    log.info("Publishing document created event for document ID: {}", document.id());
    try {
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, document);
        log.debug("Successfully published event for ID: {}", document.id());
    } catch (Exception e) {
        log.error("Failed to publish event for ID: {}", document.id(), e);
        throw new MessagingException("Failed to publish event", e);
    }
}
```
Pattern: Log operation → Try send → Log success OR catch → log error → throw layer exception

## Common Patterns

### Re-throw Domain Exceptions
```java
} catch (NotFoundException e) {
    throw e; // Preserve original exception type
}
```

### Wrap Technical Exceptions
```java
} catch (SQLException e) {
    log.error("Database error: {}", e.getMessage(), e);
    throw new DataAccessException("Failed to save", e);
}
```

### Include Context in Messages
```java
log.error("Failed to render page {} of document {}", pageNumber, id, e);
throw new PdfProcessingException("Failed to render page " + pageNumber, e);
```

### Always Log Before Throwing
```java
log.error("Failed to process document: {}", id, e);
throw new ServiceException("Failed to process document", e);
```

## Testing Exception Handling

### Test 404 Errors
```bash
curl -X GET http://localhost:8080/v1/documents/99999999-9999-9999-9999-999999999999
# Expected: 404 NOT FOUND
# Log: WARN level - "Document not found..."
```

### Test Validation Errors
```bash
curl -X POST http://localhost:8080/v1/documents \
  -F "file=" \
  -F "title=Test"
# Expected: 400 BAD REQUEST  
# Log: WARN level - "Received empty file..."
```

### Test PDF Errors
```bash
curl -X GET http://localhost:8080/v1/documents/{id}/pages/999
# Expected: 400 BAD REQUEST
# Log: WARN level - "Invalid page number..."
```

### Monitor Logs
```bash
# Follow all logs
docker-compose logs -f backend

# Filter by level
docker-compose logs backend | grep ERROR
docker-compose logs backend | grep WARN

# Filter by operation
docker-compose logs backend | grep "Creating document"
docker-compose logs backend | grep "Failed to"
```

## Configuration Reference

### application.properties
```properties
# Adjust log levels per package
logging.level.fhtw.wien.controller=INFO    # Request/response logs
logging.level.fhtw.wien.service=INFO       # Service orchestration
logging.level.fhtw.wien.business=DEBUG     # Detailed business logic
logging.level.fhtw.wien.messaging=INFO     # Message queue events
logging.level.fhtw.wien.exception=WARN     # Exception handling

# Enable/disable specific loggers
logging.level.org.hibernate.SQL=DEBUG      # SQL statements
logging.level.org.springframework.web=INFO # Spring Web logs
```

## Best Practices

1. **Always log before throwing** - Ensures visibility even if exception is caught upstream
2. **Include context** - Add IDs, parameters, and relevant data to log messages
3. **Use correct level** - INFO for business events, DEBUG for details, WARN for handled issues, ERROR for exceptions
4. **Preserve exception cause** - Always include original exception in wrapper
5. **Don't log and throw** - Either handle (log + recover) or propagate (throw)
6. **Use structured logging** - Use placeholders `{}` instead of string concatenation
7. **Re-throw domain exceptions** - Preserve semantics of business exceptions
8. **Catch specific exceptions first** - Handle known exceptions before generic catch-all

## Summary

✅ 6 layer-specific exception types
✅ 12 exception handlers in ErrorHandler  
✅ 85+ strategic log statements across all layers
✅ Proper HTTP status code mapping
✅ Full exception cause chain preservation
✅ Comprehensive logging configuration
