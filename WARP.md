# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Prerequisites
- Java 21
- Docker and Docker Compose
- For GenAI worker: `GEMINI_API_KEY` must be available (either as an environment variable or via a `.env` file used by `docker-compose`).

## Common commands

### Backend (REST API, `backend/`)

From the repo root:
- Enter module:
  - `cd backend`

Build and run:
- docker compose up --build -d
  - The REST API is exposed on `http://localhost:8080`.

Tests (see `backend/TEST_README.md` for full 

### OCR worker (`ocr-worker/`)

From the repo root:
- Enter module:
  - `cd ocr-worker`





### GenAI worker (`genai-worker/`)

From the repo root:
- Enter module:
  - `cd genai-worker`

Set Gemini API key (example for PowerShell on Windows):
- `$env:GEMINI_API_KEY="your-api-key-here"`


  - `mvn clean test`

### Frontend (`frontend/`)

The frontend is a static dashboard/details UI served via Nginx.

From the repo root:
- Build Docker image (also used in CI):
  - `docker build -f frontend/Dockerfile -t dms-frontend:latest ./frontend`

During development you can also open `frontend/paperless-ui/index.html` directly in a browser for quick UI iteration; API requests expect the backend at `http://localhost:8080` (in Docker, Nginx proxies to the `rest` service via the shared network).

### Full stack via Docker Compose

From the repo root:
- (Optional, but recommended) create `.env` with your Gemini key:
  - `GEMINI_API_KEY=your-actual-api-key-here`
- Start all services (REST backend, frontend, workers, and infra):
  - `docker-compose up --build`

This brings up:
- `rest` (backend Spring Boot service)
- `frontend` (Nginx-served UI on port 80)
- `ocr-worker`
- `genai-worker`
- `postgres` + `pgadmin`
- `rabbitmq`
- `minio`
- `elasticsearch`

To follow logs for a specific service, for example the GenAI worker:
- `docker-compose logs -f genai-worker`

## High-level architecture

### Overview

This repository implements a document management system composed of:
- A REST backend (`backend/`) that handles document lifecycle, metadata, binary storage, search, and analytics.
- An OCR worker (`ocr-worker/`) that consumes document creation events, performs OCR, indexes content into Elasticsearch, and forwards OCR results for summarization.
- A GenAI worker (`genai-worker/`) that consumes OCR completion events, calls the Google Gemini API to generate summaries, and sends summary results back to the backend.
- A static frontend (`frontend/`) served via Nginx, providing a dashboard and document details view, talking to the REST backend.
- Supporting infrastructure (via `docker-compose.yml`): PostgreSQL, RabbitMQ, MinIO, Elasticsearch, and pgAdmin.

All services are loosely coupled via RabbitMQ and share storage via PostgreSQL (metadata), MinIO (PDF binaries), and Elasticsearch (full-text index).

### Backend service (`backend/`)

Layers and packages:
- `controller/`
  - HTTP layer exposing REST endpoints under `/v1/documents` and related paths.
  - Example: `DocumentController` handles multipart uploads, validation, and delegates to `DocumentService`.
- `business/`
  - Orchestrates higher-level workflows that span multiple services/repositories.
  - `DocumentBusinessLogic` coordinates document creation, MinIO storage, and messaging.
  - `PdfRenderingBusinessLogic` handles PDF-specific concerns such as page rendering and page count.
- `service/`
  - Core application services for domain operations and integrations:
    - `DocumentService` encapsulates CRUD and binary content retrieval through MinIO.
    - `DocumentAnalyticsService` computes/aggregates analytics for documents.
    - `DocumentSearchService` queries Elasticsearch using `ElasticsearchOperations` for text search and fuzzy search.
    - `MinIOStorageService` wraps MinIO client operations and is protected by Resilience4j (circuit breaker, retry, bulkhead, time limiter).
    - `IdempotencyService` provides idempotency guarantees for message processing across services.
- `domain/`
  - JPA entities (e.g., `Document`, `DocumentAnalytics`, `DocumentStatus`) mapped to PostgreSQL tables.
- `repo/` and `repository/`
  - Spring Data JPA repositories for relational data (`DocumentRepo`, `DocumentAnalyticsRepository`).
- `elasticsearch/`
  - Elasticsearch index model and repository:
    - `DocumentIndex` represents the `documents` index with fields like `content`, `title`, counts, language, confidence, and timestamps.
    - `DocumentSearchRepository` provides Spring Data access to the index.
- `dto/`
  - Transport data structures for REST and messaging (e.g., `DocumentResponse`, `DocumentAnalyticsDto`, `SummaryResultDto`, `DocumentSearchDto`).
- `messaging/`
  - RabbitMQ producers and consumers tying the backend into the worker pipelines:
    - A producer publishes `document.created` events when new documents are stored.
    - `DocumentMessageConsumer` listens on the summary result queue (configured via `MessagingConstants.SUMMARY_RESULT_QUEUE`), uses `IdempotencyService` to deduplicate, and persists generated summaries back onto corresponding `Document` entities.
- `config/`
  - Spring configuration (JPA config, RabbitMQ exchanges/queues/routing keys, shared messaging constants).
- `exception/`
  - Domain-specific exception types plus a global `ErrorHandler` that maps them to HTTP responses.
- `health/`
  - Custom Spring Boot Actuator health indicators (e.g., `MinIOHealthIndicator`).
- `util/`
  - Utilities such as `DocumentMapper` (entity ↔ DTO mapping) and `PdfValidator`.
- `migrations/`
  - SQL migration scripts (`V1__init.sql`, `V2__add_pdf_data.sql`, `V3__remove_pdf_data.sql`) defining and evolving the relational schema.

Configuration highlights (`backend/src/main/resources/application.properties`):
- Listens on port `8080`.
- Connects to PostgreSQL via `spring.datasource.*` (defaults point to the `postgres` container).
- Uses MinIO for PDF storage (endpoint/credentials are injected via environment variables in Docker).
- Connects to Elasticsearch (`spring.elasticsearch.uris=http://elasticsearch:9200`) for indexing/search.
- Configures detailed logging and Resilience4j for MinIO interactions.
- Exposes health/ready endpoints through Spring Boot Actuator, used by container health checks.

### OCR worker (`ocr-worker/`)

Responsibilities:
- Listens to document creation events from RabbitMQ and performs OCR on newly stored documents.
- Fetches source PDFs from MinIO, converts pages to images as needed, and runs Tesseract OCR.
- Indexes successful OCR results into Elasticsearch.
- Emits OCR completion messages for the GenAI worker.

Key structure:
- `OcrWorkerApplication` is a standalone Spring Boot app (main class configured in the module `pom.xml`).
- `messaging/`
  - `OcrMessageConsumer` listens on `document.created.queue` (via `RabbitMQConfig.DOCUMENT_CREATED_QUEUE`).
  - For each message (`DocumentResponse`), it:
    - Performs an idempotency check via `IdempotencyService`.
    - Calls `OcrProcessingService` to run the OCR pipeline.
    - Indexes successful OCR results into Elasticsearch via `ElasticsearchService`.
    - Sends `OcrResultDto` to the `document.exchange` with routing key `ocr.completed` for the GenAI worker.
- `service/`
  - `OcrProcessingService` orchestrates file retrieval from MinIO, PDF conversion (`PdfConverterService`), and OCR (`TesseractOcrService`, `UnifiedOcrService`).
  - `MinIOClientService` encapsulates object storage access and is guarded by Resilience4j.
  - `IdempotencyService` ensures each document is processed at most once per pipeline step.
- `elasticsearch/`
  - `DocumentIndex` / `DocumentIndexRepository` and `ElasticsearchService` manage text indexing of OCR results.
- `util/`
  - `FileTypeDetector` and other helpers around document formats and processing.
- `domain/`
  - Local `DocumentStatus` enum (OCR-specific processing states).

Configuration (`ocr-worker/src/main/resources/application.properties`):
- Connects to RabbitMQ (`rabbitmq` host inside Docker).
- Uses Elasticsearch for indexing.
- Configures Resilience4j (circuit breaker and retry) for MinIO operations.

### GenAI worker (`genai-worker/`)

Responsibilities:
- Consumes OCR completion messages from RabbitMQ.
- Calls the Google Gemini API (via Vertex AI client) to generate document summaries.
- Emits summary result messages back to RabbitMQ for the backend to consume and persist.

Key structure:
- `GenAIWorkerApplication` is a standalone Spring Boot app with `@EnableAsync` for non-blocking processing.
- `service/`
  - `GeminiService` encapsulates HTTP/Vertex AI client calls to Gemini.
  - `SummarizationService` coordinates message handling, text truncation, retry behavior, and result emission.
  - `IdempotencyService` again provides deduplication for message processing.
- `config/`
  - `GenAIConfig` for Gemini and summarization settings.
  - `RabbitMQConfig` for GenAI-specific queues and exchanges.
- `dto/`
  - Transport types for OCR results (`OcrResultDto`), Gemini responses, and summary result messages.
- `health/`
  - `GeminiHealthIndicator` integrates external API health into Spring Boot Actuator.

Configuration highlights (`genai-worker/src/main/resources/application.properties`):
- Runs on port `8083`.
- Reads RabbitMQ connection info from `RABBITMQ_*` environment variables.
- Reads Gemini API configuration from `GEMINI_API_KEY` and related properties.
- Uses Resilience4j for circuit breaking, retries, rate limiting, and time limiting around Gemini API calls.

### Frontend (`frontend/`)

- Static UI under `frontend/paperless-ui/` (HTML, CSS, JavaScript).
- Nginx (`frontend/nginx.conf`) serves the static assets on port `80` and proxies API requests to the backend REST service over the shared Docker network.
- JavaScript modules (e.g., `DocumentManager.js`, `docDetails.js`, `utils.js`) handle:
  - Document listing and detail retrieval via the REST API.
  - Rendering previews and invoking endpoints for pages/content.

### Messaging and data flow (end-to-end)

At a high level, the system behaves as follows:
1. A client uploads a PDF through the backend (`POST /v1/documents`).
2. The backend:
   - Stores metadata in PostgreSQL (via JPA entities/repositories).
   - Uploads the PDF to MinIO via `MinIOStorageService`.
   - Publishes a `document.created` message to RabbitMQ (exchange and routing configured in backend `RabbitMQConfig`).
3. The OCR worker consumes `document.created` messages, retrieves the PDF from MinIO, runs OCR, indexes the extracted text into Elasticsearch, and publishes an `ocr.completed` message containing OCR results.
4. The GenAI worker consumes `ocr.completed` messages, calls the Gemini API to summarize the extracted text, and publishes a summary result message to a `summary.result` queue.
5. The backend listens on the summary result queue via `DocumentMessageConsumer`, applies idempotency checks, and persists the summary text onto the corresponding `Document` record.
6. The frontend queries the backend for document lists, details, previews, analytics, and search results (backed by Elasticsearch) to render the user-facing UI.

This separation into REST backend, OCR worker, and GenAI worker allows each service to be developed, scaled, and deployed independently while coordinating via RabbitMQ and shared storage (PostgreSQL, MinIO, Elasticsearch).