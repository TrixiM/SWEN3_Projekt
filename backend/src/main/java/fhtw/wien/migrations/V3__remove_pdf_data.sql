-- Remove pdf_data column as PDFs are now only stored in MinIO
ALTER TABLE documents DROP COLUMN IF EXISTS pdf_data;
