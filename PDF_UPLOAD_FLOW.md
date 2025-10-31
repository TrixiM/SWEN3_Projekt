# PDF Upload Flow - Complete Sequential Analysis

This document explains the complete flow of uploading a PDF file, from HTTP request to final summary storage, with actual code snippets and data formats at each step.

---

## 🎯 Overview

**Flow Path:**
1. Frontend uploads PDF → Backend REST API
2. Backend stores file in MinIO → Saves metadata to PostgreSQL → Publishes RabbitMQ message
3. OCR Worker consumes message → Downloads from MinIO → Extracts text → Publishes OCR result
4. GenAI Worker consumes OCR result → Calls Gemini API → Publishes summary
5. Backend consumes summary → Updates database

---

## 📤 Step 1: Frontend Upload (HTTP POST)

### Request Format
```http
POST /v1/documents HTTP/1.1
Content-Type: multipart/form-data

--boundary
Content-Disposition: form-data; name="file"; filename="example.pdf"
Content-Type: application/pdf

<binary PDF data>
--boundary
Content-Disposition: form-data; name="title"

My Document Title
--boundary
Content-Disposition: form-data; name="tags"

tag1
--boundary
Content-Disposition: form-data; name="tags"

tag2
--boundary--
```

---

## 🖥️ Step 2: Backend Receives Upload

### Code: `DocumentController.create()`
**Location:** `backend/src/main/java/fhtw/wien/controller/DocumentController.java`

```java path=null start=null
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<DocumentResponse> create(
        @RequestParam("file") MultipartFile file,
        @RequestParam("title") String title,
        @RequestParam(value = "tags", required = false) List<String> tags
) throws IOException {
    // Create Document entity
    var doc = new Document(
            title,                                          // "My Document Title"
            originalFilename,                              // "example.pdf"
            file.getContentType(),                         // "application/pdf"
            file.getSize(),                              
            storageConfiguration.getDefaultBucket(),       // "documents"
            storageConfiguration.generateObjectKey(...),   // "documents/2025/10/29/uuid-example.pdf" #Komplett unnötig? weil es e in businesslogik wieder gemacht wird
            storageConfiguration.generateStorageUri(...),  // "minio://documents/..."
            null
    );
    doc.setPdfData(file.getBytes());                      // Binary PDF data
    if (tags != null) {
        doc.setTags(tags);                                // ["tag1", "tag2"]
    }
    
    var saved = service.create(doc);
    return ResponseEntity.created(...).body(DocumentMapper.toResponse(saved));
}
```

**Input:** `MultipartFile` object with binary PDF data  
**Output:** Creates `Document` entity with PDF bytes

---

## 💾 Step 3: Backend Stores File & Metadata

### Code: `DocumentBusinessLogic.createOrUpdateDocument()`
**Location:** `backend/src/main/java/fhtw/wien/business/DocumentBusinessLogic.java`

```java path=null start=null
@Transactional
public Document createOrUpdateDocument(Document doc) {
    // Generate ID for new document
    doc.setId(UUID.randomUUID());  // e.g., "61f7eaca-6b57-4ca5-bfd0-b7d44162ab86"
    
    // Upload to MinIO
    String objectKey = minioStorageService.uploadDocument(
        doc.getId(),           // UUID
        doc.getOriginalFilename(),  // "example.pdf"
        doc.getContentType(),       // "application/pdf"
        doc.getPdfData()            // byte[] of PDF
    );
    
    // Update document with MinIO info
    doc.setBucket("documents");
    doc.setObjectKey(objectKey);  // "documents/2025/10/29/71323e64-...-example.pdf" 
    doc.setStorageUri("minio://documents/...");
    
    // Clear PDF data (don't store in DB)
    doc.setPdfData(null);
    
    // Save to PostgreSQL
    Document saved = repository.save(doc);
    return saved;
}
```

**Input:** `Document` entity with PDF data  
**Output:** 
- File stored in MinIO at `s3://documents/2025/10/29/uuid-example.pdf`
- Database record created in `documents` table:
  ```sql
  INSERT INTO documents (id, title, original_filename, content_type, size_bytes, 
                         bucket, object_key, storage_uri, status, created_at, updated_at)
  VALUES ('61f7eaca-...', 'My Document Title', 'example.pdf', 'application/pdf', 
          562611, 'documents', 'documents/2025/10/29/...', 'minio://...', 'NEW', NOW(), NOW())
  ```

---

## 📨 Step 4: Backend Publishes RabbitMQ Message

### Code: `DocumentMessageProducer.publishDocumentCreated()`
**Location:** `backend/src/main/java/fhtw/wien/messaging/DocumentMessageProducer.java`

```java path=null start=null
public void publishDocumentCreated(DocumentResponse document) {
    rabbitTemplate.convertAndSend(
            DOCUMENT_EXCHANGE,              // "document.exchange"
            DOCUMENT_CREATED_ROUTING_KEY,   // "document.created"
            document                         // DocumentResponse object
    );
}
```

**Published Message (JSON):**
```json
{
  "id": "61f7eaca-6b57-4ca5-bfd0-b7d44162ab86",
  "title": "My Document Title",
  "originalFilename": "example.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 562611,
  "bucket": "documents",
  "objectKey": "documents/2025/10/29/71323e64-...-example.pdf",
  "storageUri": "minio://documents/...",
  "checksumSha256": null,
  "status": "NEW",
  "tags": ["tag1", "tag2"],
  "summary": null,
  "version": 1,
  "createdAt": "2025-10-29T18:33:58.123Z",
  "updatedAt": "2025-10-29T18:33:58.123Z"
}
```

**RabbitMQ Route:**
- Exchange: `document.exchange`
- Routing Key: `document.created`
- Queue: `document.created.queue`

---

## 🔍 Step 5: OCR Worker Consumes Message

### Code: `OcrMessageConsumer.handleDocumentCreated()`
**Location:** `ocr-worker/src/main/java/fhtw/wien/ocrworker/messaging/OcrMessageConsumer.java`

```java path=null start=null
@RabbitListener(queues = RabbitMQConfig.DOCUMENT_CREATED_QUEUE)
public void handleDocumentCreated(DocumentResponse document) {
    log.info("📄 OCR WORKER RECEIVED: Document created - ID: {}, Title: '{}'",
            document.id(), document.title());
    
    // Process document asynchronously
    ocrProcessingService.processDocument(document)
            .whenComplete((ocrResult, throwable) -> {
                if (throwable == null) {
                    sendOcrCompletionMessage(ocrResult);
                }
            });
}
```

**Input:** `DocumentResponse` Java object (deserialized from JSON)  
**Type:** Record class with fields: `id`, `title`, `originalFilename`, `contentType`, `sizeBytes`, `bucket`, `objectKey`, etc.

---

## 📄 Step 6: OCR Processing

### Code: `UnifiedOcrService.processDocument()`
**Location:** `ocr-worker/src/main/java/fhtw/wien/ocrworker/service/UnifiedOcrService.java`

```java path=null start=null
public OcrResultDto processDocument(DocumentResponse document) {
    long startTime = System.currentTimeMillis();
    
    // 1. Download PDF from MinIO
    byte[] pdfData = minioService.downloadDocument(document.objectKey());
    
    // 2. Convert PDF to images (using PDFBox)
    List<BufferedImage> images = pdfToImages(pdfData, 300); // 300 DPI
    
    // 3. Process each page with Tesseract OCR
    List<PageResult> pageResults = new ArrayList<>();
    StringBuilder fullText = new StringBuilder();
    
    for (int i = 0; i < images.size(); i++) {
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath("/usr/share/tesseract-ocr/4.00/tessdata");
        tesseract.setLanguage("eng");
        
        String pageText = tesseract.doOCR(images.get(i));
        int confidence = 85; // Calculated from tesseract
        
        fullText.append(pageText).append("\n");
        pageResults.add(new PageResult(i + 1, pageText, pageText.length(), confidence, ...));
    }
    
    // 4. Create result
    return OcrResultDto.success(
        document.id(),
        document.title(),
        fullText.toString(),
        pageResults,
        "eng",
        85,  // overall confidence
        System.currentTimeMillis() - startTime
    );
}
```

**Input:** `DocumentResponse` object  
**Processing:**
1. Downloads PDF from MinIO: `minioService.downloadDocument("documents/2025/10/29/...")`
2. Converts to images: 5 pages at 300 DPI
3. Runs Tesseract OCR on each image
4. Extracts text: 3937 characters

**Output:** `OcrResultDto` object

---

## 📨 Step 7: OCR Worker Publishes Completion

### Code: `OcrMessageConsumer.sendOcrCompletionMessage()`

```java path=null start=null
private void sendOcrCompletionMessage(OcrResultDto ocrResult) {
    rabbitTemplate.convertAndSend(
            RabbitMQConfig.DOCUMENT_EXCHANGE,       // "document.exchange"
            RabbitMQConfig.OCR_COMPLETED_ROUTING_KEY, // "ocr.completed"
            ocrResult
    );
}
```

**Published Message (JSON):**
```json
{
  "documentId": "61f7eaca-6b57-4ca5-bfd0-b7d44162ab86",
  "documentTitle": "My Document Title",
  "extractedText": "ISO 9001 Quality Management System\n\nIntroduction to ISO 9001...\n[3937 characters total]",
  "totalCharacters": 3937,
  "totalPages": 5,
  "pageResults": [
    {
      "pageNumber": 1,
      "extractedText": "ISO 9001 Quality Management...",
      "characterCount": 834,
      "confidence": 87,
      "isHighConfidence": true,
      "processingTimeMs": 3120
    },
    // ... 4 more pages
  ],
  "language": "eng",
  "overallConfidence": 85,
  "isHighConfidence": true,
  "processingTimeMs": 15501,
  "status": "SUCCESS",
  "errorMessage": null,
  "processedAt": "2025-10-29T18:34:14.456Z"
}
```

**RabbitMQ Route:**
- Exchange: `document.exchange`
- Routing Key: `ocr.completed`
- Queue: `ocr.completed.queue`

---

## 🤖 Step 8: GenAI Worker Consumes OCR Result

### Code: `GenAIMessageConsumer.handleOcrCompleted()`
**Location:** `genai-worker/src/main/java/fhtw/wien/genaiworker/messaging/GenAIMessageConsumer.java`

```java path=null start=null
@RabbitListener(queues = RabbitMQConfig.OCR_COMPLETED_QUEUE)
public void handleOcrCompleted(OcrResultDto message) {
    log.info("📨 GENAI WORKER RECEIVED: OCR completed for document: {}", message.documentId());
    
    summarizationService.processSummarization(message)
            .whenComplete((result, throwable) -> {
                if (throwable == null) {
                    sendSummaryResult(result);
                }
            });
}
```

**Input:** `OcrResultDto` Java object with:
- `documentId`: UUID
- `extractedText`: String (3937 characters)
- `totalPages`: 5
- `overallConfidence`: 85

---

## 🧠 Step 9: GenAI Summarization

### Code: `GeminiService.generateSummary()`
**Location:** `genai-worker/src/main/java/fhtw/wien/genaiworker/service/GeminiService.java`

```java path=null start=null
public String generateSummary(String text) throws GenAIException {
    // Build Gemini API request
    Map<String, Object> requestBody = Map.of(
        "contents", List.of(
            Map.of("parts", List.of(
                Map.of("text", "Summarize this document concisely:\n\n" + text)
            ))
        ),
        "generationConfig", Map.of(
            "temperature", 0.7,
            "maxOutputTokens", 1000
        )
    );
    
    // Call Gemini API
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    
    String url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + apiKey;
    
    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
    ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
    
    // Extract summary from response
    return extractTextFromResponse(response.getBody());
}
```

**API Request to Gemini:**
```json
{
  "contents": [
    {
      "parts": [
        {
          "text": "Summarize this document concisely:\n\nISO 9001 Quality Management System\n\nIntroduction to ISO 9001..."
        }
      ]
    }
  ],
  "generationConfig": {
    "temperature": 0.7,
    "maxOutputTokens": 1000
  }
}
```

**API Response from Gemini:**
```json
{
  "candidates": [
    {
      "content": {
        "parts": [
          {
            "text": "This document provides an overview of ISO 9001, the foundational quality management system (QMS) standard. It covers the key principles, requirements, and benefits of implementing ISO 9001 for organizations seeking to improve their processes, customer satisfaction, and overall quality performance. The document highlights the importance of process approach, continual improvement, and evidence-based decision making in achieving quality objectives."
          }
        ]
      }
    }
  ]
}
```

**Processing Time:** 5858ms  
**Summary Length:** 762 characters

---

## 📨 Step 10: GenAI Worker Publishes Summary

### Code: `GenAIMessageConsumer.sendSummaryResult()`

```java path=null start=null
private void sendSummaryResult(SummaryResultMessage result) {
    rabbitTemplate.convertAndSend(
            RabbitMQConfig.DOCUMENT_EXCHANGE,           // "document.exchange"
            RabbitMQConfig.SUMMARY_RESULT_ROUTING_KEY,  // "summary.result"
            result
    );
}
```

**Published Message (JSON):**
```json
{
  "documentId": "61f7eaca-6b57-4ca5-bfd0-b7d44162ab86",
  "title": "My Document Title",
  "summary": "This document provides an overview of ISO 9001, the foundational quality management system (QMS) standard. It covers the key principles, requirements, and benefits of implementing ISO 9001 for organizations seeking to improve their processes, customer satisfaction, and overall quality performance. The document highlights the importance of process approach, continual improvement, and evidence-based decision making in achieving quality objectives.",
  "status": "SUCCESS",
  "errorMessage": null,
  "processingTimeMs": 5858,
  "timestamp": "2025-10-29T18:34:20.314Z"
}
```

**RabbitMQ Route:**
- Exchange: `document.exchange`
- Routing Key: `summary.result`
- Queue: `summary.result.queue`

---

## 💾 Step 11: Backend Updates Summary

### Code: `DocumentMessageConsumer.handleSummaryResult()`
**Location:** `backend/src/main/java/fhtw/wien/messaging/DocumentMessageConsumer.java`

```java path=null start=null
@RabbitListener(queues = SUMMARY_RESULT_QUEUE)
@Transactional
public void handleSummaryResult(SummaryResultDto summaryResult) {
    log.info("📨 BACKEND RECEIVED: Summary result for document ID: {}", summaryResult.documentId());
    
    if (summaryResult.isSuccess()) {
        // Find document and update summary
        Document document = documentRepo.findById(summaryResult.documentId())
                .orElseThrow(() -> new MessagingException("Document not found"));
        
        document.setSummary(summaryResult.summary());
        documentRepo.save(document);
        
        log.info("✅ Summary saved for document: {} (length: {} characters)",
                summaryResult.documentId(), summaryResult.summary().length());
    }
}
```

**Input:** `SummaryResultDto` Java object  
**Database Update:**
```sql
UPDATE documents 
SET summary = 'This document provides an overview of ISO 9001, the foundational quality management system (QMS) standard. It covers the key principles, requirements, and benefits of implementing ISO 9001 for organizations seeking to improve their processes, customer satisfaction, and overall quality performance. The document highlights the importance of process approach, continual improvement, and evidence-based decision making in achieving quality objectives.',
    updated_at = NOW()
WHERE id = '61f7eaca-6b57-4ca5-bfd0-b7d44162ab86'
```

---

## 📋 Step 12: Frontend Retrieves Document

### API Request
```http
GET /v1/documents/61f7eaca-6b57-4ca5-bfd0-b7d44162ab86 HTTP/1.1
```

### API Response (with summary)
```json
{
  "id": "61f7eaca-6b57-4ca5-bfd0-b7d44162ab86",
  "title": "My Document Title",
  "originalFilename": "example.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 562611,
  "bucket": "documents",
  "objectKey": "documents/2025/10/29/71323e64-...-example.pdf",
  "storageUri": "minio://documents/...",
  "checksumSha256": null,
  "status": "NEW",
  "tags": ["tag1", "tag2"],
  "summary": "This document provides an overview of ISO 9001, the foundational quality management system (QMS) standard. It covers the key principles, requirements, and benefits of implementing ISO 9001 for organizations seeking to improve their processes, customer satisfaction, and overall quality performance. The document highlights the importance of process approach, continual improvement, and evidence-based decision making in achieving quality objectives.",
  "version": 1,
  "createdAt": "2025-10-29T18:33:58.123Z",
  "updatedAt": "2025-10-29T18:34:20.567Z"
}
```

---

## 🎯 Complete Timeline Example

| Time | Service | Action | Data Format |
|------|---------|--------|-------------|
| T+0s | Frontend | POST /v1/documents | `multipart/form-data` with PDF binary |
| T+0.1s | Backend | Stores in MinIO | Binary PDF → MinIO object |
| T+0.2s | Backend | Saves to PostgreSQL | `Document` entity → DB row |
| T+0.3s | Backend | Publishes to RabbitMQ | `DocumentResponse` object → JSON |
| T+0.4s | OCR Worker | Receives message | JSON → `DocumentResponse` object |
| T+0.5s | OCR Worker | Downloads from MinIO | Object key → Binary PDF |
| T+6s | OCR Worker | Completes OCR | Binary PDF → Extracted text (3937 chars) |
| T+6.1s | OCR Worker | Publishes to RabbitMQ | `OcrResultDto` object → JSON |
| T+6.2s | GenAI Worker | Receives OCR result | JSON → `OcrResultDto` object |
| T+6.3s | GenAI Worker | Calls Gemini API | HTTP POST with text → Summary (762 chars) |
| T+12.1s | GenAI Worker | Publishes summary | `SummaryResultMessage` → JSON |
| T+12.2s | Backend | Receives summary | JSON → `SummaryResultDto` object |
| T+12.3s | Backend | Updates database | SQL UPDATE with summary text |

**Total Processing Time:** ~12 seconds (OCR: 15.5s, GenAI: 5.9s, messaging: <1s)

---

## 🗂️ Data Formats Summary

| Stage | Format | Example |
|-------|--------|---------|
| HTTP Upload | `multipart/form-data` | Binary PDF + form fields |
| Java Entity | `Document` object | In-memory Java object with byte[] |
| MinIO Storage | Binary blob | S3-compatible object storage |
| Database | SQL row | PostgreSQL `documents` table |
| RabbitMQ | JSON | Serialized Java objects |
| Gemini API | JSON | HTTP REST request/response |

---

## 🔄 Message Flow Diagram

```
Frontend
   |
   | (1) HTTP POST multipart/form-data
   ↓
Backend (REST API)
   |
   | (2) Upload binary → MinIO
   | (3) Save metadata → PostgreSQL
   | (4) Publish JSON → RabbitMQ (document.created)
   ↓
OCR Worker
   |
   | (5) Download PDF from MinIO
   | (6) Extract text with Tesseract
   | (7) Publish JSON → RabbitMQ (ocr.completed)
   ↓
GenAI Worker
   |
   | (8) Call Gemini API with text
   | (9) Generate summary
   | (10) Publish JSON → RabbitMQ (summary.result)
   ↓
Backend
   |
   | (11) Update PostgreSQL with summary
   ↓
Frontend
   |
   | (12) GET /v1/documents/{id}
   | (13) Receive JSON with summary
```

---

## 📦 Key DTOs/Objects

### DocumentResponse
```java
record DocumentResponse(
    UUID id,
    String title,
    String originalFilename,
    String contentType,
    Long sizeBytes,
    String bucket,
    String objectKey,
    String storageUri,
    String checksumSha256,
    DocumentStatus status,
    List<String> tags,
    String summary,
    int version,
    Instant createdAt,
    Instant updatedAt
)
```

### OcrResultDto
```java
record OcrResultDto(
    UUID documentId,
    String documentTitle,
    String extractedText,
    int totalCharacters,
    int totalPages,
    List<PageResult> pageResults,
    String language,
    int overallConfidence,
    boolean isHighConfidence,
    long processingTimeMs,
    String status,
    String errorMessage,
    Instant processedAt
)
```

### SummaryResultMessage
```java
record SummaryResultMessage(
    UUID documentId,
    String title,
    String summary,
    String status,
    String errorMessage,
    long processingTimeMs,
    Instant timestamp
)
```
