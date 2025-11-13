# Document Management System (DMS)

[![CI](https://github.com/YOUR_USERNAME/swen-3/actions/workflows/CICD.yml/badge.svg)](https://github.com/YOUR_USERNAME/swen-3/actions)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A modern, microservices-based document management system with OCR capabilities, AI-powered summarization, and full-text search functionality.

## 📋 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Prerequisites](#-prerequisites)
- [Quick Start](#-quick-start)
- [Project Structure](#-project-structure)
- [API Documentation](#-api-documentation)
- [Development](#-development)
- [Testing](#-testing)
- [Deployment](#-deployment)
- [Documentation](#-documentation)

## ✨ Features

### Core Functionality
- 📄 **Document Management** - Upload, retrieve, update, and delete PDF documents
- 🔍 **Full-Text Search** - Elasticsearch-powered search across document content
- 📊 **Analytics Dashboard** - Document statistics, quality scoring, and insights
- 🖼️ **PDF Rendering** - Server-side PDF page rendering and preview
- 🏷️ **Tag Management** - Organize documents with custom tags

### Advanced Features
- 🤖 **OCR Processing** - Automatic text extraction from PDF documents using Tesseract
- 🧠 **AI Summarization** - Google Gemini-powered document summarization
- 📈 **Quality Scoring** - Automatic document quality assessment
- 🔄 **Async Processing** - RabbitMQ-based message queue for scalability
- 💾 **Object Storage** - MinIO for efficient document storage
- 🔒 **Resilience Patterns** - Circuit breakers, retries, and fault tolerance

## 🏗️ Architecture

### System Overview

```
┌─────────────┐
│   Browser   │
└──────┬──────┘
       │ HTTP
       ▼
┌─────────────────┐      ┌──────────────┐
│   Frontend      │──────│   Nginx      │
│   (Static)      │      │   (Port 80)  │
└─────────────────┘      └──────────────┘
                               │
                               │ REST API
                               ▼
                    ┌─────────────────────┐
                    │   Backend (REST)    │
                    │   Spring Boot       │
                    │   (Port 8080)       │
                    └──────────┬──────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
        ▼                      ▼                      ▼
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│  PostgreSQL  │      │   RabbitMQ   │      │    MinIO     │
│  (Port 5432) │      │ (Port 5672)  │      │  (Port 9000) │
└──────────────┘      └──────┬───────┘      └──────────────┘
                             │
                ┌────────────┼────────────┐
                │                         │
                ▼                         ▼
        ┌──────────────┐          ┌──────────────┐
        │  OCR Worker  │          │ GenAI Worker │
        │  (Tesseract) │          │   (Gemini)   │
        └──────┬───────┘          └──────────────┘
               │
               ▼
        ┌──────────────┐
        │Elasticsearch │
        │ (Port 9200)  │
        └──────────────┘
```

### Component Responsibilities

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Frontend** | HTML/CSS/JS + Nginx | User interface for document management |
| **Backend** | Spring Boot 3.3.3 | REST API, business logic, orchestration |
| **OCR Worker** | Spring Boot + Tesseract | Async PDF text extraction |
| **GenAI Worker** | Spring Boot + Gemini API | AI-powered document summarization |
| **PostgreSQL** | PostgreSQL 16 | Document metadata persistence |
| **Elasticsearch** | Elasticsearch 8.11 | Full-text search and indexing |
| **RabbitMQ** | RabbitMQ 3 | Message queue for async processing |
| **MinIO** | MinIO Latest | S3-compatible object storage |

### Message Flow

```
1. User uploads PDF
   ↓
2. Backend saves metadata → PostgreSQL
   ↓
3. Backend uploads file → MinIO
   ↓
4. Backend publishes message → RabbitMQ (document.created.queue)
   ↓
5. OCR Worker consumes message
   ↓
6. OCR Worker extracts text → Elasticsearch
   ↓
7. OCR Worker publishes → RabbitMQ (ocr.completed.queue)
   ↓
8. GenAI Worker generates summary
   ↓
9. GenAI Worker publishes → RabbitMQ (summary.result.queue)
   ↓
10. Backend updates document with summary
```

## 🛠️ Tech Stack

### Backend
- **Java 21** - Programming language
- **Spring Boot 3.3.3** - Application framework
- **Spring Data JPA** - Data persistence
- **Spring AMQP** - RabbitMQ integration
- **Resilience4j** - Fault tolerance patterns
- **Lombok** - Code generation
- **MapStruct** (recommended) - DTO mapping

### Frontend
- **HTML5/CSS3** - Markup and styling
- **Vanilla JavaScript** - Client-side logic
- **Nginx** - Static file serving

### Infrastructure
- **Docker & Docker Compose** - Containerization
- **PostgreSQL 16** - Relational database
- **Elasticsearch 8.11** - Search engine
- **RabbitMQ 3** - Message broker
- **MinIO** - Object storage
- **PgAdmin 4** - Database management UI

### External Services
- **Google Gemini API** - AI text generation
- **Tesseract OCR** - Text extraction

## 📦 Prerequisites

- **Docker** (v20.10+) and **Docker Compose** (v2.0+)
- **Java 21** (for local development)
- **Maven 3.9+** (for local development)
- **Google Gemini API Key** ([Get one here](https://makersuite.google.com/app/apikey))

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/swen-3.git
cd swen-3
```

### 2. Set Up Environment Variables

Create a `.env` file in the project root:

```bash
# .env
GEMINI_API_KEY=your-actual-gemini-api-key-here
```

### 3. Start All Services

```bash
docker-compose up --build
```

This will start all services:
- ✅ Backend REST API: http://localhost:8080
- ✅ Frontend UI: http://localhost:80
- ✅ RabbitMQ Management: http://localhost:15672 (guest/guest)
- ✅ MinIO Console: http://localhost:9001 (minioadmin/minioadmin)
- ✅ PgAdmin: http://localhost:5050 (admin@admin.com/adminpassword)
- ✅ Elasticsearch: http://localhost:9200

### 4. Access the Application

Open your browser and navigate to:
- **Frontend**: http://localhost
- **API Documentation**: http://localhost:8080/swagger-ui.html

### 5. Upload Your First Document

**Via UI:**
1. Go to http://localhost
2. Click "Upload Document"
3. Select a PDF file
4. Add a title and optional tags
5. Click "Upload"

**Via API:**
```bash
curl -X POST http://localhost:8080/api/documents \
  -F "file=@test.pdf" \
  -F "title=My First Document" \
  -F "tags=test,demo"
```

### 6. Watch Processing

Monitor the workers:
```bash
# OCR Worker logs
docker-compose logs -f ocr-worker

# GenAI Worker logs
docker-compose logs -f genai-worker
```

After 10-30 seconds, your document will have:
- ✅ Extracted text (searchable)
- ✅ AI-generated summary
- ✅ Quality analytics

## 📁 Project Structure

```
swen-3/
├── backend/                      # Main REST API service
│   ├── src/
│   │   ├── main/java/fhtw/wien/
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── service/         # Service layer
│   │   │   ├── business/        # Business logic
│   │   │   ├── domain/          # Domain entities
│   │   │   ├── repository/      # Data access
│   │   │   ├── dto/             # Data transfer objects
│   │   │   ├── messaging/       # RabbitMQ consumers/producers
│   │   │   ├── exception/       # Custom exceptions
│   │   │   ├── config/          # Configuration
│   │   │   └── util/            # Utilities
│   │   └── test/                # Unit & integration tests
│   ├── pom.xml
│   └── DOCKERFILE.REST
│
├── ocr-worker/                   # OCR processing service
│   ├── src/main/java/fhtw/wien/ocrworker/
│   │   ├── messaging/           # RabbitMQ consumers
│   │   ├── service/             # OCR service
│   │   ├── elasticsearch/       # Search integration
│   │   └── config/              # Configuration
│   ├── pom.xml
│   └── DOCKERFILE.OCR
│
├── genai-worker/                 # AI summarization service
│   ├── src/main/java/fhtw/wien/genaiworker/
│   │   ├── messaging/           # RabbitMQ consumers
│   │   ├── service/             # Gemini integration
│   │   └── config/              # Configuration
│   ├── pom.xml
│   └── Dockerfile
│
├── frontend/                     # Web UI
│   ├── paperless-ui/
│   │   ├── index.html           # Main page
│   │   ├── docDetail.html       # Document details
│   │   ├── js/                  # JavaScript
│   │   └── css/                 # Stylesheets
│   ├── nginx.conf
│   └── Dockerfile
│
├── docker-compose.yml            # Multi-service orchestration
├── .env                          # Environment variables
├── pom.xml                       # Parent POM
│
└── Documentation/
    ├── IMPLEMENTATION_SUMMARY.md
    ├── ELASTICSEARCH_IMPLEMENTATION.md
    ├── GENAI_IMPLEMENTATION.md
    ├── PDF_UPLOAD_FLOW.md
    ├── STABILITY_IMPROVEMENTS.md
    └── TEST_README.md
```

## 📚 API Documentation

### Base URL
```
http://localhost:8080/api
```

### Document Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/documents` | List all documents |
| GET | `/documents/{id}` | Get document by ID |
| POST | `/documents` | Upload new document |
| PUT | `/documents/{id}` | Update document |
| DELETE | `/documents/{id}` | Delete document |
| GET | `/documents/{id}/content` | Download document content |
| GET | `/documents/{id}/pdf/page-count` | Get PDF page count |
| GET | `/documents/{id}/pdf/render-page?page={n}` | Render PDF page as image |

### Search

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/documents/search?q={query}` | Search in title and content |
| GET | `/documents/search/title?q={query}` | Search in titles only |
| GET | `/documents/search/content?q={query}` | Search in content only |

### Analytics

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/analytics/documents/{id}` | Get document analytics |
| GET | `/analytics/high-quality` | Get high-quality documents |
| GET | `/analytics/language/{lang}` | Filter by language |
| GET | `/analytics/confidence/{min}` | Filter by confidence score |
| GET | `/analytics/summary` | Get overall statistics |

### Example: Upload Document

```bash
curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: multipart/form-data" \
  -F "file=@document.pdf" \
  -F "title=My Document" \
  -F "tags=important,2024"
```

### Example: Search Documents

```bash
curl "http://localhost:8080/api/documents/search?q=contract"
```

## 🔧 Development

### Running Backend Locally

```bash
cd backend
mvn spring-boot:run
```

### Running Tests

```bash
# All tests
mvn clean test

# Specific test class
mvn test -Dtest=DocumentServiceTest

# With coverage report
mvn clean test jacoco:report
# Report: target/site/jacoco/index.html
```

### Building Docker Images

```bash
# Backend
docker build -f backend/DOCKERFILE.REST -t dms-backend:latest ./backend

# OCR Worker
docker build -f ocr-worker/DOCKERFILE.OCR -t dms-ocr:latest ./ocr-worker

# GenAI Worker
docker build -f genai-worker/Dockerfile -t dms-genai:latest ./genai-worker

# Frontend
docker build -f frontend/Dockerfile -t dms-frontend:latest ./frontend
```

### Code Quality

```bash
# Run Checkstyle (if configured)
mvn checkstyle:check

# Run SpotBugs (if configured)
mvn spotbugs:check

# Format code
mvn formatter:format
```

## 🧪 Testing

### Test Categories

| Type | Location | Description |
|------|----------|-------------|
| Unit Tests | `backend/src/test/java/*/` | Service, business logic, mapper tests |
| Integration Tests | `backend/src/test/java/*/integration/` | End-to-end workflow tests |
| Test Coverage | ~80% backend | Controllers, services, business logic |

### Running Integration Tests

```bash
# Requires Docker running (for TestContainers)
mvn verify
```

### Test Documentation
See [backend/TEST_README.md](backend/TEST_README.md) for detailed test documentation.

## 🚢 Deployment

### Production Considerations

1. **Environment Variables**
   - Set secure passwords for all services
   - Use secrets management (Vault, AWS Secrets Manager)
   - Never commit `.env` to version control

2. **Scaling**
   ```bash
   # Scale workers
   docker-compose up -d --scale ocr-worker=3 --scale genai-worker=2
   ```

3. **Monitoring**
   - Health endpoints: `/actuator/health`
   - Prometheus metrics: `/actuator/prometheus` (if configured)
   - RabbitMQ Management UI for queue monitoring

4. **Backup**
   - PostgreSQL: `docker exec postgres pg_dump -U myuser documentdb > backup.sql`
   - MinIO: Use `mc mirror` for backups

### Kubernetes Deployment
(Coming soon - see [DEPLOYMENT.md](DEPLOYMENT.md))

## 📖 Documentation

### Architecture & Implementation
- [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) - Overview of all features
- [ELASTICSEARCH_IMPLEMENTATION.md](ELASTICSEARCH_IMPLEMENTATION.md) - Search functionality
- [GENAI_IMPLEMENTATION.md](GENAI_IMPLEMENTATION.md) - AI integration details
- [PDF_UPLOAD_FLOW.md](PDF_UPLOAD_FLOW.md) - Complete upload workflow
- [STABILITY_IMPROVEMENTS.md](STABILITY_IMPROVEMENTS.md) - Resilience patterns

### Component Documentation
- [backend/TEST_README.md](backend/TEST_README.md) - Backend testing guide
- [frontend/README.md](frontend/README.md) - Frontend documentation
- [ocr-worker/README.md](ocr-worker/README.md) - OCR worker setup
- [genai-worker/README.md](genai-worker/README.md) - GenAI worker setup

### Evaluation
- [EVALUATION_CHECKLIST.md](EVALUATION_CHECKLIST.md) - Project assessment against requirements

## 🔍 Troubleshooting

### Common Issues

**Port Already in Use**
```bash
# Check what's using the port
lsof -i :8080
# Kill the process
kill -9 <PID>
```

**Docker Compose Fails**
```bash
# Clean up and restart
docker-compose down -v
docker-compose up --build
```

**OCR Worker Not Processing**
```bash
# Check logs
docker-compose logs ocr-worker
# Restart worker
docker-compose restart ocr-worker
```

**Gemini API Errors**
- Verify API key in `.env`
- Check rate limits (60 requests/minute free tier)
- Review worker logs: `docker-compose logs genai-worker`

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit changes: `git commit -am 'Add my feature'`
4. Push to branch: `git push origin feature/my-feature`
5. Create a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Authors

- **Your Name** - *Initial work* - [GitHub Profile](https://github.com/YOUR_USERNAME)

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- Tesseract OCR for text extraction capabilities
- Google for Gemini API access
- MinIO for object storage solution
- The open-source community

---

**Built with ❤️ for SWEN-3 course at FH Technikum Wien**
