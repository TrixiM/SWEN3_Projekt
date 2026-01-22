-- Backend user already exists (myuser)

-- Batch user
CREATE USER batchuser WITH PASSWORD 'batchpassword';

-- Allow batch user to connect and create tables
GRANT CONNECT ON DATABASE documentdb TO batchuser;

GRANT USAGE ON SCHEMA public TO batchuser;
GRANT CREATE ON SCHEMA public TO batchuser;

-- Backend full access (safe to repeat)
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO myuser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO myuser;


-- Batch user full access (optional but OK)
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO batchuser;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO batchuser;

-- Ensure future tables & sequences are covered
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO batchuser;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO batchuser;
