# Document Access Batch Service

## Overview

The **Document Access Batch Service** is a Spring Boot application that reads XML access log
files, processes them into access statistics, and sends each record as a message to a RabbitMQ
queue. A backend service consumes the messages and stores them in a database. After successful 
processing, the batch service archives the processed XML files.

This application is designed to run in a Docker environment and is scheduled to execute once per day.

---

## Architecture

### Components

| Component                   | Purpose                                                                |
| --------------------------- | ---------------------------------------------------------------------- |
| **Spring Batch Job**        | Orchestrates the reading, processing, writing, and archiving steps.    |
| **MultiResourceItemReader** | Reads all XML files from a configured directory.                       |
| **ItemProcessor**           | Converts XML data into DTOs with timestamped statistics.               |
| **ItemWriter**              | Sends DTO messages to RabbitMQ using a producer.                       |
| **ArchiveTasklet**          | Moves processed XML files to an archive folder.                        |
| **RabbitMQ**                | Message broker that routes messages from producer to backend consumer. |
| **Scheduler**               | Triggers the batch job automatically at 1:00 AM daily.                 |

---

## Data Flow

1. **Reader** loads all XML files matching `*.xml` from `/app/accessLog`.
2. Each `<document>` element is unmarshalled into a `DocumentAccessXml` object.
3. The processor converts each object into a `DocumentAccessStatDto`, adding a timestamp.
4. The writer sends one RabbitMQ message per DTO.
5. After all files are processed, the archive tasklet moves the XML files to `/app/accessLog/archive`.
6. The backend service consumes messages from the queue and persists them to the database.

---

## Configuration

### Application properties

| Property                    | Description                                  |
| --------------------------- | -------------------------------------------- |
| `spring.datasource.*`       | PostgreSQL connection settings.              |
| `spring.rabbitmq.*`         | RabbitMQ connection settings.                |
| `app.accesslog.dir`         | Directory containing XML access logs.        |
| `app.accesslog.archive.dir` | Directory where processed logs are archived. |
| `spring.batch.job.enabled`  | Enables Spring Batch job execution.          |

Example:

```properties
app.accesslog.dir=/app/accessLog
app.accesslog.archive.dir=/app/accessLog/archive
```

### Docker setup

The application runs inside a Docker container. The host directory `./document-access-batch-service/accessLog` is mounted into the container at `/app/accessLog` via Docker Compose. This ensures that input files and archives persist outside the container.

```yaml
volumes:
  - ./document-access-batch-service/accessLog:/app/accessLog
```

---

## Spring Batch Job

### Job Definition

The job is defined in `ChunksConfig`:

* **Job name**: `documentAccessJob`
* **Steps**:

    1. `documentAccessStep` (chunk-based processing)
    2. `archiveStep` (tasklet)

### documentAccessStep

* Chunk size: **10**
* Reader: `MultiResourceItemReader<DocumentAccessXml>`
* Processor: `DocumentAccessItemProcessor`
* Writer: `DocumentAccessItemWriter`

The step reads and processes records in chunks of 10. After each chunk is committed,
the next chunk is processed.

### archiveStep

A `Tasklet` step that runs **once after all chunks are processed**. It moves processed
XML files from the input folder to the archive folder.

---

## Reader

The reader is a `MultiResourceItemReader` that delegates to a `StaxEventItemReader`.

### Behavior

* Scans `/app/accessLog/*.xml`
* Reads each file sequentially
* Each `<document>` element is treated as one item
* JAXB is used to unmarshal XML into `DocumentAccessXml`

---

## XML Model

### `DocumentAccessXml`

Fields:

* `documentId` (UUID)
* `accessCount` (int)

Annotations:

* `@XmlRootElement(name = "document")`: Root element name
* `@XmlAccessorType(XmlAccessType.FIELD)`: JAXB reads fields directly
* `@XmlElement(name = "documentId")`: Maps XML element
* `@XmlJavaTypeAdapter(UUIDAdapter.class)`: Converts string to UUID

---

## Processor

### `DocumentAccessItemProcessor`

* Implements `ItemProcessor<DocumentAccessXml, DocumentAccessStatDto>`
* Converts XML model to DTO
* Adds processing timestamp (`LocalDate.now()`)
* Implements `StepExecutionListener` to log read/write counts after step completion

---

## Writer

### `DocumentAccessItemWriter`

* Implements `ItemWriter<DocumentAccessStatDto>`
* Sends one message per DTO to RabbitMQ using `DocumentAccessStatProducer`
* Executes once per chunk

Example:

```
chunk size 10 -> 10 messages produced
```

---

## RabbitMQ Configuration

### Exchange, Queue, Binding

* Exchange: `document.exchange` (Direct)
* Queue: `document.access.stats.queue`
* Routing key: `document.access.stats`

### Producer

`DocumentAccessStatProducer` uses `RabbitTemplate` to send messages:

```java
rabbitTemplate.convertAndSend(
  DOCUMENT_EXCHANGE,
  DOCUMENT_ACCESS_STATS_ROUTING_KEY,
  message
);
```

### Message Converter

A `Jackson2JsonMessageConverter` is configured so messages are sent as JSON.

---

## Scheduler

### `BatchScheduler`

Runs the batch job every day at **1:00 AM**.

```java
@Scheduled(cron = "0 0 1 * * *")
```

Adds a unique timestamp parameter to each run to prevent Spring Batch from treating it as a duplicate execution.

---

## File Archiving

### `ArchiveTasklet`

* Runs after the batch step completes successfully
* Moves all `.xml` files from `/app/accessLog` to `/app/accessLog/archive`
* Overwrites files in archive if they already exist
* Ensures processed files are not reprocessed

---

## Docker Compose Integration

The application runs alongside PostgreSQL and RabbitMQ in Docker Compose.

Key configuration:

* `document-access-batch-service` depends on `postgres` and `rabbitmq`
* The `accessLog` folder is mounted as a volume
* The service exposes port `8081` mapped to container port `8080`

##
