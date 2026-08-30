-- =========================================================================
-- V21: Políticas de Auto-Corte por Inadimplência e Auditoria de Ciclo de Vida RADIUS
-- =========================================================================

CREATE TABLE IF NOT EXISTS radius_policy_configs (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    auto_block_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    tolerance_days INTEGER NOT NULL DEFAULT 5,
    block_mode VARCHAR(30) NOT NULL DEFAULT 'CAPTIVE_PORTAL',
    reduced_download_kbps INTEGER NOT NULL DEFAULT 256,
    reduced_upload_kbps INTEGER NOT NULL DEFAULT 256,
    unblock_on_payment BOOLEAN NOT NULL DEFAULT TRUE,
    send_pod_on_block BOOLEAN NOT NULL DEFAULT TRUE,
    send_pod_on_unblock BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS radius_lifecycle_logs (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    contract_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    username VARCHAR(100) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    reason VARCHAR(255),
    nas_ip VARCHAR(64),
    success BOOLEAN NOT NULL DEFAULT TRUE,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_radius_lifecycle_contract ON radius_lifecycle_logs(contract_id);
CREATE INDEX IF NOT EXISTS idx_radius_lifecycle_username ON radius_lifecycle_logs(username);
CREATE INDEX IF NOT EXISTS idx_radius_lifecycle_created_at ON radius_lifecycle_logs(created_at DESC);

-- Insere configuração padrão se não existir
INSERT INTO radius_policy_configs (id, auto_block_enabled, tolerance_days, block_mode, reduced_download_kbps, reduced_upload_kbps, unblock_on_payment, send_pod_on_block, send_pod_on_unblock)
VALUES (uuidv7(), TRUE, 5, 'CAPTIVE_PORTAL', 256, 256, TRUE, TRUE, TRUE)
ON CONFLICT DO NOTHING;
