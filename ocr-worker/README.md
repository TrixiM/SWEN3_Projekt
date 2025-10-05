# OCR Worker

This is a separate OCR worker application that processes document upload messages from RabbitMQ.

## Purpose

When a document is uploaded to the REST server:
1. The REST server saves the document to the database
2. The REST server publishes a message to RabbitMQ (`document.created.queue`)
3. This OCR worker listens to the queue and processes the message
4. The worker performs OCR processing (currently simulated)
5. The worker sends an acknowledgment back to the system

## Architecture

- **Separate Application**: This runs as a standalone Spring Boot application, independent from the REST server
- **Message Consumer**: Listens to `document.created.queue` via RabbitMQ
- **Processing**: Currently implements an "empty" OCR worker that logs messages and simulates processing
- **Acknowledgment**: Sends confirmation messages back via `document.created.ack` queue

## Running Locally

### Prerequisites
- Java 21
- Maven 3.9+
- RabbitMQ running (or use Docker Compose)

### Build
```bash
cd ocr-worker
mvn clean package
```

### Run
```bash
java -jar target/OCRWorker-1.0-SNAPSHOT.jar
```

Or use Maven:
```bash
mvn spring-boot:run
```

## Running with Docker

The OCR worker is included in the main `docker-compose.yml`:

```bash
# From the project root
docker-compose up --build
```

The OCR worker will automatically:
- Connect to RabbitMQ
- Listen for document created events
- Process messages and log the details

## Configuration

Configuration is in `src/main/resources/application.properties`:

```properties
spring.rabbitmq.host=rabbitmq
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

## Future Enhancements

This is currently an "empty" worker that simulates OCR processing. Future enhancements could include:

1. **Real OCR Integration**: 
   - Integrate Tesseract OCR or similar
   - Extract text from uploaded PDFs
   - Store extracted text in database

2. **Document Processing**:
   - Image preprocessing
   - Language detection
   - Confidence scoring

3. **Status Updates**:
   - Update document status (PENDING → PROCESSING → COMPLETED)
   - Handle failures and retries
   - Dead letter queue for failed messages

4. **Scalability**:
   - Multiple worker instances
   - Load balancing
   - Parallel processing

## Message Flow

```
Document Upload
     ↓
REST Server saves document
     ↓
REST Server → RabbitMQ (document.created message)
     ↓
OCR Worker receives message
     ↓
OCR Worker processes document
     ↓
OCR Worker → RabbitMQ (acknowledgment)
```

## Logs

The worker provides detailed logging:
- 📄 Document received
- 📋 Document details
- 🔄 Processing status
- ✅ Completion status
- 📤 Acknowledgment sent

## Testing

Upload a document via the REST API and watch the OCR worker logs:

```bash
# Watch OCR worker logs
docker-compose logs -f ocr-worker

# Upload a document
curl -X POST http://localhost:8080/v1/documents \
  -F "file=@test.pdf" \
  -F "title=Test Document"
```

You should see the OCR worker receive and process the message.
