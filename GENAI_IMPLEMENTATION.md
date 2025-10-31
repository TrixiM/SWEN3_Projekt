# GenAI Worker Implementation Guide

## 🎯 Overview

This document describes the complete implementation of the GenAI worker that generates document summaries using Google Gemini API after OCR completion.

## 🏗️ Architecture

```
┌─────────────────┐
│  User Uploads   │
│   PDF Document  │
└────────┬────────┘
         │
         ↓
┌─────────────────────────┐
│   Backend REST API      │
│   - Saves to MinIO      │
│   - Saves metadata      │
│   - Publishes message   │
└────────┬────────────────┘
         │
         ↓ document.created
┌─────────────────────────┐
│   OCR Worker            │
│   - Downloads PDF       │
│   - Extracts text       │
│   - Tesseract OCR       │
└────────┬────────────────┘
         │
         ↓ ocr.completed
┌─────────────────────────┐
│   GenAI Worker          │
│   - Receives OCR result │
│   - Calls Gemini API    │
│   - Generates summary   │
└────────┬────────────────┘
         │
         ↓ summary.result
┌─────────────────────────┐
│   Backend REST API      │
│   - Receives summary    │
│   - Saves to database   │
└─────────────────────────┘
```

## 📋 Implementation Checklist

### ✅ 1. GenAI Worker Service Created

**Files Created:**
- `genai-worker/pom.xml` - Maven configuration with Gemini dependencies
- `genai-worker/Dockerfile` - Multi-stage Docker build
- `genai-worker/README.md` - Comprehensive documentation
- `genai-worker/src/main/resources/application.properties` - Configuration
- `genai-worker/src/main/java/fhtw/wien/genaiworker/GenAIWorkerApplication.java` - Main application

### ✅ 2. Google Gemini API Integration

**Files Created:**
- `genai-worker/src/main/java/fhtw/wien/genaiworker/service/GeminiService.java`
  - REST API calls to Gemini
  - Text truncation for API limits
  - Response parsing
  - Error handling

**Features:**
- ✅ Configurable API key via environment variable
- ✅ Text truncation (50,000 character limit)
- ✅ Sentence boundary preservation
- ✅ Comprehensive error handling
- ✅ HTTP client error detection (4xx vs 5xx)

### ✅ 3. RabbitMQ Integration

**Queues Added:**
- `ocr.completed.queue` - OCR worker → GenAI worker
- `summary.result.queue` - GenAI worker → Backend

**Files Created:**
- `genai-worker/src/main/java/fhtw/wien/genaiworker/config/RabbitMQConfig.java`
- `genai-worker/src/main/java/fhtw/wien/genaiworker/messaging/GenAIMessageConsumer.java`

**Files Modified:**
- `backend/src/main/java/fhtw/wien/config/MessagingConstants.java` - Added queue constants
- `backend/src/main/java/fhtw/wien/config/RabbitMQConfig.java` - Added queue bindings
- `ocr-worker/src/main/java/fhtw/wien/ocrworker/config/RabbitMQConfig.java` - Added routing key

### ✅ 4. Async Processing with Retry Logic

**Files Created:**
- `genai-worker/src/main/java/fhtw/wien/genaiworker/service/SummarizationService.java`
  - @Async for non-blocking execution
  - Retry logic (configurable attempts and delays)
  - Skip retry on client errors (4xx)
  - CompletableFuture-based processing

**Configuration:**
```properties
genai.summary.retry.max-attempts=3
genai.summary.retry.delay-ms=2000
genai.summary.timeout-seconds=30
```

### ✅ 5. Backend Database Integration

**Files Modified:**
- `backend/src/main/java/fhtw/wien/domain/Document.java`
  - Added `summary` field (TEXT column)

**Files Created:**
- `backend/src/main/java/fhtw/wien/dto/SummaryResultDto.java` - DTO for receiving summaries

**Files Modified:**
- `backend/src/main/java/fhtw/wien/messaging/DocumentMessageConsumer.java`
  - Added `handleSummaryResult()` method
  - @Transactional summary updates
  - Error handling and logging

### ✅ 6. OCR Worker Updates

**Files Modified:**
- `ocr-worker/src/main/java/fhtw/wien/ocrworker/messaging/OcrMessageConsumer.java`
  - Publishes OCR completion messages with extracted text
  - Sends to `ocr.completed` routing key

- `ocr-worker/src/main/java/fhtw/wien/ocrworker/service/OcrProcessingService.java`
  - Returns `OcrResultDto` instead of `OcrAcknowledgment`
  - Includes full extracted text for summarization

### ✅ 7. Docker Compose Configuration

**File Modified:**
- `docker-compose.yml`
  - Added `genai-worker` service
  - Environment variable for `GEMINI_API_KEY`
  - Depends on RabbitMQ

### ✅ 8. DTOs Created

**GenAI Worker:**
- `OcrResultDto.java` - Receives OCR results
- `SummaryResultMessage.java` - Sends summary results

**Backend:**
- `SummaryResultDto.java` - Receives summary results

### ✅ 9. Exception Handling

**Files Created:**
- `genai-worker/src/main/java/fhtw/wien/genaiworker/exception/GenAIException.java`

**Error Handling:**
- ✅ API key validation
- ✅ Network errors (timeouts, connection failures)
- ✅ HTTP errors (4xx, 5xx)
- ✅ JSON parsing errors
- ✅ Empty or invalid responses
- ✅ Text validation (minimum length)

### ✅ 10. Comprehensive Logging

**Log Levels:**
- INFO: Major events (message received, API called, summary generated)
- DEBUG: Detailed processing info (text length, API parameters)
- WARN: Recoverable issues (retry attempts, text truncation)
- ERROR: Failures (API errors, processing failures)

**Emoji Markers:**
- 📨 Message received
- 📄 Document details
- 🤖 Gemini API operation
- ✅ Success
- ⚠️ Warning
- ❌ Error
- 📤 Message sent

## 🚀 Setup Instructions

### 1. Get Gemini API Key

1. Visit [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Sign in with Google account
3. Click "Create API Key"
4. Copy the key

### 2. Configure Environment

Create `.env` file in project root:

```env
GEMINI_API_KEY=your-actual-api-key-here
```

### 3. Start Services

```bash
docker-compose up --build
```

### 4. Test the Flow

```bash
# Upload a document
curl -X POST http://localhost:8080/v1/documents \
  -F "file=@test.pdf" \
  -F "title=Test Document"

# Watch logs
docker-compose logs -f genai-worker
```

## 📊 Message Flow

### Complete Sequence:

```
1. POST /v1/documents
   ↓
2. Backend saves to MinIO + PostgreSQL
   ↓
3. Backend publishes: document.created
   ↓
4. OCR Worker receives message
   ↓
5. OCR Worker processes PDF (Tesseract)
   ↓
6. OCR Worker publishes: ocr.completed
   ↓
7. GenAI Worker receives OCR result
   ↓
8. GenAI Worker calls Gemini API
   ↓
9. Gemini API returns summary
   ↓
10. GenAI Worker publishes: summary.result
    ↓
11. Backend receives summary
    ↓
12. Backend saves summary to database
    ↓
13. ✅ Complete!
```

## 🧪 Testing

### Unit Tests Created:
- `backend/src/test/java/fhtw/wien/business/PdfRenderingBusinessLogicTest.java`

### Integration Testing:
1. Upload a multi-page PDF document
2. Check OCR worker logs for text extraction
3. Check GenAI worker logs for summarization
4. Query database to verify summary saved
5. Use REST API to retrieve document with summary

### Expected Logs:

**OCR Worker:**
```
📄 OCR WORKER RECEIVED: Document created - ID: abc123
🔄 Starting OCR processing...
✅ OCR processing completed: 1,234 characters from 3 pages
📤 Sent OCR completion message to GenAI worker
```

**GenAI Worker:**
```
📨 GENAI WORKER RECEIVED: OCR completed for document: abc123
📄 Document has 1,234 characters from 3 pages
🤖 Generating summary using Google Gemini API...
✅ Summary generated successfully in 2,345ms
📤 Sent summary result to backend
```

**Backend:**
```
📨 BACKEND RECEIVED: Summary result for document ID: abc123
✅ Summary saved for document: abc123 (length: 156 characters)
```

## ⚙️ Configuration

### GenAI Worker (`application.properties`):

```properties
# Gemini API
gemini.api.key=${GEMINI_API_KEY:}
gemini.api.url=https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent
gemini.model=gemini-pro
gemini.max.tokens=1000
gemini.temperature=0.7

# Retry Configuration
genai.summary.max-input-length=50000
genai.summary.retry.max-attempts=3
genai.summary.retry.delay-ms=2000
```

### Docker Compose:

```yaml
genai-worker:
  build:
    context: ./genai-worker
    dockerfile: Dockerfile
  environment:
    RABBITMQ_HOST: rabbitmq
    GEMINI_API_KEY: ${GEMINI_API_KEY}
```

## 🔒 Security

- ✅ API key via environment variable (not hardcoded)
- ✅ Text truncation prevents excessive API usage
- ✅ Input validation before API calls
- ✅ Error messages don't expose sensitive data

## 📈 Performance

- ✅ Async processing (non-blocking)
- ✅ Configurable retry logic
- ✅ Smart text truncation at sentence boundaries
- ✅ Connection pooling via RestTemplate

## 🐛 Troubleshooting

### "Gemini API key is not configured"
- Set `GEMINI_API_KEY` environment variable
- Check `.env` file exists in project root
- Verify docker-compose.yml has `${GEMINI_API_KEY}`

### "API rate limit exceeded"
- Gemini free tier: 60 requests/minute
- Wait 1 minute before retrying
- Consider upgrading to paid tier

### "Empty response from Gemini API"
- Check Gemini API status
- Verify API key is valid
- Check input text is not empty

## 📚 Additional Resources

- [Google Gemini API Documentation](https://ai.google.dev/docs)
- [GenAI Worker README](genai-worker/README.md)
- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)

## ✨ Features Summary

| Feature | Status | Description |
|---------|--------|-------------|
| Google Gemini Integration | ✅ | Calls Gemini API for summarization |
| RabbitMQ Messaging | ✅ | Event-driven architecture |
| Async Processing | ✅ | Non-blocking operations |
| Retry Logic | ✅ | Handles transient failures |
| Error Handling | ✅ | Comprehensive error management |
| Logging | ✅ | Detailed logs at all levels |
| Database Integration | ✅ | Saves summaries to PostgreSQL |
| Docker Support | ✅ | Containerized deployment |
| Configuration | ✅ | Environment-based config |
| Documentation | ✅ | README and inline docs |

## 🎉 Conclusion

The GenAI worker is now fully integrated into your document management system. Documents uploaded through the REST API will automatically:

1. Be OCR processed to extract text
2. Have summaries generated via Google Gemini
3. Be stored in the database with searchable summaries

All processing happens asynchronously with proper error handling, logging, and retry logic!
