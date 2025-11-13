# System Architecture

This document provides detailed architecture diagrams and explanations for the Document Management System.

## Table of Contents

- [System Context](#system-context)
- [Container Diagram](#container-diagram)
- [Component Diagram](#component-diagram)
- [Deployment Diagram](#deployment-diagram)
- [Sequence Diagrams](#sequence-diagrams)
- [Database Schema](#database-schema)
- [Message Flow](#message-flow)

## System Context

### High-Level Overview

```
┌────────────────────────────────────────────────────────────────┐
│                                                                │
│                    Document Management System                  │
│                                                                │
│  Upload documents, search content, generate summaries,         │
│  manage documents with OCR and AI capabilities                 │
│                                                                │
└────────────────────────────────────────────────────────────────┘
                          ▲         ▲
                          │         │
                          │         │
                    ┌─────┴──┐  ┌──┴────────┐
                    │  User  │  │ Developer │
                    │(Browser)│  │   (API)   │
                    └────────┘  └───────────┘
                                     │
                          ┌──────────┴──────────┐
                          │                     │
                    ┌─────▼──────┐      ┌──────▼──────┐
                    │  Google    │      │  Tesseract  │
                    │  Gemini    │      │     OCR     │
                    │    API     │      │   Engine    │
                    └────────────┘      └─────────────┘
```

### External Systems

| System | Purpose | Communication |
|--------|---------|---------------|
| **User Browser** | Access web interface | HTTP/HTTPS |
| **Google Gemini API** | AI text summarization | REST API (HTTPS) |
| **Tesseract OCR** | Text extraction from PDFs | Library integration |
| **Developer Tools** | API testing, monitoring | REST API |

## Container Diagram

### Architecture Components

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Browser (User)                               │
└────────────┬────────────────────────────────────────────────────────┘
             │ HTTP
             ▼
┌────────────────────────────────────────────────────────────────────┐
│                      Frontend Container                            │
│                  [Nginx + Static HTML/CSS/JS]                      │
│                                                                    │
│  • Document listing UI                                            │
│  • Upload interface                                               │
│  • Document details view                                          │
└────────────┬───────────────────────────────────────────────────────┘
             │ REST API (JSON)
             ▼
┌────────────────────────────────────────────────────────────────────┐
│                      Backend Container                             │
│                   [Spring Boot REST API]                           │
│                                                                    │
│  • REST Controllers                                               │
│  • Business Logic                                                 │
│  • Service Layer                                                  │
│  • Repository Layer                                               │
│  • Message Producers/Consumers                                    │
└─────┬────────┬────────────┬────────────┬────────────┬─────────────┘
      │        │            │            │            │
      │        │            │            │            │
  ┌───▼──┐ ┌──▼─────┐  ┌───▼────┐  ┌───▼────┐  ┌───▼────────┐
  │ Post-│ │ Rabbit │  │ MinIO  │  │Elastic-│  │   Google   │
  │ greSQL│ │   MQ   │  │ Object │  │ search │  │   Gemini   │
  │  DB  │ │ Broker │  │Storage │  │ Engine │  │    API     │
  └──────┘ └──┬─────┘  └────────┘  └────┬───┘  └────────────┘
             │                           │
             │                           │
        ┌────▼────┐                 ┌────▼────┐
        │   OCR   │                 │  GenAI  │
        │ Worker  │─────────────────│  Worker │
        │Container│   RabbitMQ      │Container│
        └─────────┘                 └─────────┘
```

### Container Responsibilities

#### 1. Frontend Container
- **Technology**: Nginx, HTML5, CSS3, JavaScript
- **Responsibilities**:
  - Serve static web pages
  - Client-side document management
  - User interface for uploads/downloads
- **Ports**: 80 (HTTP)

#### 2. Backend Container
- **Technology**: Spring Boot 3.3.3, Java 21
- **Responsibilities**:
  - REST API endpoints
  - Business logic orchestration
  - Database operations
  - Message queue coordination
  - File storage management
- **Ports**: 8080 (HTTP)
- **Key Components**:
  - Controllers (REST endpoints)
  - Services (business logic)
  - Repositories (data access)
  - Messaging (RabbitMQ integration)

#### 3. OCR Worker Container
- **Technology**: Spring Boot, Tesseract OCR
- **Responsibilities**:
  - Consume document creation messages
  - Extract text from PDFs
  - Index content in Elasticsearch
  - Publish OCR completion events
- **Scaling**: Horizontal (multiple instances)

#### 4. GenAI Worker Container
- **Technology**: Spring Boot, Google Gemini API
- **Responsibilities**:
  - Consume OCR completion messages
  - Generate document summaries
  - Publish summary results
- **Scaling**: Horizontal (multiple instances)

#### 5. PostgreSQL Database
- **Technology**: PostgreSQL 16
- **Data**:
  - Document metadata
  - Tags
  - Analytics
  - Version information

#### 6. RabbitMQ Message Broker
- **Technology**: RabbitMQ 3
- **Queues**:
  - `document.created.queue`
  - `ocr.completed.queue`
  - `summary.result.queue`
  - Dead letter queues

#### 7. MinIO Object Storage
- **Technology**: MinIO (S3-compatible)
- **Data**: PDF file storage

#### 8. Elasticsearch
- **Technology**: Elasticsearch 8.11
- **Data**: Indexed document content for search

## Component Diagram

### Backend Components

```
┌────────────────────────────────────────────────────────────────┐
│                       Backend Service                          │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │                    REST Layer                            │ │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐         │ │
│  │  │ Document   │  │  Search    │  │ Analytics  │         │ │
│  │  │ Controller │  │ Controller │  │ Controller │         │ │
│  │  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘         │ │
│  └────────┼───────────────┼───────────────┼────────────────┘ │
│           │               │               │                  │
│  ┌────────┼───────────────┼───────────────┼────────────────┐ │
│  │        │    Business Logic Layer       │                │ │
│  │  ┌─────▼──────┐  ┌──────────────┐  ┌──▼────────────┐   │ │
│  │  │ Document   │  │     PDF      │  │   Analytics   │   │ │
│  │  │ Business   │  │  Rendering   │  │   Business    │   │ │
│  │  │   Logic    │  │   Business   │  │    Logic      │   │ │
│  │  └─────┬──────┘  └──────┬───────┘  └───┬───────────┘   │ │
│  └────────┼────────────────┼──────────────┼───────────────┘ │
│           │                │              │                 │
│  ┌────────┼────────────────┼──────────────┼───────────────┐ │
│  │        │     Service Layer             │               │ │
│  │  ┌─────▼──────┐  ┌──────▼───────┐  ┌──▼────────────┐  │ │
│  │  │ Document   │  │    MinIO     │  │   Search      │  │ │
│  │  │  Service   │  │   Storage    │  │   Service     │  │ │
│  │  │            │  │   Service    │  │               │  │ │
│  │  └─────┬──────┘  └──────┬───────┘  └───┬───────────┘  │ │
│  └────────┼────────────────┼──────────────┼──────────────┘ │
│           │                │              │                │
│  ┌────────┼────────────────┼──────────────┼──────────────┐ │
│  │        │    Data Access Layer          │              │ │
│  │  ┌─────▼──────┐  ┌──────────────┐  ┌──▼───────────┐  │ │
│  │  │ Document   │  │   Analytics  │  │Elasticsearch │  │ │
│  │  │ Repository │  │  Repository  │  │  Repository  │  │ │
│  │  └─────┬──────┘  └──────┬───────┘  └───┬──────────┘  │ │
│  └────────┼────────────────┼──────────────┼─────────────┘ │
│           │                │              │               │
│  ┌────────┼────────────────┼──────────────┼─────────────┐ │
│  │        │    Messaging Layer            │             │ │
│  │  ┌─────▼──────────┐  ┌─────────────────▼──────────┐  │ │
│  │  │    Document    │  │      OCR Completion        │  │ │
│  │  │    Message     │  │       Consumer             │  │ │
│  │  │    Producer    │  │                            │  │ │
│  │  └────────────────┘  └────────────────────────────┘  │ │
│  └─────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────┘
```

### Key Design Patterns

#### 1. Layered Architecture
- **Presentation Layer**: REST Controllers
- **Business Logic Layer**: Business Logic classes
- **Service Layer**: Service classes
- **Data Access Layer**: Repositories

#### 2. Repository Pattern
- Abstract data access
- JPA repositories for PostgreSQL
- Elasticsearch repositories for search

#### 3. Service Agent Pattern
- `MinIOStorageService`: External storage
- `ElasticsearchService`: Search operations
- `GeminiService`: AI integration

#### 4. Facade Pattern
- Business Logic classes act as facades
- Coordinate multiple services
- Handle complex workflows

#### 5. Message-Driven Architecture
- Asynchronous processing
- Loose coupling between components
- Scalable worker pattern

## Deployment Diagram

### Docker Compose Deployment

```
┌─────────────────────────────────────────────────────────────────┐
│                      Docker Host                                │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                  app-network (bridge)                   │   │
│  │                                                         │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐             │   │
│  │  │ Frontend │  │ Backend  │  │   OCR    │             │   │
│  │  │  :80     │  │  :8080   │  │  Worker  │             │   │
│  │  └──────────┘  └──────────┘  └──────────┘             │   │
│  │                                                         │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐             │   │
│  │  │  GenAI   │  │ Postgres │  │ RabbitMQ │             │   │
│  │  │  Worker  │  │  :5432   │  │:5672/15672│            │   │
│  │  └──────────┘  └──────────┘  └──────────┘             │   │
│  │                                                         │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐             │   │
│  │  │  MinIO   │  │Elastic-  │  │ PgAdmin  │             │   │
│  │  │:9000/9001│  │  search  │  │  :5050   │             │   │
│  │  │          │  │  :9200   │  │          │             │   │
│  │  └──────────┘  └──────────┘  └──────────┘             │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    Volumes                              │   │
│  │  • postgres_data                                        │   │
│  │  • minio_data                                           │   │
│  │  • elasticsearch_data                                   │   │
│  │  • pgadmin_data                                         │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### Kubernetes Deployment

```
┌──────────────────────────────────────────────────────────────┐
│                   Kubernetes Cluster                         │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                 Namespace: dms                         │ │
│  │                                                        │ │
│  │  ┌──────────────────────────────────────────────────┐ │ │
│  │  │              Ingress Controller                  │ │ │
│  │  │         (nginx-ingress / LoadBalancer)           │ │ │
│  │  └───────────────────┬──────────────────────────────┘ │ │
│  │                      │                                │ │
│  │      ┌───────────────┼───────────────┐                │ │
│  │      │               │               │                │ │
│  │  ┌───▼────┐      ┌───▼────┐     ┌───▼────┐           │ │
│  │  │Frontend│      │Backend │     │Backend │           │ │
│  │  │  Pod   │      │  Pod 1 │     │  Pod 2 │           │ │
│  │  │        │      │        │     │        │           │ │
│  │  └────────┘      └───┬────┘     └───┬────┘           │ │
│  │                      │              │                │ │
│  │                      └──────┬───────┘                │ │
│  │                             │                        │ │
│  │      ┌──────────────────────┼────────────────┐       │ │
│  │      │                      │                │       │ │
│  │  ┌───▼────┐  ┌──────────┐  │  ┌──────────┐  │       │ │
│  │  │  OCR   │  │  GenAI   │  │  │Analytics │  │       │ │
│  │  │Worker 1│  │ Worker 1 │  │  │ Worker   │  │       │ │
│  │  └────────┘  └──────────┘  │  └──────────┘  │       │ │
│  │  ┌────────┐  ┌──────────┐  │                │       │ │
│  │  │  OCR   │  │  GenAI   │  │                │       │ │
│  │  │Worker 2│  │ Worker 2 │  │                │       │ │
│  │  └────────┘  └──────────┘  │                │       │ │
│  │      │           │          │                │       │ │
│  │      │  ┌────────┼──────────┼────────┐       │       │ │
│  │      │  │        │          │        │       │       │ │
│  │  ┌───▼──▼──┐  ┌─▼──────┐  ┌▼────────▼─┐  ┌──▼────┐  │ │
│  │  │RabbitMQ │  │ MinIO  │  │ Postgres  │  │Elastic│  │ │
│  │  │ Service │  │Service │  │  Service  │  │search │  │ │
│  │  │         │  │        │  │           │  │Service│  │ │
│  │  └─────────┘  └────────┘  └───────────┘  └───────┘  │ │
│  │      │            │             │            │       │ │
│  │  ┌───▼────┐   ┌──▼─────┐   ┌───▼────┐   ┌──▼────┐  │ │
│  │  │ Queue  │   │ MinIO  │   │Postgres│   │ ES    │  │ │
│  │  │  PVC   │   │  PVC   │   │  PVC   │   │ PVC   │  │ │
│  │  └────────┘   └────────┘   └────────┘   └───────┘  │ │
│  └────────────────────────────────────────────────────┘ │ │
└──────────────────────────────────────────────────────────┘

HPA: Horizontal Pod Autoscaler (OCR & GenAI Workers)
PVC: Persistent Volume Claims
```

## Sequence Diagrams

### Document Upload Flow

```
User      Frontend    Backend     MinIO      Postgres   RabbitMQ    OCR       GenAI
 │           │           │          │           │          │        Worker    Worker
 │ Upload    │           │          │           │          │          │         │
 ├──────────>│           │          │           │          │          │         │
 │           │ POST /api/│          │           │          │          │         │
 │           │  documents│          │           │          │          │         │
 │           ├──────────>│          │           │          │          │         │
 │           │           │ Upload   │           │          │          │         │
 │           │           │   PDF    │           │          │          │         │
 │           │           ├─────────>│           │          │          │         │
 │           │           │<─────────┤           │          │          │         │
 │           │           │ objectKey│           │          │          │         │
 │           │           │          │           │          │          │         │
 │           │           │   Save   │           │          │          │         │
 │           │           │ metadata │           │          │          │         │
 │           │           ├──────────┼──────────>│          │          │         │
 │           │           │<─────────┼───────────┤          │          │         │
 │           │           │          │   saved   │          │          │         │
 │           │           │          │           │          │          │         │
 │           │           │    Publish message   │          │          │         │
 │           │           ├──────────┼───────────┼─────────>│          │         │
 │           │<──────────┤          │           │          │          │         │
 │<──────────┤  200 OK   │          │           │          │          │         │
 │           │  {doc}    │          │           │          │          │         │
 │           │           │          │           │    Consume         │          │
 │           │           │          │           │<─────────────────>│          │
 │           │           │          │           │          │    Extract text     │
 │           │           │          │    Download PDF      │          │         │
 │           │           │<─────────┼───────────┼──────────┤          │         │
 │           │           │          │  PDF data │          │          │         │
 │           │           │          │           │    Index to ES      │         │
 │           │           │          │           │          │──────>│  │         │
 │           │           │          │           │    Publish OCR done │         │
 │           │           │          │           │<─────────┤          │         │
 │           │           │          │           │          │     Consume        │
 │           │           │          │           │          │<──────────────────>│
 │           │           │          │           │          │          │ Generate│
 │           │           │          │           │          │          │ Summary │
 │           │           │          │           │          │          │    │    │
 │           │           │          │           │    Publish summary   │    │    │
 │           │           │          │           │<────────────────────────┘    │
 │           │           │   Consume & Update   │          │          │         │
 │           │           │<─────────┼───────────┤          │          │         │
 │           │           │  Update  │           │          │          │         │
 │           │           │ summary  │           │          │          │         │
 │           │           ├──────────┼──────────>│          │          │         │
 │           │           │          │   saved   │          │          │         │
```

### Search Flow

```
User      Frontend    Backend    Elasticsearch
 │           │           │              │
 │  Search   │           │              │
 │  "query"  │           │              │
 ├──────────>│           │              │
 │           │ GET /api/ │              │
 │           │ documents/│              │
 │           │search?q=..│              │
 │           ├──────────>│              │
 │           │           │ Search query │
 │           │           ├─────────────>│
 │           │           │<─────────────┤
 │           │           │   Results    │
 │           │           │              │
 │           │           │ Get docs by  │
 │           │           │ IDs (Postgres)│
 │           │           │              │
 │           │<──────────┤              │
 │<──────────┤ Results   │              │
 │  Display  │ with full │              │
 │           │  metadata │              │
```

## Database Schema

### Entity Relationship Diagram

```
┌─────────────────────────────────┐
│         documents               │
├─────────────────────────────────┤
│ id (UUID) PK                    │
│ title (VARCHAR)                 │
│ original_filename (VARCHAR)     │
│ content_type (VARCHAR)          │
│ size_bytes (BIGINT)             │
│ bucket (VARCHAR)                │
│ object_key (TEXT)               │
│ storage_uri (TEXT)              │
│ checksum_sha256 (VARCHAR)       │
│ pdf_data (BYTEA) [nullable]     │
│ status (VARCHAR)                │
│ summary (TEXT)                  │
│ version (INT)                   │
│ created_at (TIMESTAMP)          │
│ updated_at (TIMESTAMP)          │
└────────────┬────────────────────┘
             │
             │ 1:N
             │
┌────────────▼────────────────────┐
│      document_tags              │
├─────────────────────────────────┤
│ document_id (UUID) FK           │
│ tag (VARCHAR)                   │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│    document_analytics           │
├─────────────────────────────────┤
│ id (UUID) PK                    │
│ document_id (UUID) FK           │
│ character_count (BIGINT)        │
│ word_count (BIGINT)             │
│ page_count (INT)                │
│ avg_confidence (DOUBLE)         │
│ detected_language (VARCHAR)     │
│ quality_score (DOUBLE)          │
│ created_at (TIMESTAMP)          │
└────────────┬────────────────────┘
             │
             │ N:1
             │
        ┌────▼──────────┐
        │   documents   │
        └───────────────┘
```

### Elasticsearch Index Structure

```json
{
  "document_index": {
    "mappings": {
      "properties": {
        "documentId": { "type": "keyword" },
        "title": { "type": "text" },
        "extractedText": { "type": "text" },
        "pageCount": { "type": "integer" },
        "averageConfidence": { "type": "double" },
        "detectedLanguage": { "type": "keyword" },
        "indexedAt": { "type": "date" }
      }
    }
  }
}
```

## Message Flow

### RabbitMQ Queue Structure

```
┌────────────────────────────────────────────────────────────────┐
│                      RabbitMQ Exchange                         │
│                   (document.exchange)                          │
└──────────┬──────────────────┬────────────────────┬────────────┘
           │                  │                    │
           │                  │                    │
   ┌───────▼───────┐   ┌──────▼────────┐   ┌──────▼───────────┐
   │document.created│   │ocr.completed  │   │summary.result    │
   │    .queue      │   │    .queue     │   │    .queue        │
   └───────┬────────┘   └──────┬────────┘   └──────┬───────────┘
           │                   │                    │
           │                   │                    │
   ┌───────▼────────┐   ┌──────▼────────┐   ┌──────▼───────────┐
   │  OCR Worker    │   │ GenAI Worker  │   │    Backend       │
   │   Consumer     │   │   Consumer    │   │   Consumer       │
   └────────────────┘   └───────────────┘   └──────────────────┘
```

### Dead Letter Queue Configuration

```
                         Main Queue
                             │
                    ┌────────▼─────────┐
                    │  Message fails   │
                    │  after retries   │
                    └────────┬─────────┘
                             │
                    ┌────────▼─────────┐
                    │  DLQ Exchange    │
                    │(document.exchange│
                    │     .dlx)        │
                    └────────┬─────────┘
                             │
                    ┌────────▼─────────┐
                    │ document.created │
                    │   .queue.dlq     │
                    └──────────────────┘
```

## Technology Stack Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    Frontend Stack                       │
│  HTML5 • CSS3 • Vanilla JavaScript • Nginx              │
└─────────────────────────────────────────────────────────┘
                          │
                          │ REST API
                          ▼
┌─────────────────────────────────────────────────────────┐
│                    Backend Stack                        │
│  Java 21 • Spring Boot 3.3.3 • Spring Data JPA          │
│  Spring AMQP • Resilience4j • Lombok                    │
└─────────────────────────────────────────────────────────┘
            │              │              │
    ┌───────┼──────────────┼──────────────┼────────┐
    │       │              │              │        │
    ▼       ▼              ▼              ▼        ▼
┌────────┬──────────┬──────────┬──────────┬──────────┐
│Postgres│ RabbitMQ │  MinIO   │Elastic-  │  Gemini  │
│   16   │    3     │ (S3-API) │ search 8 │   API    │
│  SQL   │  AMQP    │  Object  │  Search  │   REST   │
│   DB   │  Broker  │  Storage │  Engine  │          │
└────────┴──────────┴──────────┴──────────┴──────────┘
```

---

**For implementation details, see:**
- [README.md](README.md) - Quick start
- [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) - Feature overview
- [DEPLOYMENT.md](DEPLOYMENT.md) - Deployment guide
