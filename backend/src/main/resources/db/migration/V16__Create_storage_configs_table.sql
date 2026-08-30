-- ==============================================================================
-- V16: ARMAZENAMENTO S3 UNIVERSAL, SEAWEEDFS LOCAL E DRIVERS CLOUD (AWS/R2)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS storage_configs (
    id UUID PRIMARY KEY,
    company_id UUID REFERENCES companies(id) ON DELETE CASCADE,
    storage_type VARCHAR(50) NOT NULL DEFAULT 'S3',
    provider VARCHAR(50) NOT NULL DEFAULT 'SEAWEEDFS_LOCAL',
    endpoint_url VARCHAR(255),
    bucket_name VARCHAR(100) NOT NULL DEFAULT 'isperp-files',
    region VARCHAR(50) NOT NULL DEFAULT 'us-east-1',
    access_key VARCHAR(255),
    secret_key VARCHAR(255),
    path_style_access BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_storage_configs_company ON storage_configs(company_id);
CREATE INDEX IF NOT EXISTS idx_storage_configs_active ON storage_configs(is_active);
