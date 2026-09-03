-- =========================================================================
-- V31: Módulo de Backup Multi-Destino, Criptografia AES-256 e Disaster Recovery
-- =========================================================================

-- 1. Tabela de Políticas Centrais de Backup
CREATE TABLE IF NOT EXISTS backup_policies (
    id UUID PRIMARY KEY,
    security_mode VARCHAR(30) NOT NULL DEFAULT 'MANAGED_RESCUE',
    master_key_hash VARCHAR(128) NOT NULL,
    encrypted_master_key TEXT,
    cron_expression VARCHAR(50) NOT NULL DEFAULT '0 0 3 * * *',
    retention_days INT NOT NULL DEFAULT 30,
    compression_algorithm VARCHAR(20) NOT NULL DEFAULT 'ZSTD',
    auto_dry_run_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    rescue_kit_downloaded_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 2. Tabela de Destinos Remotos de Armazenamento
CREATE TABLE IF NOT EXISTS backup_destinations (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    storage_type VARCHAR(30) NOT NULL DEFAULT 'S3_COMPATIBLE',
    endpoint_url VARCHAR(255),
    bucket_name VARCHAR(100),
    region VARCHAR(50) DEFAULT 'auto',
    access_key VARCHAR(255),
    secret_key_encrypted TEXT,
    path_prefix VARCHAR(255) DEFAULT 'backups/isperp',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    last_tested_at TIMESTAMP WITH TIME ZONE,
    last_test_status VARCHAR(20),
    last_test_error TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 3. Tabela de Histórico e Auditoria de Execuções e Testes de Integridade
CREATE TABLE IF NOT EXISTS backup_execution_logs (
    id UUID PRIMARY KEY,
    policy_id UUID NOT NULL REFERENCES backup_policies(id) ON DELETE CASCADE,
    destination_id UUID REFERENCES backup_destinations(id) ON DELETE SET NULL,
    trigger_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    original_size_bytes BIGINT,
    compressed_size_bytes BIGINT,
    compression_ratio NUMERIC(5, 2),
    sha256_hash VARCHAR(64),
    duration_seconds INT,
    error_message TEXT,
    is_dry_run_verified BOOLEAN NOT NULL DEFAULT FALSE,
    dry_run_verified_at TIMESTAMP WITH TIME ZONE,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_backup_logs_policy_status ON backup_execution_logs(policy_id, status);
CREATE INDEX IF NOT EXISTS idx_backup_logs_started_at ON backup_execution_logs(started_at DESC);
CREATE INDEX IF NOT EXISTS idx_backup_destinations_active ON backup_destinations(is_active, is_primary);
