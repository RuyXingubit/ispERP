-- =============================================================================
-- V38: Ciclo de Vida Pós-Venda de Contratos (Onda I)
-- Upgrade/Downgrade com Pro-Rata Die + CoA RADIUS, Mudança de Endereço com O.S.
-- e Troca de Titularidade com Migração de Faturas e Nova Assinatura Forense
-- =============================================================================

ALTER TABLE contracts
    ADD COLUMN IF NOT EXISTS pending_pro_rata_adjustment NUMERIC(10, 2) NOT NULL DEFAULT 0.00;

CREATE TABLE IF NOT EXISTS contract_lifecycle_events (
    id UUID PRIMARY KEY,
    contract_id UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    event_type VARCHAR(40) NOT NULL,
    previous_customer_id UUID,
    new_customer_id UUID,
    previous_plan_id UUID,
    new_plan_id UUID,
    previous_address VARCHAR(255),
    new_address VARCHAR(255),
    old_monthly_fee NUMERIC(10, 2),
    new_monthly_fee NUMERIC(10, 2),
    remaining_days_in_cycle INTEGER,
    pro_rata_adjustment_amount NUMERIC(10, 2),
    adjusted_invoice_id UUID,
    migrated_invoices_count INTEGER,
    work_order_id UUID,
    signature_session_id UUID,
    signature_url VARCHAR(500),
    radius_rate_limit_applied VARCHAR(120),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_contract_lifecycle_events_contract_id
    ON contract_lifecycle_events(contract_id, created_at DESC);
