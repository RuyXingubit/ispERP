-- =========================================================================
-- V29: Schema para Payback de Projetos de Rede e Sentinela IA Anti-Fraude
-- =========================================================================

-- 1. Tabela de Projetos de Rede / Expansão de Bairros (Centro de Custo de CAPEX de Rede)
CREATE TABLE IF NOT EXISTS network_projects (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    name VARCHAR(255) NOT NULL,
    neighborhood VARCHAR(150) NOT NULL,
    city VARCHAR(100) NOT NULL,
    budget_amount NUMERIC(15, 2) NOT NULL,
    target_subscribers INT NOT NULL DEFAULT 100,
    start_date DATE NOT NULL DEFAULT CURRENT_DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- PLANNING, DEPLOYING, ACTIVE, COMPLETED
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_network_projects_status ON network_projects(status);
CREATE INDEX IF NOT EXISTS idx_network_projects_neighborhood ON network_projects(neighborhood);

-- 2. Vinculação de CTOs aos Projetos de Expansão de Rede
ALTER TABLE ftth_ctos ADD COLUMN IF NOT EXISTS project_id UUID REFERENCES network_projects(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_ftth_ctos_project ON ftth_ctos(project_id);

-- 3. Tabela de Logs e Dossiês do Sentinela IA (Auditoria Forense Contínua com Gemini)
CREATE TABLE IF NOT EXISTS sentinel_audit_logs (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    audit_type VARCHAR(100) NOT NULL, -- CASH_CONCENTRATION, FEE_WAIVER_ANOMALY, ORPHAN_ONU, INVENTORY_DROP_LEAK
    severity VARCHAR(50) NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    gemini_analysis TEXT,
    recommended_action TEXT,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sentinel_audit_severity ON sentinel_audit_logs(severity);
CREATE INDEX IF NOT EXISTS idx_sentinel_audit_resolved ON sentinel_audit_logs(resolved);
