# OCR Worker

Standalone worker that handles document.created events, runs OCR, and publishes the OCR result for summarization.

## Flow
1. REST backend saves the document and publishes `document.created` to `document.exchange`.
2. OCR worker consumes `document.created.queue`, performs OCR, optionally indexes text, and publishes the OCR result with routing key `ocr.completed`.

## Queues
- `document.created.queue` (consume)
- `ocr.completed.queue` (publish target for GenAI worker)

## Run (local)
```bash
cd ocr-worker
mvn spring-boot:run
```

## Run (docker-compose)
```bash
docker-compose up --build ocr-worker
```

## Config
`src/main/resources/application.properties` holds RabbitMQ connection settings.
