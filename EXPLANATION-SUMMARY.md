# Complete Explanation - OCR Worker System

## 🎯 Quick Answer to Your Questions

### Why Two RabbitMQConfig Files?

**Simple Answer**: Because you have TWO separate applications (REST Server and OCR Worker), and each needs its own Spring configuration.

**Analogy**: It's like two people who both need to know the address of the same post office. Each person has their own GPS app with the address programmed in. The address is the same, but each person has their own configuration.

**Technical Answer**:
1. REST Server runs in one Java process (one JVM)
2. OCR Worker runs in a different Java process (another JVM)
3. Each Spring Boot app needs its own `@Configuration` classes
4. Both define the same queue names, which is **intentional and safe**
5. RabbitMQ is idempotent - declaring the same queue twice doesn't create duplicates

---

## 📝 Example: Uploading "test.pdf"

Let me walk through exactly what happens when you run:
```bash
curl -X POST http://localhost:8080/v1/documents \
  -F "file=@test.pdf" \
  -F "title=My Document"
```

### Step-by-Step Code Execution

#### STEP 1: Request Arrives at REST Server
**File**: `DocumentController.java` (lines 34-54)
```java
@PostMapping
public ResponseEntity<DocumentResponse> create(
    @RequestParam("file") MultipartFile file,  // ← Your test.pdf
    @RequestParam("title") String title         // ← "My Document"
) {
    // Create Document object from uploaded file
    var doc = new Document(title, ...);
    doc.setPdfData(file.getBytes());  // Store PDF bytes
    
    // Save and publish
    var saved = service.create(doc);  // ← Goes to Step 2
    return ResponseEntity.created(...).body(...);
}
```
**What happens**: Spring MVC receives your HTTP request and extracts the file and title.

---

#### STEP 2: Service Saves to Database
**File**: `DocumentService.java` (lines 28-34)
```java
public Document create(Document doc) {
    // Save to PostgreSQL first
    Document created = documentBusinessLogic.createOrUpdateDocument(doc);
    
    // Convert to DTO for messaging
    DocumentResponse response = toDocumentResponse(created);
    
    // CRITICAL: Publish to RabbitMQ
    messageProducer.publishDocumentCreated(response);  // ← Goes to Step 3
    
    return created;
}
```
**What happens**: 
- Document saved to PostgreSQL with a UUID (e.g., `123e4567-...`)
- Converted to `DocumentResponse` DTO
- Calls the message producer

---

#### STEP 3: Producer Sends Message to RabbitMQ
**File**: `DocumentMessageProducer.java` (lines 23-30)
```java
public void publishDocumentCreated(DocumentResponse document) {
    log.info("Publishing document created event for document ID: {}", document.id());
    
    rabbitTemplate.convertAndSend(
        "document.exchange",        // Exchange name
        "document.created",         // Routing key
        document                    // Your DocumentResponse object
    );
}
```
**What happens**: 
- `RabbitTemplate` serializes your `DocumentResponse` to JSON
- Sends message to RabbitMQ exchange `"document.exchange"` with routing key `"document.created"`

**JSON sent to RabbitMQ looks like**:
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "title": "My Document",
  "originalFilename": "test.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 12345,
  "status": "PENDING",
  "createdAt": "2024-10-05T15:30:00.000Z",
  "updatedAt": "2024-10-05T15:30:00.000Z"
}
```

---

#### STEP 4: RabbitMQ Routes Message
**RabbitMQ Server** (internal logic)
```
1. Message arrives at exchange "document.exchange"
2. Look at routing key: "document.created"
3. Check bindings: Which queues want messages with this routing key?
4. Found: "document.created.queue" is bound with routing key "document.created"
5. Route message to "document.created.queue"
6. Message now sits in queue waiting for consumer
```

**Visual**:
```
[Producer] → Exchange: "document.exchange"
                  ↓ (routing key: "document.created")
             Queue: "document.created.queue"
                  ↓
             [Consumer waiting...]
```

---

#### STEP 5: OCR Worker Receives Message
**File**: `OcrMessageConsumer.java` (lines 22-69)
```java
@RabbitListener(queues = "document.created.queue")  // ← Spring polls this queue
public void handleDocumentCreated(DocumentResponse document) {
    // Spring automatically:
    // 1. Polls the queue for new messages
    // 2. Deserializes JSON back to DocumentResponse object
    // 3. Calls this method with the object
    
    log.info("📄 OCR WORKER RECEIVED: Document ID: {}, Title: '{}'",
            document.id(), document.title());
    
    // Log details
    log.info("📋 Document Details:");
    log.info("   - Filename: {}", document.originalFilename());
    log.info("   - Size: {} bytes", document.sizeBytes());
    
    // Simulate OCR processing
    log.info("🔄 Simulating OCR processing...");
    Thread.sleep(1000);  // Pretend we're doing OCR
    
    log.info("✅ OCR processing completed!");
    
    // Send acknowledgment
    rabbitTemplate.convertAndSend(
        "document.exchange",
        "document.created.ack",
        "Document processed successfully!"
    );
}
```
**What happens**: 
- Spring's `@RabbitListener` continuously checks the queue
- When message appears, Spring deserializes JSON to `DocumentResponse`
- Method executes: logs info, simulates processing, sends acknowledgment

---

## 🔑 Key Concepts Explained

### 1. RabbitTemplate
**What it is**: Spring's client for talking to RabbitMQ

**What it does**:
- Serializes Java objects to JSON automatically
- Deserializes JSON back to Java objects automatically
- Sends messages: `rabbitTemplate.convertAndSend(exchange, routingKey, object)`
- Configured by `RabbitMQConfig` with `Jackson2JsonMessageConverter`

### 2. @RabbitListener
**What it is**: Spring annotation that marks a method as a message consumer

**What it does**:
```java
@RabbitListener(queues = "document.created.queue")
public void handleMessage(DocumentResponse doc) {
    // This method is called when a message arrives
}
```
- Spring continuously polls the specified queue
- When message arrives, Spring:
  1. Retrieves message from queue
  2. Deserializes JSON to the parameter type
  3. Calls your method
  4. Acknowledges message (removes from queue)

### 3. Exchange
**What it is**: A message router in RabbitMQ

**Types**:
- **Direct Exchange** (what we use): Routes based on exact routing key match
- Topic Exchange: Routes based on pattern matching
- Fanout Exchange: Routes to all queues
- Headers Exchange: Routes based on headers

**Our exchange**: `"document.exchange"` (DirectExchange)

### 4. Queue
**What it is**: A mailbox that stores messages

**Properties**:
- **Name**: `"document.created.queue"`
- **Durable**: `true` means survives RabbitMQ restart
- **FIFO**: Messages processed in order (First In, First Out)

### 5. Routing Key
**What it is**: A label on messages that determines routing

**How it works**:
- Producer sends message with routing key: `"document.created"`
- Exchange checks which queues have bindings for this key
- Message routed to matching queues

**Think of it like**: The address on an envelope

### 6. Binding
**What it is**: A rule connecting queue to exchange

**Example**:
```java
@Bean
public Binding documentCreatedBinding(Queue queue, DirectExchange exchange) {
    return BindingBuilder.bind(queue)
            .to(exchange)
            .with("document.created");  // Routing key
}
```

**Says**: "Route messages with routing key 'document.created' to this queue"

---

## 🔄 Data Flow Diagram

```
┌─────────┐
│  User   │ Uploads test.pdf
└────┬────┘
     │ HTTP POST
     ▼
┌──────────────────┐
│ DocumentController│ Receives file, creates Document object
└────┬─────────────┘
     │ service.create()
     ▼
┌──────────────────┐
│ DocumentService  │ Saves to PostgreSQL, triggers messaging
└────┬─────────────┘
     │ messageProducer.publish()
     ▼
┌────────────────────┐
│ MessageProducer    │ Converts to JSON, sends to RabbitMQ
└────┬───────────────┘
     │ RabbitTemplate.send()
     ▼
┌──────────────────────────────────────┐
│           RABBITMQ SERVER            │
│                                      │
│  ┌────────────────┐                 │
│  │ document.      │ Receives message│
│  │ exchange       │                 │
│  └───────┬────────┘                 │
│          │ Routing: "document.created"
│          ▼                           │
│  ┌────────────────┐                 │
│  │ document.      │ Stores message  │
│  │ created.queue  │                 │
│  └───────┬────────┘                 │
└──────────┼──────────────────────────┘
           │ Message available
           ▼
┌──────────────────┐
│ @RabbitListener  │ Spring polls queue
└────┬─────────────┘
     │ Message found!
     ▼
┌──────────────────┐
│ OcrMessage       │ Deserializes JSON to DocumentResponse
│ Consumer         │
└────┬─────────────┘
     │ handleDocumentCreated()
     ▼
┌──────────────────┐
│ Process Document │ Log info, simulate OCR, send ACK
└──────────────────┘
```

---

## ❓ Why Two RabbitMQConfig Files? (Detailed)

### REST Server's RabbitMQConfig
**Location**: `backend/src/main/java/fhtw/wien/config/RabbitMQConfig.java`

**Purpose**: Configure RabbitMQ for the REST Server Spring application

**Creates**:
```java
@Bean
public DirectExchange documentExchange() {
    return new DirectExchange("document.exchange");
}

@Bean
public Queue documentCreatedQueue() {
    return new Queue("document.created.queue", true);
}

@Bean
public Queue documentDeletedQueue() {
    return new Queue("document.deleted.queue", true);
}

@Bean
public Binding documentCreatedBinding(...) {
    return BindingBuilder.bind(documentCreatedQueue)
            .to(documentExchange)
            .with("document.created");
}
```

**Why needed**: 
- REST Server is a separate Spring Boot application
- Needs to define what queues/exchanges it uses
- Used by `DocumentMessageProducer` to send messages
- Runs in its own JVM/container

---

### OCR Worker's RabbitMQConfig
**Location**: `ocr-worker/src/main/java/fhtw/wien/ocrworker/config/RabbitMQConfig.java`

**Purpose**: Configure RabbitMQ for the OCR Worker Spring application

**Creates**:
```java
@Bean
public DirectExchange documentExchange() {
    return new DirectExchange("document.exchange");  // SAME NAME
}

@Bean
public Queue documentCreatedQueue() {
    return new Queue("document.created.queue", true);  // SAME NAME
}

@Bean
public Binding documentCreatedBinding(...) {
    return BindingBuilder.bind(documentCreatedQueue)
            .to(documentExchange)
            .with("document.created");  // SAME ROUTING KEY
}
```

**Why needed**:
- OCR Worker is a completely separate Spring Boot application
- Has its own JVM/container
- Needs its own Spring configuration
- Used by `OcrMessageConsumer` to receive messages

---

### What Happens in RabbitMQ?

When both applications start:

1. **REST Server starts first**:
   - Connects to RabbitMQ
   - Declares: "I need exchange 'document.exchange'"
   - RabbitMQ: "OK, created!"
   - Declares: "I need queue 'document.created.queue'"
   - RabbitMQ: "OK, created!"
   - Declares: "Bind queue to exchange with key 'document.created'"
   - RabbitMQ: "OK, bound!"

2. **OCR Worker starts second**:
   - Connects to RabbitMQ
   - Declares: "I need exchange 'document.exchange'"
   - RabbitMQ: "Already exists! No problem, verified."
   - Declares: "I need queue 'document.created.queue'"
   - RabbitMQ: "Already exists! No problem, verified."
   - Declares: "Bind queue to exchange with key 'document.created'"
   - RabbitMQ: "Already bound! No problem, verified."

**Key Point**: RabbitMQ is **idempotent**. Declaring the same resource multiple times is safe and doesn't create duplicates.

---

## 🎓 Summary

### The Flow:
1. User uploads → REST Server receives
2. REST Server saves to database
3. REST Server publishes message to RabbitMQ
4. RabbitMQ routes message to queue
5. OCR Worker consumes message from queue
6. OCR Worker processes and sends acknowledgment

### Why Two Configs:
- Two **separate applications** (REST Server and OCR Worker)
- Each needs its **own Spring configuration**
- They **connect to the same** RabbitMQ queues (by name)
- RabbitMQ doesn't create duplicates - it's safe!

### Key Technologies:
- **Spring Boot**: Application framework
- **Spring AMQP**: RabbitMQ integration
- **RabbitTemplate**: Sends messages
- **@RabbitListener**: Receives messages
- **Jackson**: JSON serialization
- **RabbitMQ**: Message broker

### The Beauty:
- **Asynchronous**: Upload completes immediately
- **Decoupled**: REST Server doesn't know about OCR Worker
- **Scalable**: Add more OCR workers if needed
- **Reliable**: Messages wait in queue if worker is down
- **Maintainable**: Each service is independent

That's it! The system works through message passing via RabbitMQ queues. 🎉
