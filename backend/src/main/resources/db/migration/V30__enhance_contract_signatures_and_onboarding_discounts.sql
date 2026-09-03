-- =========================================================================
-- V30: Aprimoramento da Assinatura Eletrônica via Pix (MP 2.200-2/01)
-- Suporte a Desconto Automático de R$ 1,00 na Fatura e Fallback Oficial
-- =========================================================================

-- 1. Enriquecer contract_signatures com método de fallback e rastreio do desconto
ALTER TABLE contract_signatures 
    ADD COLUMN IF NOT EXISTS fallback_method VARCHAR(50) DEFAULT 'PIX' NOT NULL,
    ADD COLUMN IF NOT EXISTS discount_applied_invoice_id UUID REFERENCES invoices(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS onboarding_credit_amount NUMERIC(10,2) DEFAULT 0.00 NOT NULL,
    ADD COLUMN IF NOT EXISTS forensic_certificate_pdf_url TEXT;

-- 2. Enriquecer contracts para guardar crédito de onboarding caso não haja fatura emitida no momento da assinatura
ALTER TABLE contracts
    ADD COLUMN IF NOT EXISTS pending_onboarding_credit NUMERIC(10,2) DEFAULT 0.00 NOT NULL;

CREATE INDEX IF NOT EXISTS idx_contract_signatures_fallback ON contract_signatures(fallback_method);
