# Test Documentation

## Overview

This document describes the test suite for the Document Management System backend.

## Test Structure

```
backend/src/test/java/fhtw/wien/
├── controller/
│   └── DocumentControllerTest.java          # Controller layer tests with MockMvc
├── business/
│   └── DocumentBusinessLogicTest.java       # Business logic tests with mocked dependencies
├── service/
│   ├── DocumentServiceTest.java             # Service layer tests
│   └── MinIOStorageServiceTest.java         # MinIO service tests
├── util/
│   └── DocumentMapperTest.java              # DTO mapping tests
└── integration/
    └── DocumentUploadIntegrationTest.java   # End-to-end integration tests
```

## Test Categories

### 1. Unit Tests

#### DocumentControllerTest
- **Coverage**: All REST endpoints
- **Technology**: MockMvc, Mockito
- **Tests**: 14 test cases
- **Features**:
  - Document upload (valid/invalid)
  - Get all documents
  - Get document by ID
  - Get document content
  - PDF page rendering
  - PDF page count
  - Document deletion
  - Document update

#### DocumentBusinessLogicTest
- **Coverage**: Business logic layer
- **Technology**: Mockito for mocking
- **Tests**: 20+ test cases
- **Features**:
  - Document creation with MinIO upload
  - Transaction rollback handling
  - Validation (title, filename, size)
  - Document retrieval
  - Document deletion with cleanup
  - Content retrieval from MinIO

#### DocumentMapperTest
- **Coverage**: DTO mapping
- **Tests**: 10 test cases
- **Features**:
  - Complete field mapping
  - Null handling
  - Edge cases (empty tags, special characters)
  - Immutability verification

### 2. Integration Tests

#### DocumentUploadIntegrationTest
- **Coverage**: End-to-end workflows
- **Technology**: TestContainers (PostgreSQL), Spring Boot Test
- **Tests**: 8 test cases
- **Features**:
  - Full document upload workflow
  - Database persistence verification
  - API endpoint integration
  - Error handling (400, 404)

## Running Tests

### Prerequisites

- Java 21
- Maven 3.x
- Docker (for TestContainers integration tests)

### Run All Tests

```bash
mvn clean test
```

### Run Only Unit Tests

```bash
mvn test -Dtest=!*IntegrationTest
```

### Run Only Integration Tests

```bash
mvn test -Dtest=*IntegrationTest
```

### Run Specific Test Class

```bash
mvn test -Dtest=DocumentControllerTest
```

### Run with Coverage Report

```bash
mvn clean test jacoco:report
```

Coverage report will be available at: `target/site/jacoco/index.html`

## Test Configuration

### TestContainers

Integration tests use TestContainers to spin up a real PostgreSQL database:

```java
@Container
static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpass");
```

Docker must be running for integration tests to work.

### Mocking Strategy

- **Controllers**: MockMvc for HTTP layer testing
- **Business Logic**: Mockito for repository and MinIO service mocking
- **Integration Tests**: Real database (TestContainers), mocked MinIO

## Test Coverage Goals

| Layer | Current Coverage | Target |
|-------|-----------------|--------|
| Controllers | ~90% | 85%+ |
| Business Logic | ~95% | 90%+ |
| Services | ~60% | 70%+ |
| Utilities | ~100% | 95%+ |
| **Overall** | **~80%** | **80%+** |

## Adding New Tests

### Unit Test Template

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    
    @Mock
    private Dependency dependency;
    
    @InjectMocks
    private MyService service;
    
    @Test
    void methodName_WithCondition_ShouldExpectedBehavior() {
        // Given
        when(dependency.method()).thenReturn(value);
        
        // When
        Result result = service.methodUnderTest();
        
        // Then
        assertNotNull(result);
        verify(dependency).method();
    }
}
```

### Integration Test Template

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class MyIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgresContainer = // ...
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testWorkflow_ShouldSucceed() {
        // Test implementation
    }
}
```

## Continuous Integration

Tests are automatically run on:
- Pull requests
- Commits to main/dev branches
- Nightly builds

## Troubleshooting

### Issue: TestContainers fails to start

**Solution**: Ensure Docker is running and you have sufficient resources allocated.

```bash
docker ps  # Verify Docker is running
```

### Issue: Tests fail with "Connection refused"

**Solution**: Check if ports 5432 (PostgreSQL) or 9000 (MinIO) are already in use.

```bash
# Windows
netstat -ano | findstr :5432

# Linux/Mac
lsof -i :5432
```

### Issue: MinIO tests fail

**Solution**: MinIO is currently mocked in tests. For full MinIO testing, add MinIO TestContainer:

```java
@Container
static GenericContainer<?> minioContainer = new GenericContainer<>("minio/minio:latest")
        .withExposedPorts(9000)
        .withCommand("server /data");
```

## Best Practices

1. **Follow AAA Pattern**: Arrange, Act, Assert
2. **One assertion per test** (when possible)
3. **Descriptive test names**: `method_WithCondition_ShouldExpectedBehavior`
4. **Mock external dependencies**: Database, APIs, file system
5. **Clean up test data**: Use `@AfterEach` for cleanup
6. **Avoid test interdependence**: Tests should run in any order
7. **Use TestContainers** for integration tests requiring infrastructure

## Resources

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [TestContainers Documentation](https://www.testcontainers.org/)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
