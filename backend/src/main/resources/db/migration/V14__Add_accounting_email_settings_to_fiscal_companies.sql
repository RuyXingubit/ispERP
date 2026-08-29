-- ==============================================================================
-- V14: CONFIGURAÇÕES DE ASSESSORIA CONTÁBIL & ENVIO AUTOMÁTICO DE RELATÓRIOS
-- ==============================================================================

ALTER TABLE fiscal_companies
    ADD COLUMN IF NOT EXISTS accounting_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS accounting_emails VARCHAR(500),
    ADD COLUMN IF NOT EXISTS accounting_send_day INTEGER NOT NULL DEFAULT 5,
    ADD COLUMN IF NOT EXISTS accounting_auto_send BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS accounting_last_sent_at TIMESTAMP WITH TIME ZONE;
