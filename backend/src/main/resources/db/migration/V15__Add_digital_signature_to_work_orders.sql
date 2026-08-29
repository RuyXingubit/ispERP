-- ==============================================================================
-- V15: ASSINATURA DIGITAL EM CAMPO & TERMOS DE INSTALAÇÃO
-- ==============================================================================

ALTER TABLE work_orders
    ADD COLUMN IF NOT EXISTS digital_signature_base64 TEXT,
    ADD COLUMN IF NOT EXISTS customer_signature_name VARCHAR(150);
