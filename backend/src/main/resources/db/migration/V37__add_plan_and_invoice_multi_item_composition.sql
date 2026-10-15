-- ============================================================================
-- V37: Composição Tributária Multi-Itens no Plano e Fatura (SCM + SVAs/Locação)
-- e Tributação Seletiva na NFCom Modelo 62 (Onda H)
-- ============================================================================

ALTER TABLE plans
    ADD COLUMN IF NOT EXISTS scm_amount NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS sva_amount NUMERIC(10, 2) DEFAULT 0.00;

UPDATE plans
SET scm_amount = price,
    sva_amount = 0.00
WHERE scm_amount IS NULL;

CREATE TABLE IF NOT EXISTS plan_composition_items (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL REFERENCES plans(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(40) NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    fiscal_code VARCHAR(30),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_plan_composition_items_plan_id
    ON plan_composition_items(plan_id);

ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS scm_amount NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS sva_amount NUMERIC(10, 2) DEFAULT 0.00;

UPDATE invoices
SET scm_amount = amount,
    sva_amount = 0.00
WHERE scm_amount IS NULL;

CREATE TABLE IF NOT EXISTS invoice_composition_items (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(40) NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    fiscal_code VARCHAR(30),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_invoice_composition_items_invoice_id
    ON invoice_composition_items(invoice_id);

ALTER TABLE nfcom_records
    ADD COLUMN IF NOT EXISTS scm_taxable_amount NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS sva_exempt_amount NUMERIC(12, 2) DEFAULT 0.00;

UPDATE nfcom_records
SET scm_taxable_amount = valor_total,
    sva_exempt_amount = 0.00
WHERE scm_taxable_amount IS NULL;
