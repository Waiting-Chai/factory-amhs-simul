-- Migration script to fix existing fileUrl format
-- Updates old format /{bucket}/{key} to /api/v1/files/{fileId}/content
-- Execute this in MySQL directly or let Flyway run it

UPDATE model_versions mv
INNER JOIN files f ON mv.file_id = f.id
SET mv.file_url = CONCAT('/api/v1/files/', f.id, '/content')
WHERE mv.file_url LIKE '/sim-artifacts-local/%';

UPDATE files f
SET f.storage_url = CONCAT('/api/v1/files/', f.id, '/content')
WHERE f.storage_url LIKE '/sim-artifacts-local/%';
