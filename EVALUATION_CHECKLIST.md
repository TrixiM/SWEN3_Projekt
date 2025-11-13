# SWEN-3 Project Evaluation Checklist

**Date:** 2025-11-13  
**Project:** Document Management System  

---

## Functional Requirements (20 Points)

### ✅ Use Cases Understanding and Implementation (5/5)
- [x] **Document Upload Use Case** - Implemented with REST API, file upload, MinIO storage
- [x] **Document Retrieval Use Case** - Get all documents, get by ID, get content
- [x] **Document Search Use Case** - Elasticsearch integration with full-text search
- [x] **Document Analytics Use Case** - Analytics tracking with quality scoring
- [x] **Document Deletion Use Case** - Complete deletion including storage cleanup
- [x] **PDF Rendering Use Case** - PDF page rendering and preview functionality
- [x] **OCR Processing Use Case** - Async OCR processing with worker service
- [x] **AI Summary Generation Use Case** - GenAI worker with Gemini API integration

**Evidence:**
- Controllers: `DocumentController`, `DocumentSearchController`, `DocumentAnalyticsController`
- Business Logic: `DocumentBusinessLogic`, `PdfRenderingBusinessLogic`
- Documentation: Multiple detailed markdown files (IMPLEMENTATION_SUMMARY.md, PDF_UPLOAD_FLOW.md)

---

### ✅ REST API Implementation (5/5)
- [x] **Service Layers Implemented**
  - `DocumentService` - Domain service layer
  - `DocumentSearchService` - Search functionality
  - `DocumentAnalyticsService` - Analytics operations
  - `MinIOStorageService` - Storage abstraction with @CircuitBreaker
  - `IdempotencyService` - Message deduplication
- [x] **Converters/Mappers**
  - `DocumentMapper` - Entity to DTO conversion
  - `DocumentUpdateMapper` - Update DTO mapping
  - Clean separation between domain and presentation
- [x] **Service Agents**
  - `MinIOStorageService` - MinIO client wrapper
  - `GeminiService` - Gemini API integration
  - `ElasticsearchService` - Search integration
- [x] **REST Endpoints**
  - Document CRUD: GET, POST, PUT, DELETE
  - Search: 3 endpoints (title, content, combined)
  - Analytics: 5 endpoints
  - PDF rendering: page count, render page
- [x] **OpenAPI Documentation** - springdoc-openapi integration (v2.6.0)

**Evidence:**
- Service layer clearly separated from controllers
- Proper exception handling with @RestControllerAdvice
- DTOs for request/response (CreateDocumentDTO, DocumentResponse, etc.)

---

### ⚠️ Web Frontend UX and Completeness (3/5)
- [x] **Basic Functionality Present**
  - Document listing (index.html)
  - Document details view (docDetail.html)
  - Document upload
  - Tag management
- [x] **JavaScript Architecture**
  - `DocumentManager.js` - Document operations
  - `main.js` - Main app logic
  - `docDetails.js` - Detail view logic
  - `utils.js` - Utility functions with retry logic
- [⚠️] **Missing/Limited Features**
  - ❌ No search UI implementation (backend API exists)
  - ❌ No analytics dashboard (backend API exists)
  - ❌ Basic CSS styling only
  - ❌ Limited error handling/user feedback
  - ❌ No responsive design evident

**Recommendation:** Add search interface and analytics dashboard to utilize backend APIs

---

### ✅ Additional Use Case (5/5)
- [x] **Document Analytics Use Case** - Comprehensive implementation
  - New entity: `DocumentAnalytics`
  - Quality score calculation (confidence + content density)
  - Automatic analytics on OCR completion
  - Multiple filtering options (language, confidence, quality)
  - Summary statistics endpoint
- [x] **Elasticsearch Integration** - Advanced search capability
  - Full-text search across documents
  - Content indexing after OCR
  - Multiple search modes (title, content, combined)

**Evidence:**
- 5 analytics REST endpoints
- 8 unit tests for DocumentAnalyticsService
- Automatic creation via RabbitMQ consumer
- Documented in IMPLEMENTATION_SUMMARY.md

**Score:** 5/5 - Exceeds requirements with two substantial additional use cases

---

## Non-Functional Requirements (18 Points)

### ✅ Queues - Async Communication (4/4)
- [x] **RabbitMQ Integration** - Complete implementation
  - `document.created.queue` - OCR processing trigger
  - `ocr.completed.queue` - GenAI processing trigger  
  - `summary.result.queue` - Summary persistence
  - `document.deleted.queue` - Deletion events
- [x] **Dead Letter Queues (DLQ)** - All queues configured with DLQ
  - Message TTL: 5 minutes
  - Separate DLQ exchange: `document.exchange.dlx`
- [x] **Multiple Workers**
  - Backend (message consumer and producer)
  - OCR Worker (separate service)
  - GenAI Worker (separate service)
- [x] **Message Idempotency** - Implemented in all consumers
- [x] **Proper Configuration**
  - Prefetch count configured
  - Acknowledgment modes set
  - RabbitMQConfig in all services

**Evidence:**
- Docker-compose includes RabbitMQ with management UI
- Comprehensive documentation in STABILITY_IMPROVEMENTS.md
- IdempotencyService prevents duplicate processing

---

### ✅ Logging (2/2)
- [x] **All Layers Have Logging**
  - Controllers: Log requests/responses
  - Business Logic: Log operations and decisions
  - Services: Log external calls and errors
  - Message Consumers: Log message processing
  - Workers: Comprehensive logging with emoji markers
- [x] **Proper Log Levels**
  - DEBUG: Detailed operation info
  - INFO: Key operations (create, delete)
  - WARN: Non-critical issues (MinIO cleanup failures)
  - ERROR: Critical errors with stack traces
- [x] **Logger Pattern**
  - SLF4J with LoggerFactory
  - Example: `private static final Logger log = LoggerFactory.getLogger(...)`
- [x] **Structured Logging**
  - Consistent format across all components
  - Context included (IDs, filenames, etc.)

**Evidence:**
- 15+ files with proper logging identified via grep
- ErrorHandler logs all exception types
- Detailed logs in STABILITY_IMPROVEMENTS.md

---

### ⚠️ Validation (1.5/2)
- [x] **Input Validation in Controllers**
  - `@Valid` annotations on request parameters
  - `@NotBlank`, `@Size`, `@Positive` on DTOs
  - Example: CreateDocumentDTO has complete validation
- [x] **Business Logic Validation**
  - `validateDocument()` in DocumentBusinessLogic
  - File size limits (max 100MB)
  - Required field checks
  - `PdfValidator` for PDF validation
- [⚠️] **Service Layer Validation**
  - Some validation present in services
  - Could be more comprehensive
- [❌] **Database Constraints**
  - JPA annotations present (@Column nullable, length)
  - Not clear if all constraints are validated at DB level

**Recommendation:** Add more explicit validation at service layer; ensure DB constraints match validation rules

---

### ✅ Stability Patterns (2/2)
- [x] **Exception Handling**
  - Custom exception hierarchy:
    - `NotFoundException`
    - `InvalidRequestException`
    - `BusinessLogicException`
    - `ServiceException`
    - `MessagingException`
    - `DataAccessException`
    - `PdfProcessingException`
  - Global exception handler: `ErrorHandler` with @RestControllerAdvice
  - Proper HTTP status codes (404, 400, 422, 503, 500)
- [x] **Layer-Based Exceptions**
  - Each layer throws appropriate exceptions
  - Exceptions converted to proper HTTP responses
- [x] **Resilience4j Patterns**
  - **Circuit Breaker**: MinIO operations, Gemini API calls
  - **Retry with Exponential Backoff**: 3 attempts, 1s base delay
  - **Rate Limiting**: Gemini API (10 requests/60s)
  - **Bulkhead**: Configuration present
  - **Timeouts**: Connection (10s), Read (30s), Write (30s)
- [x] **Fallback Mechanisms**
  - Circuit breaker fallback methods
  - Graceful degradation patterns
- [x] **Transaction Management**
  - @Transactional on business logic
  - Rollback on MinIO upload failure

**Evidence:**
- STABILITY_IMPROVEMENTS.md documents all patterns
- Resilience4j dependencies in all services
- Comprehensive exception handling

**Score:** 2/2 - Excellent implementation

---

### ⚠️ Unit Tests (3/4)
- [x] **Tests Present**
  - 12 test files identified
  - 20+ unit tests documented
  - Mocking with Mockito
  - MockMvc for controllers
- [x] **Test Categories**
  - Controller tests: `DocumentControllerTest` (14 tests)
  - Business logic tests: `DocumentBusinessLogicTest` (20+ tests)
  - Service tests: `DocumentServiceTest`, `MinIOStorageServiceTest`
  - Mapper tests: `DocumentMapperTest` (10 tests)
  - Search tests: `DocumentSearchServiceTest` (6 tests)
  - Analytics tests: `DocumentAnalyticsServiceTest` (8 tests)
  - PDF tests: `PdfRenderingBusinessLogicTest` (20 tests)
- [⚠️] **Coverage**
  - Backend: ~80% documented coverage
  - Frontend: ❌ No tests identified
  - OCR Worker: ⚠️ Only Elasticsearch service tests mentioned
  - GenAI Worker: ❌ No tests identified
- [x] **Test Quality**
  - Proper AAA pattern (Arrange, Act, Assert)
  - Descriptive test names
  - Good use of mocking

**Recommendation:** Add unit tests for workers; add frontend tests (Jest/Vitest)

---

### ⚠️ Integration Tests (2/4)
- [x] **End-to-End Test Present**
  - `DocumentUploadIntegrationTest` - 8 tests
  - TestContainers with PostgreSQL
  - Full workflow testing
- [x] **Infrastructure**
  - TestContainers for real DB
  - Documented in TEST_README.md
- [❌] **Coverage Gaps**
  - ❌ No RabbitMQ integration tests
  - ❌ No MinIO integration tests
  - ❌ No Elasticsearch integration tests
  - ❌ No multi-service workflow tests
  - ❌ No OCR worker integration tests
  - ❌ No GenAI worker integration tests

**Recommendation:** Add integration tests for:
1. Complete upload → OCR → AI summary workflow
2. RabbitMQ message flow
3. Elasticsearch search functionality
4. MinIO storage operations

---

### ✅ Clean Code (2/2)
- [x] **SOLID Principles**
  - **S**ingle Responsibility: Services focused on specific concerns
  - **O**pen/Closed: Strategy pattern with services
  - **L**iskov Substitution: Interface implementations
  - **I**nterface Segregation: Small, focused interfaces
  - **D**ependency Inversion: Constructor injection, no @Autowired
- [x] **Code Quality**
  - Small, focused classes
  - Descriptive naming (DocumentBusinessLogic, MinIOStorageService)
  - No God classes
  - Proper encapsulation (private methods, final classes for utilities)
- [x] **Design Patterns**
  - Repository Pattern
  - Service Layer Pattern
  - Facade Pattern (BusinessLogic)
  - Builder Pattern (DocumentBuilder)
  - Strategy Pattern (different validators)
- [x] **No Code Smells**
  - No @Autowired (constructor injection)
  - No magic numbers
  - Proper constant usage
  - Clean exception handling

**Evidence:**
- Constructor injection throughout
- Well-organized package structure
- Utility classes are final with private constructors (DocumentMapper)

---

## Software Architecture (18 Points)

### ✅ Packaging (10/10)
- [x] **Docker Compose Configuration**
  - Complete multi-service setup
  - 8 services: REST, frontend, postgres, pgadmin, rabbitmq, ocr-worker, genai-worker, minio, elasticsearch
  - Proper networking (app-network)
  - Volume management for persistence
  - Health checks (MinIO, Elasticsearch)
  - Environment variable configuration
- [x] **Dockerfiles**
  - Backend: DOCKERFILE.REST
  - Frontend: Dockerfile with nginx
  - OCR Worker: DOCKERFILE.OCR  
  - GenAI Worker: Dockerfile
- [x] **Configuration Management**
  - Environment variables for secrets
  - .env file for GEMINI_API_KEY
  - Proper dependency definitions (depends_on)
- [x] **Ports Properly Exposed**
  - REST API: 8080
  - Frontend: 80
  - PostgreSQL: 5432
  - PgAdmin: 5050
  - RabbitMQ: 5672, 15672
  - MinIO: 9000, 9001
  - Elasticsearch: 9200, 9300
- [x] **Restart Policies** - All services have `restart: always`

**Score:** 10/10 - Production-ready containerization

---

### ✅ Loose Coupling (2/2)
- [x] **Interfaces Used**
  - Repository interfaces (extends JpaRepository)
  - ElasticsearchRepository interface
  - Service abstractions
- [x] **Dependency Injection**
  - Constructor injection (no @Autowired)
  - Spring manages dependencies
- [x] **Message-Based Communication**
  - Services decoupled via RabbitMQ
  - OCR Worker independent
  - GenAI Worker independent
- [x] **Configuration Externalized**
  - application.properties
  - Environment variables
  - No hardcoded values
- [x] **Abstraction Layers**
  - MinIOStorageService abstracts storage details
  - Service agents hide external API complexity

**Evidence:**
- Workers can be scaled independently
- Services communicate via queues
- Clear interface boundaries

---

### ⚠️ Mapper (1.5/2)
- [x] **Manual Mapping**
  - `DocumentMapper` - Entity to DTO
  - `DocumentUpdateMapper` - Update operations
  - Clean, focused mapping logic
- [❌] **No Mapping Framework**
  - ❌ No MapStruct
  - ❌ No ModelMapper
  - ❌ No Orika
- [x] **Mapping Tested**
  - `DocumentMapperTest` with 10 tests
  - Edge case coverage

**Recommendation:** Consider using MapStruct for better maintainability and less boilerplate

---

### ✅ Dependency Injection (2/2)
- [x] **Spring DI Framework Used**
  - Spring Boot 3.3.3
  - Spring Context managing beans
- [x] **Constructor Injection**
  - All services use constructor injection
  - No field injection (@Autowired on fields)
  - Immutable dependencies
- [x] **Component Scanning**
  - @Service, @Component, @Repository annotations
  - Proper Spring configuration
- [x] **Configuration Classes**
  - `RabbitMQConfig` - Queue configuration
  - `StorageConfiguration` - MinIO setup
  - `JpaConfig` - Database config

**Evidence:**
- Grep found no @Autowired on fields
- Constructor injection consistently used
- Proper Spring Boot structure

---

### ✅ DAL - Data Access Layer (2/2)
- [x] **ORM Used**
  - Spring Data JPA with Hibernate
  - JPA entities with proper annotations
  - @Entity, @Table, @Column
- [x] **Repository Pattern**
  - `DocumentRepository` - extends JpaRepository
  - `DocumentAnalyticsRepository`
  - Custom query methods
  - @Query for complex queries
- [x] **Entity Design**
  - `Document` entity with complete mapping
  - `DocumentAnalytics` entity
  - Proper relationships (@ElementCollection for tags)
  - Auditing (@CreatedDate, @LastModifiedDate)
- [x] **Database Features**
  - Optimistic locking (@Version)
  - Eager/Lazy loading configured
  - N+1 query prevention (JOIN FETCH)
  - Pagination support

**Evidence:**
- DocumentRepository has 20+ methods
- Custom queries for optimization
- Proper JPA configuration

---

### ✅ BL - Business Logic (2/2)
- [x] **Business Logic Layer Exists**
  - `DocumentBusinessLogic` - Core document operations
  - `PdfRenderingBusinessLogic` - PDF operations
- [x] **Domain Entities**
  - `Document` - Rich domain entity
  - `DocumentAnalytics` - Analytics domain
  - `DocumentStatus` - Enum for states
  - `DocumentBuilder` - Builder pattern
- [x] **Workflow Logic**
  - Document lifecycle management (NEW → PROCESSING → COMPLETED)
  - Transaction coordination
  - MinIO + DB transaction rollback
  - Validation and business rules
- [x] **Facade Pattern**
  - BusinessLogic classes act as facades
  - Coordinate between services
  - Handle complex workflows
- [x] **Separation of Concerns**
  - Business logic separate from controllers
  - Business logic separate from repositories
  - Clear responsibility boundaries

**Evidence:**
- DocumentBusinessLogic has 200+ lines of pure business logic
- Validation methods (validateDocument, validateId)
- Workflow coordination (upload, save, cleanup on failure)

---

## Software Development Workflow (20 Points)

### ⚠️ GitFlow (2/5)
- [x] **Git Used**
  - Repository exists with .git directory
  - Commit history present
- [x] **Branches Present**
  - `master` branch
  - `dev` branch
  - Remote branches: origin/dev, origin/master
- [❌] **GitFlow Not Fully Implemented**
  - ❌ No feature branches visible
  - ❌ No release branches
  - ❌ No hotfix branches
  - ⚠️ All commits on dev branch
- [❌] **No Pull Requests Evidence**
  - Commit history shows direct commits
  - No merge commit patterns
  - "Update OcrMessageConsumer.java" direct commit visible

**Git History Analysis:**
- 20 commits checked
- All on dev branch
- Direct commits (no PR merges)

**Recommendation:** 
1. Use feature branches (feature/search, feature/analytics)
2. Create pull requests for code review
3. Merge via PR to dev/master
4. Use proper GitFlow: feature → dev → release → master

---

### ❌ Issue Tracking (1/5)
- [⚠️] **To-DO File Exists**
  - Text file "To-DO" in root
  - Not a proper issue tracking system
- [❌] **No GitHub Issues**
  - .github folder exists but minimal
  - No evidence of issue tracking
- [❌] **No Kanban Board**
  - No GitHub Projects
  - No Jira integration
  - No Trello board linked
- [❌] **Not Kept Current**
  - Static TO-DO file

**Recommendation:**
1. Enable GitHub Issues
2. Create GitHub Project board
3. Track features, bugs, tasks as issues
4. Link commits/PRs to issues
5. Update regularly

---

### ⚠️ CI/CD Pipelines (3/5)
- [x] **GitHub Actions Present**
  - `.github/workflows/CICD.yml` exists
- [x] **Basic CI Implemented**
  - Triggers on push to dev/master
  - Triggers on PR to dev/master
  - JDK 21 setup
  - Run tests: `mvn test`
  - Docker image build
- [❌] **Limited Automation**
  - ❌ No linting step
  - ❌ No code coverage reporting
  - ❌ No static analysis (SonarQube, etc.)
  - ❌ No deployment automation (CD)
  - ❌ Only builds backend, not all services
  - ❌ No multi-stage pipeline
- [⚠️] **Docker Build Issues**
  - Builds from wrong context
  - Should build from backend directory

**Recommendation:**
1. Add linting (Checkstyle, SpotBugs)
2. Add coverage report (JaCoCo) upload
3. Build all services (backend, workers, frontend)
4. Add deployment step (Docker registry push)
5. Add smoke tests
6. Fix build context for backend

---

### ⚠️ Documentation (3.5/5)
- [x] **Extensive Technical Documentation**
  - IMPLEMENTATION_SUMMARY.md - Overview
  - ELASTICSEARCH_IMPLEMENTATION.md - Search feature
  - GENAI_IMPLEMENTATION.md - AI integration
  - PDF_UPLOAD_FLOW.md - Upload workflow
  - STABILITY_IMPROVEMENTS.md - Resilience patterns
  - TEST_README.md - Test documentation
- [x] **Component READMEs**
  - backend/TEST_README.md
  - frontend/README.md
  - genai-worker/README.md
  - ocr-worker/README.md
- [x] **Documentation Quality**
  - Detailed implementation notes
  - Code examples
  - Configuration snippets
  - Testing instructions
- [⚠️] **Missing Documentation**
  - ❌ No main README.md in root
  - ❌ No architecture diagrams (searched for images)
  - ❌ No API documentation (Swagger UI mentioned but not verified)
  - ⚠️ No deployment guide
  - ⚠️ No setup/installation guide in root

**Recommendation:**
1. Create comprehensive README.md in root with:
   - Project overview
   - Architecture diagram
   - Setup instructions
   - Quick start guide
   - API documentation link
2. Add architecture diagrams (C4, sequence diagrams)
3. Document API endpoints (or link to Swagger UI)

---

## Summary Scores

| Category | Max | Score | Notes |
|----------|-----|-------|-------|
| **Functional Requirements** | 20 | 18 | -2 for incomplete frontend |
| Use Cases | 5 | 5 | ✅ Excellent |
| REST API | 5 | 5 | ✅ Complete |
| Web Frontend | 5 | 3 | ⚠️ Missing search/analytics UI |
| Additional Use Case | 5 | 5 | ✅ Two use cases! |
| **Non-Functional Requirements** | 18 | 14.5 | Good overall |
| Queues | 4 | 4 | ✅ Excellent |
| Logging | 2 | 2 | ✅ Comprehensive |
| Validation | 2 | 1.5 | ⚠️ Could be more thorough |
| Stability Patterns | 2 | 2 | ✅ Outstanding |
| Unit Tests | 4 | 3 | ⚠️ Missing worker tests |
| Integration Tests | 4 | 2 | ⚠️ Limited coverage |
| Clean Code | 2 | 2 | ✅ Excellent |
| **Software Architecture** | 18 | 17.5 | Excellent |
| Packaging | 10 | 10 | ✅ Perfect |
| Loose Coupling | 2 | 2 | ✅ Complete |
| Mapper | 2 | 1.5 | ⚠️ No framework |
| Dependency Injection | 2 | 2 | ✅ Perfect |
| DAL | 2 | 2 | ✅ Complete |
| BL | 2 | 2 | ✅ Complete |
| **Software Development Workflow** | 20 | 9.5 | Needs improvement |
| GitFlow | 5 | 2 | ⚠️ Not properly used |
| Issue Tracking | 5 | 1 | ❌ No proper system |
| CI/CD Pipelines | 5 | 3 | ⚠️ Basic implementation |
| Documentation | 5 | 3.5 | ⚠️ Missing key docs |
| **TOTAL** | **76** | **59.5** | **78%** |

---

## Critical Missing Items (Priority Order)

### High Priority
1. ❌ **Main README.md** - Essential for project understanding
2. ❌ **Frontend Search UI** - Backend API exists but no UI
3. ❌ **Frontend Analytics Dashboard** - Backend API exists but no UI
4. ❌ **GitHub Issues/Project Board** - No issue tracking
5. ❌ **Pull Request Workflow** - No PR evidence
6. ❌ **Integration Tests for Message Queue Flow** - Critical path untested
7. ❌ **Architecture Diagrams** - Visual documentation missing

### Medium Priority
8. ⚠️ **More Comprehensive Validation** - Service layer validation
9. ⚠️ **Mapping Framework** - Consider MapStruct
10. ⚠️ **Worker Unit Tests** - OCR and GenAI workers need tests
11. ⚠️ **Enhanced CI/CD** - Add linting, coverage, multi-service builds
12. ⚠️ **Feature Branch Usage** - Implement proper GitFlow

### Low Priority
13. ⚠️ **Frontend Tests** - Jest/Vitest for JS
14. ⚠️ **API Documentation** - Verify Swagger UI accessibility
15. ⚠️ **Deployment Guide** - Production deployment instructions

---

## Strengths

1. ✅ **Excellent Architecture** - Clean layered architecture with proper separation
2. ✅ **Outstanding Resilience** - Comprehensive stability patterns implemented
3. ✅ **Production-Ready Containerization** - Docker Compose setup is exemplary
4. ✅ **Comprehensive Backend** - REST API is complete and well-designed
5. ✅ **Good Testing** - Backend has solid test coverage
6. ✅ **Async Processing** - RabbitMQ integration is well done
7. ✅ **Additional Features** - Two extra use cases (Analytics + Elasticsearch)
8. ✅ **Clean Code** - SOLID principles followed, good naming, no code smells
9. ✅ **Extensive Documentation** - Technical docs are detailed

---

## Recommended Actions

### To Reach 90% (68.4 points)
1. **Add Main README.md** (+1.0) - Project overview and setup guide
2. **Implement Frontend Search** (+1.5) - Utilize existing backend API
3. **Implement Frontend Analytics** (+0.5) - Basic dashboard
4. **Set up GitHub Issues** (+2.0) - Issue tracking system
5. **Implement PR Workflow** (+2.0) - Feature branches + PRs
6. **Add Integration Tests** (+1.5) - Message queue flow tests
7. **Enhanced CI/CD** (+1.5) - Linting, coverage, multi-service
8. **Add Architecture Diagrams** (+0.5) - Visual documentation

### Quick Wins (Can be done in < 2 hours)
- Create README.md with setup instructions
- Enable GitHub Issues and create initial issues
- Add architecture diagram (even simple)
- Fix CI/CD Docker build context
- Add Checkstyle/Spotbugs to Maven

---

## Overall Assessment

**Grade: B+ (78%)**

**Strengths:**
- Solid technical implementation
- Excellent backend architecture
- Production-ready infrastructure
- Good code quality

**Areas for Improvement:**
- Complete the frontend features
- Implement proper GitFlow workflow
- Set up issue tracking
- Enhance CI/CD pipeline
- Add missing documentation

The project demonstrates strong technical skills and understanding of modern software architecture principles. The main gaps are in the development workflow and frontend completeness rather than backend implementation quality.
