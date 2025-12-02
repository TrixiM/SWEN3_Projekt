-- Since PostgreSQL database schema is persistent and db migration tools (Flyway, Liquibase) are not in scope of this project
ALTER TABLE documents
DROP CONSTRAINT documents_status_check;

ALTER TABLE documents
    ADD CONSTRAINT documents_status_check CHECK (
        status IN (
                   'NEW',
                   'UPLOADED',
                   'OCR_PENDING',
                   'OCR_IN_PROGRESS',
                   'OCR_COMPLETED',
                   'OCR_FAILED',
                   'INDEXED'
            )
        );
