# Quick Start Guide - OCR Worker Implementation

## 🚀 Start the System

```bash
# From project root
docker-compose up --build
```

Wait for all services to start. You should see logs from:
- rest-server
- frontend
- postgres
- pgadmin
- rabbitmq
- **ocr-worker** ⭐

## ✅ Verify OCR Worker is Running

```bash
# Check if all containers are up
docker-compose ps

# Watch OCR worker logs
docker-compose logs -f ocr-worker
```

Expected output:
```
ocr-worker    | 🚀 Starting OCR Worker Application...
ocr-worker    | ✅ OCR Worker Application started successfully
```

## 📤 Test Document Upload

### Option 1: Using curl
```bash
curl -X POST http://localhost:8080/v1/documents \
  -F "file=@/path/to/your/document.pdf" \
  -F "title=Test Document"
```

### Option 2: Using PowerShell
```powershell
$file = [System.IO.File]::ReadAllBytes("path\to\document.pdf")
$boundary = [System.Guid]::NewGuid().ToString()
$headers = @{"Content-Type"="multipart/form-data; boundary=$boundary"}

Invoke-RestMethod -Uri "http://localhost:8080/v1/documents" `
  -Method POST -Headers $headers -InFile "path\to\document.pdf"
```

### Option 3: Using the test script
```bash
./test-ocr-worker.sh
```

## 👀 Watch the Magic Happen

After uploading, check OCR worker logs:
```bash
docker-compose logs -f ocr-worker
```

You should see:
```
📄 OCR WORKER RECEIVED: Document created - ID: xxx, Title: 'Test Document'
📋 Document Details:
   - Filename: document.pdf
   - Content Type: application/pdf
   - Size: 12345 bytes
   - Status: PENDING
🔄 Simulating OCR processing...
✅ OCR processing completed successfully for document: xxx
📤 Sent acknowledgment to queue
```

## 🐰 Check RabbitMQ

1. Open browser: http://localhost:15672
2. Login: `guest` / `guest`
3. Go to "Queues" tab
4. You should see:
   - `document.created.queue` ✅
   - `document.created.ack.queue` ✅
   - `document.deleted.queue` ✅

## 🔍 Troubleshooting

### OCR Worker Not Starting
```bash
# Check logs for errors
docker-compose logs ocr-worker

# Rebuild if needed
docker-compose up --build ocr-worker
```

### No Messages Being Received
```bash
# Check if RabbitMQ is running
docker-compose ps rabbitmq

# Check REST server logs
docker-compose logs rest

# Verify queue exists
# Visit http://localhost:15672 and check Queues tab
```

### Upload Fails
```bash
# Check if REST server is healthy
curl http://localhost:8080/v1/documents

# Check REST server logs
docker-compose logs rest
```

## 📊 Access Points

| Service | URL | Credentials |
|---------|-----|-------------|
| Frontend | http://localhost | - |
| REST API | http://localhost:8080/v1/documents | - |
| RabbitMQ UI | http://localhost:15672 | guest/guest |
| pgAdmin | http://localhost:5050 | admin@admin.com/adminpassword |

## 🛑 Stop the System

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

## 📝 Key Files

| File | Purpose |
|------|---------|
| `docker-compose.yml` | Service orchestration |
| `backend/src/main/java/fhtw/wien/service/DocumentService.java` | Publishes to RabbitMQ |
| `ocr-worker/src/main/java/fhtw/wien/ocrworker/messaging/OcrMessageConsumer.java` | Consumes messages |
| `OCR-WORKER-IMPLEMENTATION.md` | Detailed documentation |
| `test-ocr-worker.sh` | Automated test |

## ✨ What You Should See

1. **On Upload**: REST server saves document and publishes message
2. **In Queue**: Message appears in `document.created.queue`
3. **OCR Worker**: Receives and processes the message
4. **Acknowledgment**: Sends confirmation back to queue
5. **Complete**: All logs show successful processing

## 🎯 Success Criteria

- [x] REST server starts without errors
- [x] OCR worker starts and connects to RabbitMQ
- [x] Document upload succeeds (returns 201 Created)
- [x] Message appears in RabbitMQ queue
- [x] OCR worker logs show "RECEIVED" message
- [x] OCR worker logs show "processing completed"
- [x] Acknowledgment sent to queue

If all checkboxes pass, **Requirement 3 is successfully implemented!** ✅

## 📖 More Information

- See `OCR-WORKER-IMPLEMENTATION.md` for detailed architecture
- See `IMPLEMENTATION-SUMMARY.md` for changes overview
- See `ocr-worker/README.md` for OCR worker specifics
