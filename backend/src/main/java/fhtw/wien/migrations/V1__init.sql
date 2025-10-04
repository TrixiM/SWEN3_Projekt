CREATE TABLE IF NOT EXISTS documents (
                                         id UUID PRIMARY KEY,
                                         title VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(127) NOT NULL,
    size_bytes BIGINT NOT NULL,
    bucket VARCHAR(63) NOT NULL,
    object_key TEXT NOT NULL,
    storage_uri TEXT NOT NULL,
    checksum_sha256 CHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'NEW',
    version INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
    );
