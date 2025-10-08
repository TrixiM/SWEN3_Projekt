# Exception Handling and Logging Implementation Summary

## Overview
This document summarizes the implementation of comprehensive exception handling with layer-specific exceptions and strategic logging throughout the application.

## 1. Layer-Specific Exception Hierarchy

### Exception Classes Created

#### Base/Generic Exceptions
- **`NotFoundException`** (existing) - Used when resources are not found
- **`InvalidRequestException`** - For invalid request parameters or validation errors

#### Layer-Specific Exceptions
1. **`BusinessLogicException`** - Business Logic Layer
   - Thrown when business rules are violated
   - Parent class for specialized business exceptions
   
2. **`PdfProcessingException`** (extends BusinessLogicException) - Business Logic Layer
   - Specialized exception for PDF rendering/processing failures
   
3. **`ServiceException`** - Service Layer
   - Thrown when service orchestration operations fail
   
4. **`MessagingException`** - Messaging Layer
   - Thrown when message publishing or consuming operations fail
   
5. **`DataAccessException`** - Data Access Layer
   - Thrown when database operations fail

### Exception Handler Coverage

The `ErrorHandler` class now handles:
- `NotFoundException` → HTTP 404
- `InvalidRequestException` → HTTP 400
- `BusinessLogicException` → HTTP 422 (Unprocessable Entity)
- `PdfProcessingException` → HTTP 500
- `ServiceException` → HTTP 500
- `MessagingException` → HTTP 503 (Service Unavailable)
- `DataAccessException` → HTTP 500
- `IOException` → HTTP 500
- `MethodArgumentNotValidException` → HTTP 400
- `ConstraintViolationException` → HTTP 400
- `IllegalArgumentException` → HTTP 400
- Generic `Exception` → HTTP 500 (catch-all)

All exception handlers include logging at appropriate levels (WARN for client errors, ERROR for server errors).

## 2. Logging Implementation

### Logging Strategy by Layer

#### Controller Layer (`DocumentController`)
**Logger**: `org.slf4j.Logger`

**Log Points:**
- **INFO level**: 
  - Incoming requests (POST, PUT, DELETE, GET operations)
  - Successful operation completions with resource IDs
  
- **WARN level**:
  - Empty file uploads
  - Missing PDF data
  - Invalid page number requests
  
- **DEBUG level**:
  - Response details (document retrieval, page counts)
  - File sizes and metadata

**Example:**
```java
log.info("POST /v1/documents - Creating document with title: {}, filename: {}", title, file.getOriginalFilename());
log.info("Document created successfully with ID: {}", saved.getId());
```

#### Service Layer (`DocumentService`)
**Logger**: `org.slf4j.Logger`

**Log Points:**
- **INFO level**:
  - Document creation/update/deletion operations start
  - Successful operation completions
  
- **DEBUG level**:
  - Document retrieval operations
  - PDF rendering operations
  - Page count queries
  
- **ERROR level**:
  - Operation failures with full exception stack traces

**Example:**
```java
log.info("Creating document with title: {}", doc.getTitle());
log.error("Failed to create document: {}", doc.getTitle(), e);
```

#### Business Logic Layer (`DocumentBusinessLogic`, `PdfRenderingBusinessLogic`)
**Logger**: `org.slf4j.Logger`

**Log Points:**
- **DEBUG level**:
  - Repository operations (save, fetch, delete)
  - PDF loading and page counting
  - Successful operation details
  
- **WARN level**:
  - Resource not found scenarios
  - Invalid page numbers
  
- **ERROR level**:
  - Repository operation failures
  - PDF processing errors
  - Unexpected exceptions

**Example:**
```java
log.debug("Rendering page {} of document {} with scale {}", pageNumber, document.getId(), scale);
log.error("IO error while rendering page {} of document {}", pageNumber, document.getId(), e);
```

#### Messaging Layer (`DocumentMessageProducer`, `DocumentMessageConsumer`)
**Logger**: `org.slf4j.Logger`

**Log Points:**
- **INFO level**:
  - Message publishing events
  - Message consumption events
  - Acknowledgment sending
  
- **DEBUG level**:
  - Successful message delivery confirmations
  
- **ERROR level**:
  - Message publishing failures
  - Message processing failures

**Example:**
```java
log.info("Publishing document created event for document ID: {}", document.id());
log.error("Failed to publish document created event for ID: {}", document.id(), e);
```

### Logging Configuration

Added comprehensive logging configuration in `application.properties`:

```properties
# Root logging level
logging.level.root=INFO

# Application-specific logging
logging.level.fhtw.wien=DEBUG
logging.level.fhtw.wien.controller=INFO
logging.level.fhtw.wien.service=INFO
logging.level.fhtw.wien.business=DEBUG
logging.level.fhtw.wien.messaging=INFO
logging.level.fhtw.wien.exception=WARN

# Spring Framework logging
logging.level.org.springframework.web=INFO
logging.level.org.springframework.amqp=INFO
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Log pattern with timestamp, thread, level, logger name, and message
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
```

## 3. Exception Handling Flow

### Flow by Layer

```
Controller Layer
    ↓ (catches IOException, throws InvalidRequestException)
Service Layer
    ↓ (wraps in ServiceException, re-throws domain exceptions)
Business Logic Layer
    ↓ (throws BusinessLogicException, PdfProcessingException, NotFoundException)
Data Access Layer
    ↓ (wraps in DataAccessException)
Repository Layer
```

### Exception Re-throw Strategy

- **Domain exceptions** (NotFoundException, InvalidRequestException) are re-thrown as-is to preserve semantics
- **Technical exceptions** are wrapped in layer-specific exceptions with original cause
- **All exceptions** eventually reach the ErrorHandler for HTTP response generation

## 4. Critical Logging Positions

### Entry Points (Request Reception)
- ✅ All controller endpoints log incoming requests
- ✅ Message consumers log received events

### Exit Points (Response/Processing)
- ✅ All controller endpoints log successful completions
- ✅ Message producers log successful publishing

### Error Points
- ✅ All exception scenarios logged with appropriate levels
- ✅ Full stack traces included for ERROR level
- ✅ Context information (IDs, parameters) included

### Business-Critical Operations
- ✅ Document creation/deletion (INFO level)
- ✅ PDF rendering operations (INFO/DEBUG level)
- ✅ Message queue operations (INFO level)
- ✅ Database transactions (DEBUG level via Hibernate)

## 5. Benefits Achieved

### Exception Handling
1. **Layer Isolation**: Each layer has its own exception types, making it clear where errors originate
2. **Proper HTTP Status Codes**: Different exception types map to appropriate HTTP responses
3. **Exception Context**: All exceptions include meaningful messages with relevant IDs/parameters
4. **Cause Preservation**: Original exceptions are preserved in the cause chain
5. **Centralized Handling**: Single ErrorHandler manages all exception-to-response mapping

### Logging
1. **Traceability**: Every request can be traced through all layers
2. **Debugging**: DEBUG level provides detailed operation information
3. **Monitoring**: INFO level provides operational visibility
4. **Alerting**: ERROR level captures failures needing immediate attention
5. **Performance**: Appropriate log levels prevent excessive logging overhead
6. **Context**: All logs include relevant IDs and parameters for correlation

## 6. Testing Recommendations

### Manual Testing
1. Test each endpoint with valid data → should see INFO logs for success
2. Test with invalid IDs → should see WARN logs and 404 responses
3. Test with invalid parameters → should see WARN logs and 400 responses
4. Test PDF rendering with invalid pages → should see ERROR logs and 500 responses
5. Stop RabbitMQ → should see ERROR logs and 503 responses

### Log Verification
```bash
# Follow logs in real-time
docker-compose logs -f backend

# Filter by log level
docker-compose logs backend | grep ERROR
docker-compose logs backend | grep WARN
docker-compose logs backend | grep INFO
```

## 7. Files Modified/Created

### Created Exception Classes (6 new files)
- `/backend/src/main/java/fhtw/wien/exception/BusinessLogicException.java`
- `/backend/src/main/java/fhtw/wien/exception/ServiceException.java`
- `/backend/src/main/java/fhtw/wien/exception/MessagingException.java`
- `/backend/src/main/java/fhtw/wien/exception/DataAccessException.java`
- `/backend/src/main/java/fhtw/wien/exception/PdfProcessingException.java`
- `/backend/src/main/java/fhtw/wien/exception/InvalidRequestException.java`

### Modified Files (8 files)
- `/backend/src/main/java/fhtw/wien/exception/ErrorHandler.java` - Added handlers for all exception types
- `/backend/src/main/java/fhtw/wien/controller/DocumentController.java` - Added logging and validation
- `/backend/src/main/java/fhtw/wien/service/DocumentService.java` - Added logging and exception handling
- `/backend/src/main/java/fhtw/wien/business/DocumentBusinessLogic.java` - Added logging and exception handling
- `/backend/src/main/java/fhtw/wien/business/PdfRenderingBusinessLogic.java` - Added logging and exception handling
- `/backend/src/main/java/fhtw/wien/messaging/DocumentMessageProducer.java` - Added exception handling
- `/backend/src/main/java/fhtw/wien/messaging/DocumentMessageConsumer.java` - Added exception handling
- `/backend/src/main/resources/application.properties` - Added logging configuration

## Summary

✅ **Requirement 4: Failure/exception-handling with layer-specific exceptions** - FULLY IMPLEMENTED
- Created 6 new layer-specific exception classes
- Implemented proper exception handling in all layers
- Added comprehensive error handler with proper HTTP status mapping
- Preserved exception causes and added meaningful context

✅ **Requirement 5: Logging in remarkable/critical positions** - FULLY IMPLEMENTED
- Added SLF4J logging to all layers (Controller, Service, Business Logic, Messaging)
- Implemented strategic logging at entry/exit points
- Added error logging with full context and stack traces
- Configured log levels and patterns in application.properties
- All critical operations (CRUD, PDF rendering, messaging) are logged

The implementation follows Spring Boot best practices and provides production-ready exception handling and logging capabilities.
