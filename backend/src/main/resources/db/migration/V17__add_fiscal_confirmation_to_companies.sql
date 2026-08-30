-- ==============================================================================
-- V17: CONFIRMAÇÃO CONTÁBIL E ASSISTENTE DE REGIME FISCAL EM FISCAL_COMPANIES
-- ==============================================================================

ALTER TABLE fiscal_companies 
    ADD COLUMN IF NOT EXISTS fiscal_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS fiscal_confirmed_at TIMESTAMP WITH TIME ZONE;
