# GenAI Worker

This microservice generates document summaries using Google Gemini API after OCR completion.

## Purpose

When OCR processing is completed:
1. OCR worker publishes OCR completion message to RabbitMQ (`ocr.completed.queue`)
2. GenAI worker listens to the queue and processes the message
3. GenAI worker calls Google Gemini API to generate a summary
4. GenAI worker sends the summary back via RabbitMQ (`summary.result.queue`)
5. REST server receives the summary and stores it in the database

## Architecture

- **Separate Application**: Runs as a standalone Spring Boot application
- **Message Consumer**: Listens to `ocr.completed.queue` via RabbitMQ
- **GenAI Integration**: Calls Google Gemini API for text summarization
- **Asynchronous Processing**: Uses Spring @Async for non-blocking operations
- **Retry Logic**: Implements retry mechanism for transient failures
- **Error Handling**: Comprehensive error handling and logging

## Prerequisites

- Java 21
- Maven 3.9+
- RabbitMQ running
- **Google Gemini API Key** (required)

## Getting Your Gemini API Key

1. Go to [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Sign in with your Google account
3. Click "Create API Key"
4. Copy the API key

## Configuration

Configuration is in `src/main/resources/application.properties`:

```properties
# Google Gemini API Configuration
gemini.api.key=${GEMINI_API_KEY:}
gemini.api.url=https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent
gemini.model=gemini-pro
gemini.max.tokens=1000
gemini.temperature=0.7

# GenAI Configuration
genai.summary.max-input-length=50000
genai.summary.retry.max-attempts=3
genai.summary.retry.delay-ms=2000
genai.summary.timeout-seconds=30
```

## Running Locally

### Set API Key

**Windows (PowerShell):**
```powershell
$env:GEMINI_API_KEY="your-api-key-here"
```

**Linux/Mac:**
```bash
export GEMINI_API_KEY="your-api-key-here"
```

### Build
```bash
cd genai-worker
mvn clean package
```

### Run
```bash
java -jar target/GenAIWorker-1.0-SNAPSHOT.jar
```

Or use Maven:
```bash
mvn spring-boot:run
```

## Running with Docker

The GenAI worker is included in the main `docker-compose.yml`:

### 1. Create `.env` file in project root

```env
GEMINI_API_KEY=your-actual-api-key-here
```

### 2. Start all services

```bash
# From the project root
docker-compose up --build
```

The GenAI worker will automatically:
- Connect to RabbitMQ
- Listen for OCR completion events
- Generate summaries using Gemini API
- Send results back to the backend

## Message Flow

```
Document Upload
     ↓
OCR Worker processes document
     ↓
OCR Worker → RabbitMQ (ocr.completed message)
     ↓
GenAI Worker receives message
     ↓
GenAI Worker → Google Gemini API
     ↓
Gemini API returns summary
     ↓
GenAI Worker → RabbitMQ (summary.result message)
     ↓
Backend receives and saves summary
```

## Features

### 1. **Smart Text Processing**
   - Truncates long documents to fit API limits
   - Preserves sentence boundaries when truncating
   - Validates minimum text length before processing

### 2. **Retry Logic**
   - Automatically retries on transient failures
   - Configurable retry attempts and delays
   - Skips retry on client errors (4xx)

### 3. **Error Handling**
   - Catches and logs all exceptions
   - Sends failure messages back to backend
   - Graceful degradation on API failures

### 4. **Logging**
   - Comprehensive logging at all critical points
   - Emoji markers for easy visual scanning
   - Debug logs for troubleshooting

## API Limits

**Google Gemini Free Tier:**
- 60 requests per minute
- 50,000 characters max input
- 1,000 tokens max output

The worker automatically handles these limits through:
- Text truncation for long documents
- Retry logic for rate limiting
- Error handling for quota exceeded

## Testing

Upload a document via the REST API and watch the GenAI worker logs:

```bash
# Watch GenAI worker logs
docker-compose logs -f genai-worker

# Upload a document
curl -X POST http://localhost:8080/v1/documents \
  -F "file=@test.pdf" \
  -F "title=Test Document"
```

You should see:
1. OCR worker processes the document
2. GenAI worker receives OCR completion
3. GenAI worker calls Gemini API
4. Summary is generated and sent back
5. Backend saves the summary to database

## Troubleshooting

### "Gemini API key is not configured"
- Ensure `GEMINI_API_KEY` environment variable is set
- Check docker-compose.yml has `${GEMINI_API_KEY}` configured
- Verify .env file exists with API key

### "API rate limit exceeded"
- Wait a minute and try again
- Gemini free tier has 60 requests/minute limit
- Consider upgrading to paid tier for higher limits

### "Failed to parse Gemini API response"
- Check Gemini API status
- Verify API key is valid
- Review logs for detailed error message

## Monitoring

The GenAI worker logs include:
- 📨 Message received
- 📄 Document details
- 🤖 API request sent
- ✅ Summary generated
- 📤 Result sent to backend
- ❌ Errors and failures

## Future Enhancements

- Support for multiple languages
- Customizable summary length
- Summary quality scoring
- Caching frequently summarized content
- Support for other GenAI providers (OpenAI, Claude)
