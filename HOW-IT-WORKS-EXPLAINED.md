# How the OCR Worker System Works - Detailed Explanation

## Complete Example: Uploading a Document Named "test.pdf"

Let me walk you through exactly what happens when you upload a document, showing every piece of code that gets executed.

---

## 🎬 THE COMPLETE SEQUENCE

### STEP 1: User Uploads Document
```bash
curl -X POST http://localhost:8080/v1/documents \
  -F "file=@test.pdf" \
  -F "title=My Test Document"
```

---

### STEP 2: REST Server - DocumentController Receives Request

**File**: `backend/src/main/java/fhtw/wien/controller/DocumentController.java`
**Lines**: 33-54

```java
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<DocumentResponse> create(
        @RequestParam("file") MultipartFile file,      // ← Your test.pdf comes here
        @RequestParam("title") String title            // ← "My Test Document"
) throws IOException {
    // Step 2.1: Create a Document object from the uploaded file
    var doc = new Document(
            title,                          // "My Test Document"
            file.getOriginalFilename(),     // "test.pdf"
            file.getContentType(),          // "application/pdf"
            file.getSize(),                 // file size in bytes
            "local-storage",
            "documents/" + System.currentTimeMillis() + "-" + file.getOriginalFilename(),
            "file://" + file.getOriginalFilename(),
            null
    );
    doc.setPdfData(file.getBytes());        // Store the actual PDF bytes
    
    // Step 2.2: Call the service layer to save it
    var saved = service.create(doc);         // ← THIS IS WHERE THE MAGIC BEGINS!
    
    var body = toResponse(saved);
    return ResponseEntity.created(URI.create("/v1/documents/" + saved.getId())).body(body);
}
```

**What happens here?**
- Spring Boot receives your HTTP POST request
- Extracts the file and title from the multipart form data
- Creates a `Document` object with all the metadata
- Calls `service.create(doc)` to save it

---

### STEP 3: REST Server - DocumentService Saves and Publishes

**File**: `backend/src/main/java/fhtw/wien/service/DocumentService.java`
**Lines**: 28-34

```java
public Document create(Document doc) {
    // Step 3.1: Save to database
    Document created = documentBusinessLogic.createOrUpdateDocument(doc);
    // At this point, the document is saved to PostgreSQL with a UUID
    
    // Step 3.2: Convert to DTO (Data Transfer Object)
    DocumentResponse response = toDocumentResponse(created);
    // This creates a response object with:
    // - id: UUID (e.g., "123e4567-e89b-12d3-a456-426614174000")
    // - title: "My Test Document"
    // - originalFilename: "test.pdf"
    // - contentType: "application/pdf"
    // - sizeBytes: 12345
    // - status: PENDING
    // - createdAt, updatedAt, etc.
    
    // Step 3.3: PUBLISH TO RABBITMQ! ← THIS IS THE KEY STEP
    messageProducer.publishDocumentCreated(response);
    
    return created;
}
```

**What happens here?**
- First, saves the document to PostgreSQL (via `documentBusinessLogic`)
- Converts the saved document to a `DocumentResponse` DTO
- Calls `messageProducer.publishDocumentCreated()` to send a message to RabbitMQ

**Why do we convert to DocumentResponse?**
- It's a clean DTO without internal implementation details
- Contains only the data needed for messaging
- JSON-serializable for RabbitMQ

---

### STEP 4: REST Server - DocumentMessageProducer Sends to RabbitMQ

**File**: `backend/src/main/java/fhtw/wien/messaging/DocumentMessageProducer.java`
**Lines**: 23-30

```java
public void publishDocumentCreated(DocumentResponse document) {
    log.info("Publishing document created event for document ID: {}", document.id());
    
    // This sends the message to RabbitMQ
    rabbitTemplate.convertAndSend(
            RabbitMQConfig.DOCUMENT_EXCHANGE,           // Exchange: "document.exchange"
            RabbitMQConfig.DOCUMENT_CREATED_ROUTING_KEY, // Routing Key: "document.created"
            document                                      // The DocumentResponse object
    );
}
```

**What happens here?**
- Uses `RabbitTemplate` (Spring's RabbitMQ client)
- Sends to exchange: `"document.exchange"`
- With routing key: `"document.created"`
- Payload: Your `DocumentResponse` object (automatically converted to JSON)

**JSON that gets sent looks like this:**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "title": "My Test Document",
  "originalFilename": "test.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 12345,
  "status": "PENDING",
  "createdAt": "2024-10-05T15:30:00.000Z",
  ...
}
```

---

### STEP 5: RabbitMQ Routes the Message

**RabbitMQ Internal Process:**

1. Message arrives at exchange `"document.exchange"`
2. RabbitMQ looks at the routing key: `"document.created"`
3. RabbitMQ checks which queues are bound to this exchange with this routing key
4. Finds `"document.created.queue"` is bound with routing key `"document.created"`
5. Routes the message to `"document.created.queue"`
6. Message sits in the queue waiting to be consumed

**Visual:**
```
REST Server → [document.exchange] --routing key: "document.created"--> [document.created.queue]
```

---

### STEP 6: OCR Worker - OcrMessageConsumer Receives Message

**File**: `ocr-worker/src/main/java/fhtw/wien/ocrworker/messaging/OcrMessageConsumer.java`
**Lines**: 22-69

```java
@RabbitListener(queues = RabbitMQConfig.DOCUMENT_CREATED_QUEUE)
// ↑ This annotation tells Spring: "Listen to the 'document.created.queue'"
// Spring automatically calls this method when a message arrives

public void handleDocumentCreated(DocumentResponse document) {
    // Step 6.1: Message received! Spring automatically deserializes JSON to DocumentResponse
    log.info("📄 OCR WORKER RECEIVED: Document created - ID: {}, Title: '{}'",
            document.id(), document.title());
    
    // Step 6.2: Log the details
    log.info("📋 Document Details:");
    log.info("   - Filename: {}", document.originalFilename());
    log.info("   - Content Type: {}", document.contentType());
    log.info("   - Size: {} bytes", document.sizeBytes());
    log.info("   - Status: {}", document.status());
    
    // Step 6.3: Simulate OCR processing
    log.info("🔄 Simulating OCR processing...");
    
    try {
        Thread.sleep(1000);  // Pretend we're doing OCR for 1 second
        
        log.info("✅ OCR processing completed successfully for document: {}", document.id());
        
        // Step 6.4: Send acknowledgment back
        String ackMessage = String.format(
                "✅ OCR Worker: Document '%s' (ID: %s) processed successfully",
                document.title(), document.id()
        );
        
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DOCUMENT_EXCHANGE,      // Same exchange
                "document.created.ack",                 // Different routing key
                ackMessage                              // Simple string message
        );
        
        log.info("📤 Sent acknowledgment to queue");
        
    } catch (Exception e) {
        log.error("❌ Error processing document", e);
    }
}
```

**What happens here?**
- Spring's `@RabbitListener` automatically listens to the queue
- When message arrives, Spring deserializes the JSON back to `DocumentResponse` object
- Method executes: logs info, simulates processing, sends acknowledgment
- The acknowledgment goes to `"document.created.ack.queue"` (separate queue)

---

## 🤔 WHY TWO RabbitMQConfig FILES?

Great question! Here's the explanation:

### REST Server's RabbitMQConfig
**File**: `backend/src/main/java/fhtw/wien/config/RabbitMQConfig.java`

**Purpose**: Configures RabbitMQ for the REST Server application

**What it does**:
```java
@Bean
public DirectExchange documentExchange() {
    return new DirectExchange(DOCUMENT_EXCHANGE);
}

@Bean
public Queue documentCreatedQueue() {
    return new Queue(DOCUMENT_CREATED_QUEUE, true);
}

@Bean
public Binding documentCreatedBinding(Queue documentCreatedQueue, DirectExchange documentExchange) {
    return BindingBuilder.bind(documentCreatedQueue)
            .to(documentExchange)
            .with(DOCUMENT_CREATED_ROUTING_KEY);
}
```

**This creates in RabbitMQ**:
- Exchange: `"document.exchange"`
- Queue: `"document.created.queue"`
- Binding: Connects queue to exchange with routing key `"document.created"`
- Also creates queues for: deleted, acknowledgments

**Key Constants**:
- `DOCUMENT_EXCHANGE = "document.exchange"`
- `DOCUMENT_CREATED_QUEUE = "document.created.queue"`
- `DOCUMENT_DELETED_QUEUE = "document.deleted.queue"`
- `DOCUMENT_CREATED_ACK_QUEUE = "document.created.ack.queue"`

---

### OCR Worker's RabbitMQConfig
**File**: `ocr-worker/src/main/java/fhtw/wien/ocrworker/config/RabbitMQConfig.java`

**Purpose**: Configures RabbitMQ for the OCR Worker application

**What it does**:
```java
@Bean
public DirectExchange documentExchange() {
    return new DirectExchange(DOCUMENT_EXCHANGE);
}

@Bean
public Queue documentCreatedQueue() {
    return new Queue(DOCUMENT_CREATED_QUEUE, true);
}

@Bean
public Binding documentCreatedBinding(Queue documentCreatedQueue, DirectExchange documentExchange) {
    return BindingBuilder.bind(documentCreatedQueue)
            .to(documentExchange)
            .with(DOCUMENT_CREATED_ROUTING_KEY);
}
```

**This creates in RabbitMQ**:
- Exchange: `"document.exchange"` (same as REST Server)
- Queue: `"document.created.queue"` (same as REST Server)
- Binding: Same connection
- Also creates: `"document.created.ack.queue"`

**Key Constants** (subset):
- `DOCUMENT_EXCHANGE = "document.exchange"` (SAME)
- `DOCUMENT_CREATED_QUEUE = "document.created.queue"` (SAME)
- `DOCUMENT_CREATED_ACK_QUEUE = "document.created.ack.queue"` (SAME)

---

## 🎯 WHY DUPLICATE CONFIGURATIONS?

### Reason 1: Separate Applications
- REST Server and OCR Worker are **completely separate applications**
- They run in different processes, different containers
- Each needs its own Spring configuration
- Each needs to define what queues/exchanges it uses

### Reason 2: RabbitMQ is Idempotent
- When multiple applications declare the same queue/exchange, RabbitMQ says "OK, already exists"
- It **doesn't create duplicates**
- It just verifies the configuration matches
- This is by design and safe!

### Reason 3: Different Responsibilities

**REST Server needs**:
- `document.created.queue` (to publish to)
- `document.deleted.queue` (to publish to)
- `document.deleted.ack.queue` (to consume from)
- All the bindings for these

**OCR Worker needs**:
- `document.created.queue` (to consume from)
- `document.created.ack.queue` (to publish to)
- Only the bindings it uses

### Reason 4: Independence
- If you deploy only the REST Server, it creates its queues
- If you deploy only the OCR Worker, it creates its queues
- If you deploy both, they share the same queues (by name)
- Each app is self-contained and can run independently

---

## 📊 VISUAL ARCHITECTURE

```
┌─────────────────────────────────────────────────────────────────┐
│                         RABBITMQ SERVER                         │
│                                                                 │
│  ┌──────────────────┐                                          │
│  │ document.exchange│  (Direct Exchange)                       │
│  └────────┬─────────┘                                          │
│           │                                                     │
│           ├──routing key: "document.created"──────┐           │
│           │                                         ▼           │
│           │                        ┌────────────────────────┐ │
│           │                        │document.created.queue │ │
│           │                        └────────────────────────┘ │
│           │                                                     │
│           ├──routing key: "document.deleted"──────┐           │
│           │                                         ▼           │
│           │                        ┌────────────────────────┐ │
│           │                        │document.deleted.queue │ │
│           │                        └────────────────────────┘ │
│           │                                                     │
│           └──routing key: "document.created.ack"──┐           │
│                                                     ▼           │
│                                    ┌──────────────────────────┐│
│                                    │document.created.ack.queue││
│                                    └──────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘

         ▲                                           │
         │ Publishes                                 │ Consumes
         │ (Send messages)                           │ (Receive messages)
         │                                           ▼

┌──────────────────┐                        ┌──────────────────┐
│   REST Server    │                        │   OCR Worker     │
│                  │                        │                  │
│  DocumentService │                        │ OcrMessage       │
│       ↓          │                        │ Consumer         │
│  MessageProducer │                        │                  │
│                  │                        │ @RabbitListener  │
│  RabbitMQConfig  │                        │                  │
│  (defines queues)│                        │  RabbitMQConfig  │
│                  │                        │  (defines queues)│
└──────────────────┘                        └──────────────────┘
```

---

## 🔄 COMPLETE MESSAGE FLOW

```
1. User uploads "test.pdf"
   ↓
2. DocumentController.create() receives file
   ↓
3. DocumentService.create() saves to PostgreSQL
   ↓
4. DocumentService calls messageProducer.publishDocumentCreated()
   ↓
5. DocumentMessageProducer sends JSON message to RabbitMQ
   {
     "id": "xxx",
     "title": "My Test Document",
     "originalFilename": "test.pdf",
     ...
   }
   ↓
6. RabbitMQ receives at "document.exchange" with routing key "document.created"
   ↓
7. RabbitMQ routes to "document.created.queue"
   ↓
8. OCR Worker's OcrMessageConsumer (with @RabbitListener) receives message
   ↓
9. Spring deserializes JSON back to DocumentResponse object
   ↓
10. handleDocumentCreated() method executes
    - Logs document info
    - Simulates OCR processing (1 second delay)
    - Sends acknowledgment to "document.created.ack" queue
   ↓
11. Done! Document is saved and processed
```

---

## 🔑 KEY CONCEPTS EXPLAINED

### 1. Exchange
- **What**: A router that receives messages and routes them to queues
- **Type**: DirectExchange (routes based on routing key matching)
- **Name**: `"document.exchange"`
- **Think of it as**: A post office that sorts mail

### 2. Queue
- **What**: Storage for messages waiting to be consumed
- **Durable**: `true` means survives RabbitMQ restarts
- **Names**: `"document.created.queue"`, `"document.deleted.queue"`, etc.
- **Think of it as**: A mailbox where messages wait

### 3. Routing Key
- **What**: A label on the message that determines which queue(s) it goes to
- **Examples**: `"document.created"`, `"document.deleted"`, `"document.created.ack"`
- **Think of it as**: The address on an envelope

### 4. Binding
- **What**: A rule connecting a queue to an exchange
- **Says**: "Route messages with routing key X to queue Y"
- **Example**: `documentCreatedBinding` says "Route messages with key 'document.created' to 'document.created.queue'"

### 5. Producer
- **What**: Code that sends messages (DocumentMessageProducer)
- **Does**: `rabbitTemplate.convertAndSend(exchange, routingKey, message)`

### 6. Consumer
- **What**: Code that receives messages (OcrMessageConsumer)
- **Does**: `@RabbitListener(queues = "document.created.queue")`

### 7. RabbitTemplate
- **What**: Spring's client for interacting with RabbitMQ
- **Automatically**: Serializes objects to JSON
- **Automatically**: Deserializes JSON back to objects

---

## ❓ COMMON QUESTIONS

### Q: Why not just call the OCR worker directly from REST server?
**A**: 
- **Decoupling**: REST server doesn't need to know about OCR worker
- **Scalability**: Can run multiple OCR workers
- **Reliability**: If OCR worker is down, messages wait in queue
- **Asynchronous**: User doesn't wait for OCR to complete
- **Resilience**: Messages can be retried if processing fails

### Q: What if OCR worker is not running?
**A**: Messages accumulate in `"document.created.queue"` and are processed when worker starts

### Q: Can I have multiple OCR workers?
**A**: Yes! Run multiple instances and RabbitMQ load-balances messages between them

### Q: What if processing fails?
**A**: You can configure retry logic and dead letter queues (not implemented yet)

### Q: Where is the actual PDF stored?
**A**: In PostgreSQL database (in the `pdf_data` column as bytes). OCR worker would download it from there.

---

## 🎓 SUMMARY

1. **Two configs because**: Two separate applications, each needs its own Spring configuration
2. **Same queue names**: Intentional! They connect to the same RabbitMQ queues
3. **Message flow**: REST Server → RabbitMQ → OCR Worker
4. **Asynchronous**: Upload completes immediately, OCR happens in background
5. **Reliable**: Messages wait in queue if worker is busy/down
6. **Scalable**: Add more workers to process faster

The beauty of this design is that each application is independent, but they communicate through RabbitMQ queues!
